package com.lucendar.gnss.types;

import info.gratour.jt808common.protocol.msg.types.trk.Trk;

import java.util.Arrays;
import java.util.StringJoiner;

/**
 * Create multimedia request
 */
public class CreateMmReq {

    private String appId;
    private String simNo;
    private long recvTm;
    private long gpsTm;
    private long mediaId;
    private short typ;
    private short fmt;
    private short evtCode;
    private short chan;
    private Trk trk;
    private byte[] data;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSimNo() {
        return simNo;
    }

    public void setSimNo(String simNo) {
        this.simNo = simNo;
    }

    public long getRecvTm() {
        return recvTm;
    }

    public void setRecvTm(long recvTm) {
        this.recvTm = recvTm;
    }

    public long getGpsTm() {
        return gpsTm;
    }

    public void setGpsTm(long gpsTm) {
        this.gpsTm = gpsTm;
    }

    public long getMediaId() {
        return mediaId;
    }

    public void setMediaId(long mediaId) {
        this.mediaId = mediaId;
    }

    public short getTyp() {
        return typ;
    }

    public void setTyp(short typ) {
        this.typ = typ;
    }

    public short getFmt() {
        return fmt;
    }

    public void setFmt(short fmt) {
        this.fmt = fmt;
    }

    public short getEvtCode() {
        return evtCode;
    }

    public void setEvtCode(short evtCode) {
        this.evtCode = evtCode;
    }

    public short getChan() {
        return chan;
    }

    public void setChan(short chan) {
        this.chan = chan;
    }

    public Trk getTrk() {
        return trk;
    }

    public void setTrk(Trk trk) {
        this.trk = trk;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CreateMmReq.class.getSimpleName() + "[", "]")
                .add("appId='" + appId + "'")
                .add("simNo='" + simNo + "'")
                .add("recvTm=" + recvTm)
                .add("gpsTm=" + gpsTm)
                .add("mediaId=" + mediaId)
                .add("typ=" + typ)
                .add("fmt=" + fmt)
                .add("evtCode=" + evtCode)
                .add("chan=" + chan)
                .add("trk=" + trk)
                .add("data=" + Arrays.toString(data))
                .toString();
    }
}
