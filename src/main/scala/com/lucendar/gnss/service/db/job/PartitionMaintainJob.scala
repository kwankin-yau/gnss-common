package com.lucendar.gnss.service.db.job

import com.lucendar.common.db.jdbc.DbHelper.JdbcContext
import com.lucendar.common.db.jdbc.DbSupport
import com.lucendar.common.db.types.SqlDialect
import com.lucendar.gnss.service.db.job.PartitionMaintainJob.LOGGER
import com.typesafe.scalalogging.Logger

class PartitionMaintainJob(val jdbcCtx: JdbcContext, val sqlDialect: SqlDialect) extends DbSupport {

  private def safeExec(sql: String): Unit = {
    try {
      execute(sql)

      LOGGER.debug(s"SQL executed: $sql")
    } catch {
      case t: Throwable =>
        LOGGER.error(s"Error occurred when execute SQL: $sql", t)
    }
  }

  /**
   * Do not throws exception
   */
  def exec(): Unit = {
    safeExec("SELECT p_create_partitions()")
    safeExec("SELECT p_delete_old_parts()")
  }


}

object PartitionMaintainJob {
  private final val LOGGER = Logger("gnss.partitionMaintainJob")
}