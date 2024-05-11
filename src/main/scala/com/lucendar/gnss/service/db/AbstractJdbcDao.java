/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */

package com.lucendar.gnss.service.db;

import com.lucendar.common.db.jdbc.CallStmtProcessor;
import com.lucendar.common.db.jdbc.DbHelper;
import com.lucendar.common.db.jdbc.ResultSetMapper;
import com.lucendar.common.db.jdbc.StatementSetter;
import com.lucendar.common.db.types.SqlDialect;
import com.lucendar.strm.common.StreamingApi;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
 * 使用Spring JdbcTemplate的抽象DAO
 */
public abstract class AbstractJdbcDao {

    /**
     * 代表数据库操作的接口，用于 `AbstractJdbcDao.dbAction()`方法中。
     *
     * @param <T> 操作返回的类型
     */
    public interface DbAction<T> {
        T apply(Connection conn);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("gateway.abstractJdbcDao");

    /**
     * 数据源
     */
    protected final DataSource ds;

    /**
     * SQL方言
     */
    protected final SqlDialect sqlDialect;

    public AbstractJdbcDao(@NonNull DataSource ds, @NonNull SqlDialect sqlDialect) {
        this.ds = ds;
        this.sqlDialect = sqlDialect;
    }

    public DataSource getDs() {
        return ds;
    }

    public SqlDialect getSqlDialect() {
        return sqlDialect;
    }

    protected String appIdDef(@Nullable String appId) {
        if (appId != null)
            return appId;
        else
            return StreamingApi.DEFAULT_APP_ID;
    }

    protected <T> T dbAction(DbAction<T> action) {
        var conn = DataSourceUtils.getConnection(ds);
        try {
            return action.apply(conn);
        } finally {
            DataSourceUtils.releaseConnection(conn, ds);
        }
    }

    protected <T> T qryObject(
            @NonNull String sql,
            @Nullable StatementSetter setter,
            @NonNull ResultSetMapper<T> resultSetMapper) {
        LOGGER.debug("qryObject: {}", sql);
        return dbAction(conn -> DbHelper.qryObjectEx(sql, setter, resultSetMapper, conn));
    }

    protected String qryStringValue(
            @NonNull String sql,
            @Nullable StatementSetter setter) {
        LOGGER.debug("qryString: {}", sql);
        return dbAction(conn -> DbHelper.qryStringValueEx(sql, setter, conn).getOrElse(null));
    }

    protected Long qryLongValue(
            @NonNull String sql,
            @Nullable StatementSetter setter) {
        LOGGER.debug("qryString: {}", sql);
        return dbAction(conn -> DbHelper.qryLongValueEx(sql, setter, conn).getOrElse(null));
    }


    protected <T> List<T> qryList(
            @NonNull String sql,
            @Nullable StatementSetter setter,
            @NonNull ResultSetMapper<T> resultSetMapper) {
        LOGGER.debug("qryList: {}", sql);
        return dbAction(conn -> DbHelper.qryListEx(sql, setter, resultSetMapper, conn));
    }


    protected int update(
            @NonNull String sql,
            @Nullable StatementSetter setter) {
        LOGGER.debug("update: {}", sql);
        return dbAction(conn -> DbHelper.updateEx(sql, setter, conn));
    }

    protected long insertAndReturnKey(
            @NonNull String sql,
            @Nullable StatementSetter setter) {
        LOGGER.debug("update: {}", sql);
        return dbAction(conn -> {
            var r = DbHelper.updateExWithGenKey(sql, setter, conn);
            return r.getGeneratedKey();
        });
    }

    protected <T> void call(@NonNull String sql, @NonNull CallStmtProcessor<T> callback) {
        LOGGER.debug("call: {}", sql);
        dbAction(conn -> DbHelper.callEx2(sql, callback, conn));
    }


    protected StatementSetter strStmtSetter(String value) {
        return DbHelper.strStatementSetter(value);
    }

}
