package com.lucendar.gnss.service.db.alm;

import com.lucendar.common.db.jdbc.*;
import com.lucendar.common.db.types.SqlDialect;
import com.lucendar.gnss.sdk.alm.Alm;
import com.lucendar.gnss.sdk.alm.AlmParam;
import com.lucendar.gnss.sdk.alm.CloseAlmReq;
import com.lucendar.gnss.sdk.db.RecIdProvider;
import com.lucendar.gnss.service.db.AbstractJdbcDao;
import com.lucendar.gnss.service.db.RowAccessor;
import com.lucendar.gnss.service.memdb.LocalMemDb;
import com.lucendar.gnss.service.memdb.MemDb;
import de.bytefish.pgbulkinsert.row.SimpleRowWriter;
import de.bytefish.pgbulkinsert.util.PostgreSqlUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.postgresql.PGConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;

public class AlmDao_Pg extends AbstractJdbcDao implements AlmDao {

    private static final String ALM_PARAMS_PREFIX = MemDb.PUBLIC_PREFIX + "ap:";

    private static final Logger LOGGER = LoggerFactory.getLogger("gnss.almDaoPg");

    private final RecIdProvider almIdProvider;
    private final MemDb memDb;

    public AlmDao_Pg(@NonNull DataSource ds,
                     @NonNull SqlDialect sqlDialect,
                     @NonNull RecIdProvider almIdProvider,
                     boolean cached) {
        super(ds, sqlDialect);
        this.almIdProvider = almIdProvider;
        memDb = cached ? new LocalMemDb() : null;
    }

    private static final String SELECT_ALM_PARAM = """
            SELECT f_app_id,
                   f_typ,
                   f_disabled,
                   f_stat,
                   f_send_voice,
                   f_voice_text,
                   f_notify_clnt,
                   f_capture,
                   f_capture_chan_mask,
                   f_live_vid_mon,
                   f_live_vid_mon_chan_mask
            FROM t_alm_param
            """;

    public static final ResultSetMapper<AlmParam> AlmParamMapper = new ResultSetMapper<>() {
        @Override
        public AlmParam map(ResultSetAccessor acc) {
            AlmParam almParam = new AlmParam();
            almParam.setAppId(acc.str());
            almParam.setTyp(acc.str());
            almParam.setDisabled(acc.bool());
            almParam.setStat(acc.bool());
            almParam.setSendVoice(acc.bool());
            almParam.setVoiceText(acc.str());
            almParam.setNotifyClnt(acc.bool());
            almParam.setCapture(acc.bool());
            almParam.setCaptureChanMask(acc.small());
            almParam.setLiveVidMon(acc.bool());
            almParam.setLiveVidMonChanMask(acc.small());
            return almParam;
        }
    };

    @NonNull
    public List<AlmParam> dbGetAlmParams(@NonNull String appId) {
        var sql = SELECT_ALM_PARAM + " WHERE f_app_id=?";

        return qryList(sql, DbHelper.strStatementSetter(appId), AlmParamMapper);
    }

    @Override
    public List<AlmParam> getAlmParams(@NonNull String appId) {
        return dbGetAlmParams(appId);
    }

    public AlmParam dbGetAlmParam(@NonNull String appId, @NonNull String typ) {
        var sql = SELECT_ALM_PARAM + " WHERE f_app_id=? AND f_typ=?";

        return qryObject(sql, DbHelper.twoStrStatementSetter(appId, typ), AlmParamMapper);
    }

    @Nullable
    @Override
    public AlmParam getAlmParam(@NonNull String appId, @NonNull String almTyp) {
        if (memDb != null) {
            AlmParam r = memDb.getJsonObject(ALM_PARAMS_PREFIX,
                    appId + ":" + almTyp,
                    AlmParam.class);
            if (r != null)
                return r;
        }

        var ap = dbGetAlmParam(appId, almTyp);
        if (ap != null && memDb != null)
            memDb.setJsonObject(ALM_PARAMS_PREFIX, appId + ":" + almTyp, ap);

        return ap;
    }

    private static final String UPDATE_ALM_PARAM = """
            UPDATE t_alm_param
            SET f_disabled = ?,
                f_stat = ?,
                f_send_voice = ?,
                f_voice_text = ?,
                f_notify_clnt = ?,
                f_capture = ?,
                f_capture_chan_mask = ?,
                f_live_vid_mon = ?,
                f_live_vid_mon_chan_mask = ?
            WHERE f_app_id = ? AND f_typ = ?
            """;

    public void dbUpdateAlmParam(@NonNull AlmParam almParam) {
        update(UPDATE_ALM_PARAM, setter -> {
            setter.setBool(almParam.isDisabled());
            setter.setBool(almParam.isStat());
            setter.setBool(almParam.isSendVoice());
            setter.setString(almParam.getVoiceText());
            setter.setBool(almParam.isNotifyClnt());
            setter.setBool(almParam.isCapture());
            setter.setShort(almParam.getCaptureChanMask());
            setter.setBool(almParam.isLiveVidMon());
            setter.setShort(almParam.getLiveVidMonChanMask());
        });
    }

