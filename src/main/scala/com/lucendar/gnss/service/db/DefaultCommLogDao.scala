/*
 * Copyright (c) 2024 lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.db

import com.lucendar.common.db.jdbc.{DbHelper, ResultSetAccessor, ResultSetMapper, StatementBinder}
import com.lucendar.common.db.types.SqlDialect
import com.lucendar.gnss.service.db.DefaultCommLogDao.{DATA_SZ_MAPPER, MAPPER}
import com.lucendar.strm.common.types.CommLog
import org.checkerframework.checker.nullness.qual.NonNull

import java.util
import javax.sql.DataSource

class DefaultCommLogDao(@NonNull ds: DataSource, @NonNull sqlDialect: SqlDialect)
  extends AbstractJdbcDao(ds, sqlDialect) with CommLogDao {

  override def qryGatewayCommLog(
                                  startTs: Long,
                                  appId  : String,
                                  simNo  : String,
                                  retData: Boolean,
                                  limit  : Int,
                                  offset : Int
                                ): util.List[CommLog] = {


    val (sql, mapper) =
      if (retData)
        (
          """
       SELECT f_id, f_ts, f_app_id, f_sim_no, f_evt_typ, f_desc, f_data
       FROM t_comm_log
       WHERE f_ts >= ? AND f_app_id = ? AND f_sim_no = ?
       LIMIT %d OFFSET %d
       """.formatted(limit, offset),
          MAPPER
        )
      else (
        """
       SELECT f_id, f_ts, f_app_id, f_sim_no, f_evt_typ, f_desc, length(f_data) AS f_data_sz
       FROM t_comm_log
       WHERE f_ts >= ? AND f_app_id = ? AND f_sim_no = ?
       LIMIT %d OFFSET %d
       """.formatted(limit, offset),
        DATA_SZ_MAPPER
      )


    qryList(
      sql, (setter: StatementBinder) => {
        setter.setBeijingConvOdt(startTs)
        setter.setString(appIdDef(appId))
        setter.setString(simNo)
      }, mapper
    )
  }


  override def saveCommLogs(commLogs: util.List[CommLog]): Unit = {
    val sql =
      """
       INSERT INTO t_comm_log (f_ts, f_app_id, f_sim_no, f_evt_typ, f_desc, f_data)
       VALUES (?, ?, ?, ?, ?, ?)
       """

    dbAction(
      conn => {
        DbHelper.batchUpdate(
          sql,
          commLogs,
          (commLog: CommLog, binder: StatementBinder) => {
            binder.setBeijingConvOdt(commLog.getTs)
            binder.setString(commLog.getAppId)
            binder.setString(commLog.getSimNo)
            binder.setString(commLog.getEvtTyp)
            binder.setString(commLog.getDesc)
            binder.setBytes(commLog.getBinaryData)
          }
        )(conn)
      }
    )
  }
}

object DefaultCommLogDao {

  private val MAPPER: ResultSetMapper[CommLog] = (acc: ResultSetAccessor) => {
    val r = new CommLog
    r.setId(acc.str())
    r.setTs(acc.epochMillisLong())
    r.setAppId(acc.str())
    r.setSimNo(acc.str())
    r.setEvtTyp(acc.str())
    r.setDesc(acc.str())
    r.setBinaryData(acc.byteArray())
    r
  }

  private val DATA_SZ_MAPPER: ResultSetMapper[CommLog] = (acc: ResultSetAccessor) => {
    val r = new CommLog
    r.setId(acc.str())
    r.setTs(acc.epochMillisLong())
    r.setAppId(acc.str())
    r.setSimNo(acc.str())
    r.setEvtTyp(acc.str())
    r.setDesc(acc.str())
    r.setDataSz(acc.int32())
    r
  }
}
