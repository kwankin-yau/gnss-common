/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.db;

public interface SysDao {

    /**
     * Get next sequence value for given sequence.
     *
     * @param seqId Sequence name.
     * @return The next value of the given sequence.
     */
    long nextSeqValue(String seqId);
}
