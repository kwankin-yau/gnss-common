/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */

package com.lucendar.gnss.service.db.recid

import akka.actor.{Actor, Props, Timers}
import com.lucendar.gnss.service.db.SysDao
import com.typesafe.scalalogging.Logger

import java.util
import scala.concurrent.duration
import scala.concurrent.duration.Duration

class DbRecIdProvMaintainer(sysDao: SysDao) extends Actor with Timers {

  import DbRecIdProvMaintainer.LOGGER

  private final val pending: java.util.Map[String, DbRecIdProvider] = new util.HashMap[String, DbRecIdProvider]()

  private def putNewSegment(provider: DbRecIdProvider): Boolean = {
    val config = provider.config

    try {
      val v = sysDao.nextSeqValue(config.seqName)
      val segment = new SeqValueSegment(config, v)
      provider.put(segment)

      LOGGER.whenDebugEnabled {
        LOGGER.debug(s"Fetch next segment for ${config.seqName} successfully.")
      }

      pending.remove(provider.config.seqName())

      true
    } catch {
      case t: Throwable =>
        LOGGER.whenDebugEnabled {
          LOGGER.error(s"Error occurred when fetch next segment for ${config.seqName}.", t)
        }
        false
    }
  }

  private def updateNewSegmentFor(provider: DbRecIdProvider): Unit = {
    if (!putNewSegment(provider)) {
      timers.startSingleTimer(
        DbRecIdProvMaintainer.RetryTimerKey,
        DbRecIdProvMaintainer.Retry(provider),
        Duration(5, duration.SECONDS)
      )
    }
  }

  override def receive: Receive = {
    case RecIdMaintainReq(provider) =>
      if (!pending.containsKey(provider.config.seqName()))
        updateNewSegmentFor(provider)

    case DbRecIdProvMaintainer.Retry(provider) =>
      updateNewSegmentFor(provider)
  }

}

object DbRecIdProvMaintainer {
  private final val LOGGER = Logger("gateway.dbRecIdProvMaintainer")

  def props(sysDao: SysDao): Props =
    Props(new DbRecIdProvMaintainer(sysDao))

  private case object RetryTimerKey

  private case class Retry(provider: DbRecIdProvider)

}
