/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */

package com.lucendar.gnss.service.db;

import com.lucendar.common.db.types.SqlDialect;
import info.gratour.common.error.ErrorWithCode;
import info.gratour.jt808common.spi.model.TermCmd;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.sql.DataSource;
import java.sql.Types;

public class DefaultTermCmdDao extends AbstractJdbcDao implements TermCmdDao {
    public DefaultTermCmdDao(@NonNull DataSource ds, @NonNull SqlDialect sqlDialect) {
        super(ds, sqlDialect);
    }

    @Override
    public TermCmd createTermCmd(TermCmd termCmd) {
        if (termCmd.getId() == null)
            throw ErrorWithCode.invalidParam("id");

        if (termCmd.getReqTm() == 0L) {
            termCmd.setReqTm(System.currentTimeMillis());
        }

        var sql = """
                  INSERT INTO t_term_cmd (
                    f_id, f_msg_id, f_sub_cmd_typ, f_app_id, f_sim_no, f_req_id, f_plate_no,
                    f_plate_color, f_req_tm, f_sent_tm, f_ack_tm, f_end_tm,
                    f_status, f_msg_sn, f_ack_code, f_params, f_ack_params
                  ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?
                  )
                """;

        update(sql, binder -> {
            binder.setString(termCmd.getId());
            binder.setString(termCmd.getMsgId());
            binder.setString(termCmd.getSubCmdTyp());
            binder.setString(termCmd.getAppId());
            binder.setString(termCmd.getSimNo());
            binder.setString(termCmd.getReqId());
            binder.setString(termCmd.getPlateNo());

            binder.setIntObject(termCmd.getPlateColor());
            binder.setOffsetDateTime(termCmd.getReqTm());
            binder.setNull(Types.TIMESTAMP_WITH_TIMEZONE);
            binder.setNull(Types.TIMESTAMP_WITH_TIMEZONE);
            binder.setNull(Types.TIMESTAMP_WITH_TIMEZONE);

            binder.setInt(termCmd.getStatus());
            binder.setIntObject(null);
            binder.setIntObject(null);
            binder.setString(termCmd.paramsJson());
            binder.setString(termCmd.ackParamsJson());
        });



        return termCmd;
    }

    @Override
    public void markCmdSent(TermCmd termCmd) {
        if (termCmd.getId() == null)
            throw ErrorWithCode.invalidParam("id");

        if (termCmd.getSentTm() == null)
            termCmd.setSentTm(System.currentTimeMillis());

        var sql = """
                UPDATE t_term_cmd SET
                    f_status = ?,
                    f_msg_sn = ?,
                    f_sent_tm = ?
                WHERE f_id = ? AND f_status = 0
                """;

        var r = update(sql, setter -> {
            setter.setInt(TermCmd.CMD_STATUS__SENT);
            setter.setIntObject(termCmd.getMsgSn());
            setter.setOffsetDateTime(termCmd.getSentTm());
            setter.setString(termCmd.getId());
        });

        if (r == 0) {
            sql = """
                  UPDATE t_term_cmd SET
                    f_msg_sn = ?,
                    f_sent_tm = ?
                  WHERE f_id = ? AND f_sent_tm IS NULL
                  """;

            update(sql, setter -> {
                setter.setIntObject(termCmd.getMsgSn());
                setter.setOffsetDateTime(termCmd.getSentTm());
                setter.setString(termCmd.getId());
            });
        }
    }

    @Override
    public void markCmdAck(TermCmd termCmd) {
        if (termCmd.getId() == null)
            throw ErrorWithCode.invalidParam("id");

        if (termCmd.getAckTm() == null)
            termCmd.setAckTm(System.currentTimeMillis());

        if (termCmd.getEndTm() == null)
            termCmd.setEndTm(termCmd.getAckTm());

        var sql = """
                UPDATE t_term_cmd SET
                    f_status = ?,
                    f_ack_tm = ?,
                    f_end_tm = ?,
                    f_ack_code = ?,
                    f_ack_params = ?
                WHERE f_id = ? AND f_status IN (0, 1)
                """;

        update(sql, setter -> {
            setter.setInt(TermCmd.CMD_STATUS__SENT);
            setter.setOffsetDateTime(termCmd.getSentTm());
            setter.setOffsetDateTime(termCmd.getEndTm());
            setter.setIntObject(termCmd.getAckCode());
            setter.setString(termCmd.ackParamsJson());
            setter.setString(termCmd.getId());
        });
    }

    @Override
    public void markCmdCompleted(TermCmd termCmd) {
        if (termCmd.getId() == null)
            throw ErrorWithCode.invalidParam("id");

        if (termCmd.getEndTm() == null)
            termCmd.setEndTm(System.currentTimeMillis());

        var sql = """
                UPDATE t_term_cmd SET
                    f_status = ?,
                    f_end_tm = ?
                WHERE f_id = ? AND f_status IN (0, 1)
                """;
        update(sql, setter -> {
            setter.setInt(termCmd.getStatus());
            setter.setOffsetDateTime(termCmd.getEndTm());
            setter.setString(termCmd.getId());
        });
    }
}
