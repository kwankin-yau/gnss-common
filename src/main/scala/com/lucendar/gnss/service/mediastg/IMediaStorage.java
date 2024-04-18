/*******************************************************************************
 *  Copyright (c) 2019, 2021 lucendar.com.
 *  All rights reserved.
 *
 *  Contributors:
 *     KwanKin Yau (alphax@vip.163.com) - initial API and implementation
 *******************************************************************************/
package com.lucendar.gnss.service.mediastg;


import info.gratour.jt808common.protocol.msg.JT808Msg_0801_MultiMediaData;

/**
 * 多媒体（照片、音频、视频，与JT/T 1078不同）存储服务
 */
public interface IMediaStorage {

    /**
     * 保存媒体。网关在收到多媒体数据时调用此方法。
     *
     * @param appId 应用ID
     * @param m 媒体数据消息 (0x0801)
     * @param recvTime 接收时间
     */
    void saveMedia(String appId, JT808Msg_0801_MultiMediaData m, long recvTime);

    /**
     * 多媒体存储服务的空实现
     */
    class DummyMediaStorage implements IMediaStorage {

        @Override
        public void saveMedia(String appId, JT808Msg_0801_MultiMediaData m, long recvTime) {
            // nop
        }
    }
}
