package com.lucendar.gnss.service.db.job

import akka.actor.{Actor, Props, Timers}
import com.lucendar.common.db.jdbc.DbHelper.JdbcContext
import com.lucendar.common.db.types.SqlDialect
import com.lucendar.gnss.service.db.job.PartitionMaintainer.Job
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.{CronExpression, CronTrigger}


/**
 * Execute partition maintain job every 24 hours
 *
 * @param jdbcCtx
 * @param sqlDialect
 * @param scheduler
 * @param cronExpression
 */
class PartitionMaintainer(jdbcCtx       : JdbcContext,
                          sqlDialect    : SqlDialect,
                          scheduler     : TaskScheduler,
                          cronExpression: String) extends Actor with Timers {

  private final val job = new PartitionMaintainJob(jdbcCtx, sqlDialect)

  override def preStart(): Unit = {
    scheduler.schedule(new Runnable {
      override def run(): Unit = {
        self ! Job
      }
    }, new CronTrigger(cronExpression))


    // first time execution
    job.exec()
  }

  override def receive: Receive = {
    case Job =>
      job.exec()
  }

}

object PartitionMaintainer {
  private case object Job

  def props(jdbcCtx: JdbcContext,
            sqlDialect: SqlDialect,
            scheduler: TaskScheduler,
            cronExpression: String): Props =
    Props(new PartitionMaintainer(jdbcCtx, sqlDialect, scheduler, cronExpression))
}