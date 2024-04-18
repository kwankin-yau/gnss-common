/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */

package com.lucendar.gnss.service.db;

import info.gratour.jt808common.spi.model.TermCmd;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface TermCmdDao {

    /**
     *
     * @param termCmd The termCmd to be save to db. Note that the `id` of termCmd must be set before call this method.
     * @return given `termCmd` object with some properties set(for example, `reqTm`).
     */
    TermCmd createTermCmd(TermCmd termCmd);

    /**
     * Note: this method may modify the properties of the given `termCmd` parameter.
     * @param termCmd
     */
    void markCmdSent(TermCmd termCmd);

    /**
     * Note: this method may modify the properties of the given `termCmd` parameter.
     *
     * @param termCmd
     */
    void markCmdAck(TermCmd termCmd);

    /**
     * Note: this method may modify the properties of the given `termCmd` parameter.
     *
     * @param termCmd
     */
    void markCmdCompleted(TermCmd termCmd);

    /**
     * Note: this method may modify the properties of the given `termCmd` parameter.
     *
     * @param termCmd
     */
    default void updateTermCmdStatus(TermCmd termCmd) {
        switch (termCmd.getStatus()) {
            case TermCmd.CMD_STATUS__SENT ->
                    markCmdSent(termCmd);

            case TermCmd.CMD_STATUS__ACK ->
                    markCmdAck(termCmd);

            default -> {
                markCmdCompleted(termCmd);
            }

        }
    }

}
