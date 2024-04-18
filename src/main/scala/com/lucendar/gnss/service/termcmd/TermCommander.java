/*******************************************************************************
 *  Copyright (c) 2019, 2022 lucendar.com.
 *  All rights reserved.
 *
 *  Contributors:
 *     KwanKin Yau (alphax@vip.163.com) - initial API and implementation
 *******************************************************************************/
package com.lucendar.gnss.service.termcmd;

import info.gratour.jt808common.protocol.msg.types.ackparams.JT808AckParams;
import info.gratour.jt808common.spi.model.TermCmd;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Terminal command issuer
 */
public interface TermCommander {

    /**
     * Create terminal command and notify the listeners(via message queue).
     * <p>
     * Note: this method may throw an exception.
     *
     * @param cmd     command to create.
     * @param publish whether publish the terminal command to MQ after command record created.
     * @return created command
     * @exception Exception 本方法可抛出异常。
     */
    TermCmd createCmd(@NonNull TermCmd cmd, boolean publish) throws Exception;

    /**
     * Publish the terminal command to the MQ.
     *
     * @param cmd the terminal command to publish
     */
    void publishCmd(@NonNull TermCmd cmd);

    /**
     * 查找终端指令记录
     *
     * @param cmdId 指令ID
     * @return 终端指令记录
     */
    TermCmd findCmd(@NonNull String cmdId);

    /**
     * 根据终端指令的外部 ID 查找终端指令记录
     * @param externalCmdId 终端指令的外部 ID
     * @return 终端指令记录
     */

    TermCmd findCmdByExternalId(@NonNull String externalCmdId);


    /**
     * 将指令状态改为已发送，并投递 TermCmdStateChanged 事件到 EventBus。
     * <p>
     * 注意：此方法不能抛出异常。实际的数据库变更为异步处理，并可能迟于 TermCmdStateChanged 事件。
     *
     * @param cmd                终端指令对象
     * @param sentTm             发送时间
     * @param msgSn              流水号
     * @return the input cmd which the status is updated to CMD_STATUS__SENT and the sentTm is set to given `sentTm`.
     */
    TermCmd markCmdSent(@NonNull TermCmd cmd, long sentTm, @Nullable Integer msgSn);

    /**
     * 将指令状态改为已应答，并投递 TermCmdStateChanged 事件到 EventBus。
     * <p>
     * 注意：此方法不能抛出异常。实际的数据库变更为异步处理，并可能迟于 TermCmdStateChanged 事件。
     *
     * @param cmd                终端指令对象
     * @param ackTm              应答时间
     * @param ackMsgId           终端应答自身的指令消息号
     * @param ackSeqNo           终端应答自身的指令消息流水号
     * @param ackCode            终端应答的应答码
     * @param ackParams          终端应答的数据内容
     * @return the input `cmd` which the status is updated to CMD_STATUS__ACK and the `ackTm`, `ackCode`, `ackParams`
     * is set to given value of parameters.
     */
    TermCmd markCmdAck(
            @NonNull TermCmd cmd,
            long ackTm,
            @Nullable String ackMsgId,
            @Nullable Integer ackSeqNo,
            @Nullable Integer ackCode,
            @Nullable JT808AckParams ackParams);

    /**
     * 将指令状态改为已完成（除已应答外的状态），并投递 TermCmdStateChanged 事件到 EventBus。
     * <p>
     * 注意：此方法不能抛出异常。实际的数据库变更为异步处理，并可能迟于 TermCmdStateChanged 事件。
     *
     * @param cmd                终端指令对象
     * @param status             新的状态
     * @param endTm              状态改变时间
     * @return the input cmd which the `status`, `endTm` is set to given value of parameters.
     */
    TermCmd markCmdCompleted(@NonNull TermCmd cmd, int status, long endTm);
}
