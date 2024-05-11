/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.db.trk

import com.google.gson.Gson
import com.lucendar.common.db.types.{ServerVer, SqlDialect}
import com.lucendar.gnss.service.db.trk.TrkDao_Pg.LOGGER
import com.lucendar.gnss.service.db.{AbstractJdbcDao, RowAccessor}
import com.typesafe.scalalogging.Logger
import de.bytefish.pgbulkinsert.row.{SimpleRow, SimpleRowWriter}
import de.bytefish.pgbulkinsert.util.PostgreSqlUtils
import info.gratour.jt808common.protocol.msg.types.trk.Trk
import org.checkerframework.checker.nullness.qual.NonNull
import org.springframework.transaction.annotation.Transactional

import javax.sql.DataSource
import scala.util.Using

class TrkDao_Pg(@NonNull ds: DataSource, @NonNull sqlDialect: SqlDialect, @NonNull gson: Gson)
  extends AbstractJdbcDao(ds, sqlDialect) with TrkDao {

  private final val serverVer: ServerVer = sqlDialect.getServerVer(ds)
  private final val pg15OrNewer = ServerVer(15, 0).compare(serverVer) >= 0


  private final val fields = Array(
    "f_id", "f_sim_no", "f_veh_id", "f_plate_no", "f_plate_clr", "f_recv_tm", "f_gps_tm", "f_retrans", "f_alm", "f_st",
    "f_lng", "f_lat", "f_alt", "f_spd", "f_rec_spd", "f_dir", "f_signal", "f_io_st", "f_vid_alm", "f_adas_alm",
    "f_mile", "f_gas", "f_drv_name", "f_drv_no", "f_addt"
  )

  // f_id, f_sim_no, f_veh_id, f_plate_no, f_plate_clr...
  private final val fieldsStr: String = fields.mkString(",")

  // id, sim_no, veh_id, plate_no, plate_clr...
  private final val virtualFieldsStr: String = fields.map(_.substring(2)).mkString(",")

  // (?, ?, ?, ?...)
  private final val paramsPlaceHolders: String = fields.map(_ => "?").mkString("(", ",", ")")

  // [id, sim_no, veh_id, plate_no, plate_clr...]  (without f_sim_no)
  private final val updateFields: Array[String] = fields.filter(_ != "f_sim_no")

  // f_id, f_veh_id, f_plate_no, f_plate_clr...  (without f_sim_no)
  private final val updateFieldsStr: String = updateFields.mkString(",")

  // excluded.f_id, excluded.f_veh_id, excluded.f_plate_no, excluded.f_plate_clr...  (without excluded.f_sim_no)
  private final val updateExcludedFieldsStr: String = updateFields.map(f => "excluded." + f).mkString(",")

  // id, veh_id, plate_no, plate_clr...  (without sim_no)
  private final val virtualUpdateFieldsStr = updateFields.map(_.substring(2)).mkString(",")

  // vals.id, vals.sim_no, vals.veh_id, vals.plate_no, vals.plate_clr...
  private final val valsPrefixedInsertFields: String = fields.map(f => "vals." + f.substring(2)).mkString(",")

  // vals.id, vals.veh_id, vals.plate_no, vals.plate_clr...  (without sim_no)
  private final val valsPrefixedUpdateFields: String = updateFields.map(f => "vals." + f).mkString(",")



  private final val table: SimpleRowWriter.Table = new SimpleRowWriter.Table(
    "t_trk", fields: _*
  )

  private def executeUpsert(sql: String, trks: Array[Trk], count: Int): Unit = {
    update(
      sql, binder => {
        for (i <- 0 until count) {
          val trk = trks(i)

          binder.setString(trk.getId)
          binder.setString(trk.getSimNo)
          binder.setString(trk.getVehId)
          binder.setString(trk.getPlateNo)
          binder.setIntObject(trk.getPlateColor)
          binder.setBeijingConvOdt(trk.getRecvTm)
          binder.setBeijingConvOdt(trk.getGpsTm)
          binder.setBool(trk.isReTrans)
          binder.setInt(trk.getAlm)
          binder.setInt(trk.getSt)
          binder.setDouble(trk.getLng)
          binder.setDouble(trk.getLat)
          binder.setInt(trk.getAlt)
          binder.setSingle(trk.getSpd)
          binder.setSingleObject(trk.getRecSpd)
          binder.setInt(trk.getDir)
          binder.setIntObject(trk.getSignal)
          binder.setIntObject(trk.getIoSt)
          binder.setIntObject(trk.getVidAlm)
          binder.setIntObject(trk.getAdasAlm)
          binder.setSingleObject(trk.getMile)
          binder.setSingleObject(trk.getGas)
          binder.setString(trk.getDrvName)
          binder.setString(trk.getDrvNo)
          val s = if (trk.getAddt != null) gson.toJson(trk.getAddt) else null
          binder.setString(s)
        }
      }
    )
  }


  /**
   *
   * @param trks
   * @param count
   * @param params (?, ?, ?...)[, (?, ?, ?...)]
   */
  private def bulkSaveLatestTrksUseUpsert(trks: Array[Trk], count: Int, params: String): Unit = {
//    val sql =
//      s"""
//    WITH vals ($virtualFieldsStr) AS (VALUES $params),
//    upd (upd_sim_no) AS (
//        UPDATE t_latest_trk ut SET
//          ($updateFieldsStr)
//          = ($virtualUpdateFieldsStr)
//        FROM vals
//        WHERE ut.f_sim_no = vals.sim_no
//        RETURNING ut.f_sim_no
//      )
//    INSERT INTO t_latest_trk($fieldsStr)
//      SELECT $virtualFieldsStr
//      FROM vals LEFT JOIN upd ON upd.upd_sim_no = vals.sim_no
//      WHERE upd_sim_no IS NULL
//    """

    val sql = s"""
    INSERT INTO t_latest_trk ($fieldsStr)
    VALUES $params
    ON CONFLICT (f_sim_no) DO UPDATE SET ($updateFieldsStr) = ($updateExcludedFieldsStr)
    """

    executeUpsert(sql, trks, count)
  }

  // The pg version must be 15 or above
  // Warning: The MERGE in postgresql may occurred key violation if there is a concurrent insert.
  private def bulkSaveLatestTrksUseMerge(trks: Array[Trk], count: Int, params: String): Unit = {
    val sql =
      s"""
      MERGE INTO t_latest_trk t
      USING (VALUES $params) AS vals($virtualFieldsStr)
      ON t.f_sim_no = vals.sim_no
      WHEN MATCHED THEN
        UPDATE SET ($updateFieldsStr) = ($valsPrefixedUpdateFields)
      WHEN NOT MATCHED THEN
        INSERT ($fieldsStr)
        VALUES ($valsPrefixedInsertFields)
      """

    executeUpsert(sql, trks, count)
  }

  /**
   * 批量保存实时轨迹
   *
   * @param trks  轨迹数组
   * @param count 轨迹数量
   */
  @Transactional
  override def bulkSaveLatestTrks(trks: Array[Trk], count: Int): Unit = {
    val params = (1 to count).map(_ => paramsPlaceHolders).mkString(",")

//    if (pg15OrNewer) {
//      bulkSaveLatestTrksUseMerge(trks, count, params)
//    } else

    // Note: The MERGE in postgresql may occurred key violation if there is a concurrent insert.
    // So, we use INSERT ON CONFLICT statement even if the PG version is 15 or above
    bulkSaveLatestTrksUseUpsert(trks, count, params)
  }

  /**
   * 批量保存历史轨迹
   *
   * @param trks  轨迹数组
   * @param count 轨迹数量
   * @note The transaction is managed in this method's internal.
   */
  override def bulkSaveHisTrks(trks: Array[Trk], count: Int): Unit = {
    if (count == 0) return
    Using(ds.getConnection) { conn => {
      val pg = PostgreSqlUtils.getPGConnection(conn)

      Using.resource(new SimpleRowWriter(table, pg)) { writer => {
        for (i <- 0 until count) {
          val trk = trks(i)
          writer.startRow(
            (r: SimpleRow) => {
              val acc = new RowAccessor(r)
              acc.str(trk.getId) // f_id
              acc.str(trk.getSimNo) // f_sim_no
              acc.str(trk.getVehId) // f_veh_id
              acc.str(trk.getPlateNo) // f_plate_no
              acc.int16(trk.getPlateColor) // f_plate_clr
              acc.tsz(trk.getRecvTm) // f_recv_tm
              acc.tsz(trk.getGpsTm) // f_gps_tm
              acc.boo(trk.getReTrans) // f_retrans
              acc.int32(trk.getAlm) // f_alm
              acc.int32(trk.getSt) // f_st
              acc.dbl(trk.getLng) // f_lng
              acc.dbl(trk.getLat) // f_lat
              acc.int16(trk.getAlt) // f_alt
              acc.fld(trk.getSpd) // f_spd
              acc.fld(trk.getRecSpd) // f_rec_spd
              acc.int16(trk.getDir) // f_dir
              acc.int32(trk.getSignal) // f_signal
              acc.int16(trk.getIoSt) // f_io_st
              acc.int32(trk.getVidAlm) // f_vid_alm
              acc.int16(trk.getAdasAlm) // f_adas_alm
              acc.fld(trk.getMile) // f_mile
              acc.fld(trk.getGas) // f_gas
              acc.str(trk.getDrvName) // f_drv_name
              acc.str(trk.getDrvNo) // f_drv_no

              val addt = trk.getAddt
              var s: String = null
              if (addt != null) s = gson.toJson(addt)
              else s = null
              acc.str(s) // f_addt
            }
          )
        }
      }}
    }}.recover(t => {
      LOGGER.error("Error occurred when save trks.", t)
    })
  }
}

object TrkDao_Pg {
  private final val LOGGER = Logger("gnss.trkDaoPg")
}
