/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */

package com.lucendar.gnss.service.db;

import com.lucendar.strm.common.types.CommLog;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 终端通讯日志数据库访问接口
 */
public interface CommLogDao {

    /**
     * 查询终端通讯日志
     *
     * @param startTs 开始时间
     * @param appId   AppId
     * @param simNo   终端识别号
     * @param limit   返回的最大记录数
     * @param offset  返回结果的偏移量
     * @return
     */
    @Transactional
    List<CommLog> qryGatewayCommLog(
            @NonNull long startTs,
            @NonNull String appId,
            @NonNull String simNo,
            int limit,
            int offset);

    /**
     * 保存通讯日志。注意，本方法内部管理事务
     *
     * @param commLogs 所要保存的通讯日志列表
     */
    void saveCommLogs(@NonNull List<CommLog> commLogs);
}
