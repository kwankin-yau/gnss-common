package com.lucendar.gnss.service.db.alm;

import com.lucendar.gnss.sdk.alm.Alm;
import com.lucendar.gnss.sdk.alm.AlmParam;
import com.lucendar.gnss.sdk.alm.CloseAlmReq;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public interface AlmDao {

    List<AlmParam> getAlmParams(@NonNull String appId);
    AlmParam getAlmParam(@NonNull String appId, @NonNull String almTyp);
    void updateAlmParam(@NonNull AlmParam almParam);

    @NonNull
    Alm createAlm(@NonNull Alm alm);
    void closeAlm(@NonNull CloseAlmReq close);
    void bulkSaveAlms(@NonNull Alm[] alms, int count);
}