    @Override
    public void updateAlmParam(@NonNull AlmParam almParam) {
        if (memDb != null)
            memDb.setJsonObject(ALM_PARAMS_PREFIX, almParam.getAppId() + ":" + almParam.getTyp(), almParam);

        dbUpdateAlmParam(almParam);
    }

    private static final String INSERT_ALM = """
            INSERT INTO t_alm (f_id, f_app_id, f_sim_no, f_veh_id, f_grp_id,
                               f_trk_id, f_typ, f_sub_typ, f_src, f_lvl,
                               f_actv, f_tm1, f_recv_tm1, f_lng1, f_lat1,
                               f_spd1, f_rec_spd1, f_alt1, f_dir1, f_addt1,
                               f_tm0, f_recv_tm0, f_lng0, f_lat0, f_spd0,
                               f_rec_spd0, f_alt0, f_dir0, f_addt0, f_dur,
                               f_att_cnt, f_drv_name, f_drv_no, f_plat_rgn_id, f_proc_st,
                               f_proc_meth, f_proc_tm, f_opr_name, f_opr_corp_name, f_superv_id,
                               f_superv_tm, f_superv_deadline, f_superv_reply_needed)
            VALUES (?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?);
            """;

    private StatementSetter insertAlmSetter(Alm alm) {
        return new StatementSetter() {
            @Override
            public void set(StatementBinder binder) {
                binder.setString(alm.getId());
                binder.setString(alm.getAppId());
                binder.setString(alm.getSimNo());
                binder.setString(alm.getVehId());
                binder.setString(alm.getGrpId());

                binder.setString(alm.getTrkId());
                binder.setString(alm.getTyp());
                binder.setString(alm.getSubTyp());
                binder.setInt(alm.getSrc());
                binder.setIntObject(alm.getLvl());


                binder.setBool(alm.isActv());
                binder.setBeijingConvOdt(alm.getTm1());
                binder.setBeijingConvOdt(alm.getRecvTm1());
                binder.setDouble(alm.getLng1());
                binder.setDouble(alm.getLat1());

                binder.setFloat(alm.getSpd1());
                binder.setFloat(alm.getRecSpd1());
                binder.setInt(alm.getAlt1());
                binder.setInt(alm.getDir1());
                binder.setString(alm.addt1ToJson());


                binder.setBeijingConvOdt(alm.getTm0());
                binder.setBeijingConvOdt(alm.getRecvTm0());
                binder.setDouble(alm.getLng0());
                binder.setDouble(alm.getLat0());
                binder.setFloat(alm.getSpd0());

                binder.setFloat(alm.getRecSpd0());
                binder.setInt(alm.getAlt0());
                binder.setInt(alm.getDir0());
                binder.setString(alm.addt0ToJson());
                binder.setIntObject(alm.getDur());


                binder.setInt(alm.getAttCnt());
                binder.setString(alm.getDrvName());
                binder.setString(alm.getDrvNo());
                binder.setString(alm.getPlatRgnId());
                binder.setIntObject(alm.getProcSt());

                binder.setIntObject(alm.getProcMeth());
                binder.setString(alm.getOprName());
                binder.setString(alm.getOprCorpName());
                binder.setString(alm.getSupervId());

                binder.setBeijingConvOdt(alm.getSupervTm());
                binder.setBeijingConvOdt(alm.getSupervDeadline());
                binder.setBoolObject(alm.getSupervReplyNeeded());
            }
        };
    }

    ;

    @NonNull
    @Override
    public Alm createAlm(@NonNull Alm alm) {
        if (alm.getId() == null)
            alm.setId(almIdProvider.nextId());

        update(INSERT_ALM, insertAlmSetter(alm));

        return alm;
    }

    @Override
    public void closeAlm(@NonNull CloseAlmReq req) {
        String CALL = "{ ? = call p_close_alm(?, ?, ?, ?, ?::real,   ?::real, ?, ?, ?, ?::d_id, ?) }";

        call(CALL, (conn, bind) -> {
            bind.registerOutParameter(Types.BOOLEAN);

            bind.setBeijingConvOdt(req.getTm0());
            bind.setBeijingConvOdt(req.getRecvTm0());
            bind.setDouble(req.getLng0());
            bind.setDouble(req.getLat0());
            bind.setSingle(req.getSpd0());

            bind.setSingleObject(req.getRecSpd0());
            bind.setShort(req.getAlt0());
            bind.setShort(req.getDir0());
            bind.setString(req.addt0ToJson());

            bind.setString(req.getId());
            bind.setBeijingConvOdt(req.getTm1());

            bind.execute();

            return bind.getBool(1);
        });
    }

