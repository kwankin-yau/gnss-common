/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.db;

import com.lucendar.common.db.types.SqlDialect;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.sql.DataSource;

public class SysDao_Pg extends AbstractJdbcDao implements SysDao {

    // language=PostgreSQL
    private static final String SELECT_SEQ_VALUE ="SELECT nextval(?)";

    public SysDao_Pg(@NonNull DataSource ds, @NonNull SqlDialect sqlDialect) {
        super(ds, sqlDialect);
    }

    @Override
    public long nextSeqValue(String seqId) {
        return qryLongValue(SELECT_SEQ_VALUE, strStmtSetter(seqId));
    }

}
