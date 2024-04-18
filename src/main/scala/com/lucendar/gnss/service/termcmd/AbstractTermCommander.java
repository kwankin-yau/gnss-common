package com.lucendar.gnss.service.termcmd;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lucendar.gnss.sdk.db.RecIdProvider;
import com.lucendar.gnss.service.db.TermCmdDao;
import com.lucendar.gnss.service.evtbus.EventBus;
import info.gratour.common.error.ErrorWithCode;
import info.gratour.jt808common.protocol.msg.types.ackparams.JT808AckParams;
import info.gratour.jt808common.spi.model.TermCmd;
import info.gratour.jt808common.spi.model.TermCmdStateChanged;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 终端指令处理器抽象类
 */
public abstract class AbstractTermCommander implements TermCommander {

    /**
     * Logger
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger("gateway.termCommander");

    private final String servInstanceId;
    private final TermCmdDao termCmdDao;
    private final RecIdProvider recIdProvider;

    // key: cmdId
    private final Cache<String, TermCmd> termCmdCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    // key: externalCmdId
    private final Cache<String, TermCmd> termCmdExternalCmdIdCache = Caffeine.newBuilder()
            .expireAfterWrite(
                    TimeUnit.HOURS.toSeconds(2) + 5,
                    TimeUnit.SECONDS)
            .build();

    /**
     * 构造函数
     *
     * @param termCmdDao           终端指令DAO
     * @param termCmdRecIdProvider 终端指令ID提供者
     */
    public AbstractTermCommander(String servInstanceId, TermCmdDao termCmdDao, RecIdProvider termCmdRecIdProvider) {
        this.servInstanceId = servInstanceId;
        this.termCmdDao = termCmdDao;
        this.recIdProvider = termCmdRecIdProvider;
    }

    private void putToCache(@NonNull TermCmd termCmd, boolean changed) {
        termCmdCache.put(termCmd.getId(), termCmd);
        var externalCmdId = termCmd.getExternalId();
        if (externalCmdId != null) {
            LOGGER.debug("Put command into externalCmdIdCache: {}", externalCmdId);
            termCmdExternalCmdIdCache.put(externalCmdId, termCmd);
        }

        if (changed) {
            termCmdDao.updateTermCmdStatus(termCmd);

            var notif = new TermCmdStateChanged();
            notif.setRandomUuid();
            notif.setPub(servInstanceId);
            notif.assign(termCmd, System.currentTimeMillis());
            LOGGER.debug("Publish {}", notif);
            EventBus.CmdStateChangedEventHub.publish(notif);
        }
    }

    @Override
    public TermCmd createCmd(@NonNull TermCmd cmd, boolean publish) {
        if (cmd.getId() == null)
            cmd.setId(recIdProvider.nextId());

        cmd = termCmdDao.createTermCmd(cmd);
        putToCache(cmd, false);

        var r = cmd.clone();

        if (publish)
            publishCmd(cmd);

        return r;
    }

    @Override
    public TermCmd findCmd(@NonNull String cmdId) {
        return termCmdCache.getIfPresent(cmdId);
    }

    @Override
    public TermCmd findCmdByExternalId(@NonNull String externalCmdId) {
        if (externalCmdId == null)
            throw new NullPointerException("externalCmdId");

        return termCmdExternalCmdIdCache.getIfPresent(externalCmdId);
    }

    @Override
    public TermCmd markCmdSent(TermCmd cmd, long sentTm, @Nullable Integer msgSn) {
        cmd.setStatus(TermCmd.CMD_STATUS__SENT);
        cmd.setSentTm(sentTm);
        cmd.setMsgSn(msgSn);

        var cached = termCmdCache.getIfPresent(cmd.getId());
        if (cached != null) {
            cached = cached.clone();

            cached.setStatus(TermCmd.CMD_STATUS__SENT);
            cached.setSentTm(sentTm);
            cached.setMsgSn(msgSn);

            putToCache(cached, true);
        }

        return cmd;
    }

    @Override
    public TermCmd markCmdAck(
            @NonNull TermCmd cmd,
            long ackTm,
            @Nullable String ackMsgId,
            @Nullable Integer ackSeqNo,
            @Nullable Integer ackCode,
            @Nullable JT808AckParams ackParams) {
        cmd.setStatus(TermCmd.CMD_STATUS__ACK);
        cmd.setAckTm(ackTm);
        cmd.setEndTm(ackTm);
        cmd.setAckCode(ackCode);
        cmd.setAckParams(ackParams);

        var cached = termCmdCache.getIfPresent(cmd.getId());
        if (cached != null) {
            cached = cached.clone();

            cached.setStatus(TermCmd.CMD_STATUS__ACK);
            cached.setAckTm(ackTm);
            cached.setEndTm(ackTm);
            cached.setAckMsgId(ackMsgId);
            cached.setAckSeqNo(ackSeqNo);
            cached.setAckCode(ackCode);
            cached.setAckParams(ackParams);

            putToCache(cached, true);
        }

        return cmd;
    }

    @Override
    public TermCmd markCmdCompleted(@NonNull TermCmd cmd, int status, long endTm) {
        if (status == TermCmd.CMD_STATUS__ACK || !TermCmd.isAckOrCompletedStatus(status))
            throw ErrorWithCode.invalidParam("status");

        cmd.setStatus(status);
        cmd.setEndTm(endTm);

        var cached = termCmdCache.getIfPresent(cmd.getId());
        if (cached != null) {
            cached = cached.clone();

            cached.setStatus(status);
            cached.setEndTm(endTm);

            putToCache(cached, true);
        }

        return cmd;
    }
}
