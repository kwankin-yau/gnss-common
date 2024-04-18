/*
 * Copyright (c) 2024 lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.model.term;

import info.gratour.jt808common.spi.model.TermBrief;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.StringJoiner;

/**
 * 终端简要信息
 */
public class TermBriefInfo implements TermBrief {

    private String appId;
    private String simNo;
    private String vehId;
    private String plateNo;
    private Integer plateColor;
    private int stdProtocols;

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public String getSimNo() {
        return simNo;
    }

    @Override
    public @Nullable String getVehId() {
        return vehId;
    }

    @Override
    public @Nullable String getPlateNo() {
        return plateNo;
    }

    @Override
    public @Nullable Integer getPlateColor() {
        return plateColor;
    }

    @Override
    public int getStdProtocols() {
        return stdProtocols;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setSimNo(String simNo) {
        this.simNo = simNo;
    }

    public void setVehId(String vehId) {
        this.vehId = vehId;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public void setPlateColor(Integer plateColor) {
        this.plateColor = plateColor;
    }

    public void setStdProtocols(int stdProtocols) {
        this.stdProtocols = stdProtocols;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TermBriefInfo.class.getSimpleName() + "[", "]")
                .add("appId='" + appId + "'")
                .add("simNo='" + simNo + "'")
                .add("vehId='" + vehId + "'")
                .add("plateNo='" + plateNo + "'")
                .add("plateColor=" + plateColor)
                .add("stdProtocols=" + stdProtocols)
                .toString();
    }
}