    private static final String[] ALM_COLUMNS = new String[]{
            "f_id",
            "f_app_id",
            "f_sim_no",
            "f_veh_id",
            "f_grp_id",
            "f_trk_id",
            "f_typ",
            "f_sub_typ",
            "f_src",
            "f_lvl",
            "f_actv",
            "f_tm1",
            "f_recv_tm1",
            "f_lng1",
            "f_lat1",
            "f_spd1",
            "f_rec_spd1",
            "f_alt1",
            "f_dir1",
            "f_addt1",
            "f_tm0",
            "f_recv_tm0",
            "f_lng0",
            "f_lat0",
            "f_spd0",
            "f_rec_spd0",
            "f_alt0",
            "f_dir0",
            "f_addt0",
            "f_dur",
            "f_att_cnt",
            "f_drv_name",
            "f_drv_no",
            "f_plat_rgn_id",
            "f_proc_st",
            "f_proc_meth",
            "f_proc_tm",
            "f_opr_name",
            "f_opr_corp_name",
            "f_superv_id",
            "f_superv_tm",
            "f_superv_deadline",
            "f_superv_reply_needed"
    };

    private final static SimpleRowWriter.Table AlmTable = new SimpleRowWriter.Table("t_alm", ALM_COLUMNS);

    @Override
    public void bulkSaveAlms(@NonNull Alm[] alms, int count) {
        if (count == 0)
            return;

        try (Connection conn = ds.getConnection()) {
            PGConnection pg = PostgreSqlUtils.getPGConnection(conn);
            try (SimpleRowWriter writer = new SimpleRowWriter(AlmTable, pg)) {
                for (int i = 0; i < count; i++) {
                    Alm alm = alms[i];
                    writer.startRow(row -> {
                        var acc = new RowAccessor(row);

                        acc.str(alm.getId()); // "f_id",
                        acc.str(alm.getAppId()); // "f_app_id",
                        acc.str(alm.getSimNo()); //    "f_sim_no",
                        acc.str(alm.getVehId()); //    "f_veh_id",
                        acc.str(alm.getGrpId()); //    "f_grp_id",
                        acc.str(alm.getTrkId()); //    "f_trk_id",
                        acc.str(alm.getTyp()); //    "f_typ",
                        acc.str(alm.getSubTyp()); //    "f_sub_typ",
                        acc.int16(alm.getSrc()); //    "f_src",
                        acc.int32(alm.getLvl()); //    "f_lvl",
                        acc.boo(alm.isActv()); //    "f_actv",

                        acc.tsz(alm.getTm1()); //    "f_tm1",
                        acc.tsz(alm.getRecvTm1()); //    "f_recv_tm1",
                        acc.dbl(alm.getLng1()); //    "f_lng1",
                        acc.dbl(alm.getLat1()); //    "f_lat1",
                        acc.fld(alm.getSpd1()); //    "f_spd1",
                        acc.fld(alm.getRecSpd1()); //    "f_rec_spd1",
                        acc.int32(alm.getAlt1()); //    "f_alt1",
                        acc.int32(alm.getDir1()); //    "f_dir1",
                        acc.str(alm.addt1ToJson()); //    "f_addt1",

                        acc.tsz(alm.getTm0()); //    "f_tm0",
                        acc.tsz(alm.getRecvTm0()); //    "f_recv_tm0",
                        acc.dbl(alm.getLng0()); //    "f_lng0",
                        acc.dbl(alm.getLat0()); //    "f_lat0",
                        acc.fld(alm.getSpd0()); //    "f_spd0",
                        acc.fld(alm.getRecSpd0()); //    "f_rec_spd0",
                        acc.int32(alm.getAlt0()); //    "f_alt0",
                        acc.int32(alm.getDir0()); //    "f_dir0",
                        acc.str(alm.addt0ToJson()); //    "f_addt0",

                        acc.int32(alm.getDur()); //                                "f_dur",
                        acc.int32(alm.getAttCnt()); //                                "f_att_cnt",
                        acc.str(alm.getDrvName()); //                                "f_drv_name",
                        acc.str(alm.getDrvNo()); //                                "f_drv_no",
                        acc.str(alm.getPlatRgnId()); //                                "f_plat_rgn_id",
                        acc.int16(alm.getProcSt()); //                                "f_proc_st",
                        acc.int32(alm.getProcMeth()); //                                "f_proc_meth",
                        acc.tsz(alm.getProcTm()); //                                "f_proc_tm",
                        acc.str(alm.getOprName()); //                                "f_opr_name",
                        acc.str(alm.getOprCorpName()); //                                "f_opr_corp_name",
                        acc.str(alm.getSupervId()); //                                "f_superv_id",
                        acc.tsz(alm.getSupervTm()); //                                "f_superv_tm",
                        acc.tsz(alm.getSupervDeadline()); //                                "f_superv_deadline",
                        acc.boo(alm.getSupervReplyNeeded()); //                                "f_superv_reply_needed"
                    });
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Error occurred when bulk save alarm to database", t);
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else
                throw new RuntimeException(t);
        }
    }
}
