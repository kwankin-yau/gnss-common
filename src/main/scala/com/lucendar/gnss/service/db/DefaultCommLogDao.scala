/*
 * Copyright (c) 2024 lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.db

import com.lucendar.common.db.jdbc.{DbHelper, ResultSetAccessor, ResultSetMapper, StatementBinder}
import com.lucendar.common.db.types.SqlDialect
import com.lucendar.gnss.service.db.DefaultCommLogDao.MAPPER
import com.lucendar.strm.common.types.CommLog
import org.checkerframework.checker.nullness.qual.NonNull

import java.util
import javax.sql.DataSource

class DefaultCommLogDao(@NonNull ds: DataSource, @NonNull sqlDialect: SqlDialect)
  extends AbstractJdbcDao(ds, sqlDialect) with CommLogDao {

  override def qryGatewayCommLog(
                                  startTs: Long,
                                  appId: String,
                                  simNo: String,
                                  limit: Int,
                                  offset: Int
                                ): util.List[CommLog] = {
    var sql =
      """
       SELECT f_id, f_ts, f_app_id, f_sim_no, f_evt_typ, f_desc, f_data
       FROM t_comm_log
       WHERE f_ts >= ? AND f_app_id = ? AND f_sim_no = ?
       LIMIT %d OFFSET %d
       """

    sql = String.format(sql, limit, offset)

    qryList(
      sql, (setter: StatementBinder) => {
        setter.setOffsetDateTime(startTs)
        setter.setString(appIdDef(appId))
        setter.setString(simNo)

      }, MAPPER
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

}
