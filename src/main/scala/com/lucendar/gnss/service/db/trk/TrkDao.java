/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.db.trk;

import info.gratour.jt808common.protocol.msg.types.trk.Trk;

public interface TrkDao {

    /**
     * 批量保存实时轨迹
     *
     * @param trks 轨迹数组
     * @param count 轨迹数量
     */
    void bulkSaveLatestTrks(Trk[] trks, int count);


    /**
     * 批量保存历史轨迹
     *
     * @param trks 轨迹数组
     * @param count 轨迹数量
     */
    void bulkSaveHisTrks(Trk[] trks, int count);
}
