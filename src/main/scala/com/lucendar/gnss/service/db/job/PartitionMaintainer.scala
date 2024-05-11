package com.lucendar.gnss.service.db.job

import akka.actor.{Actor, Props, Timers}
import com.lucendar.common.db.jdbc.DbHelper.JdbcContext
import com.lucendar.common.db.types.SqlDialect
import com.lucendar.gnss.service.db.job.PartitionMaintainer.Job

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
 * Execute partition maintain job every 24 hours
 *
 * @param jdbcCtx
 * @param sqlDialect
 */
class PartitionMaintainer(jdbcCtx: JdbcContext, sqlDialect: SqlDialect) extends Actor with Timers {

  private final val job = new PartitionMaintainJob(jdbcCtx, sqlDialect)

  override def preStart(): Unit = {
    timers.startTimerAtFixedRate(Job, Job, FiniteDuration(1, TimeUnit.DAYS))
  }

  override def receive: Receive = {
    case Job =>
      job.exec()
  }

}

object PartitionMaintainer {
  private case object Job

  def props(jdbcCtx: JdbcContext, sqlDialect: SqlDialect): Props =
    Props(new PartitionMaintainer(jdbcCtx, sqlDialect))
}