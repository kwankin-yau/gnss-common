/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */

package com.lucendar.gnss.service.db.recid

import akka.actor.ActorRef
import com.lucendar.gnss.sdk.db.RecIdProvider
import com.lucendar.gnss.sdk.db.SeqValues.SeqValueConfig
import com.lucendar.gnss.service.db.SysDao
import com.typesafe.scalalogging.Logger

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

/**
 * 由数据库(序列号)支持的记录ID提供者。可以使用 DbRecIdProvider.create() 方法来创建本对象实例。
 *
 * @param config     序列号配置
 * @param init       初始段
 * @param maintainer 序列号维护 Actor
 */
class DbRecIdProvider(
                       val config: SeqValueConfig,
                       init      : SeqValueSegment,
                       maintainer: ActorRef
                     ) extends RecIdProvider {

  private val segmentQueue = new ArrayBlockingQueue[SeqValueSegment](2)
  segmentQueue.offer(init)

  private var currSegment: SeqValueSegment = _

  override def nextId(): String = {
    synchronized {
      if (currSegment == null) {
        currSegment = segmentQueue.poll(5, TimeUnit.SECONDS)
        if (currSegment == null)
          return null
      }

      val opt = currSegment.get
      val value =
        if (opt.isEmpty) {
          currSegment = segmentQueue.poll(5, TimeUnit.SECONDS)
          if (currSegment == null)
            return null

          currSegment.get.get
        } else
          opt.get

      if (value == currSegment.threshold)
        maintainer ! RecIdMaintainReq(this)

      value.toString
    }
  }

  def put(seqValueSegment: SeqValueSegment): Unit =
    segmentQueue.put(seqValueSegment)

}

object DbRecIdProvider {
  private val logger = Logger[DbRecIdProvider]

  private def getInitSeqValueSegment(sysRepository: SysDao, config: SeqValueConfig): SeqValueSegment = {

    val v = sysRepository.nextSeqValue(config.seqName)
    val r = new SeqValueSegment(config, v)

    logger.whenDebugEnabled {
      logger.debug(s"Fetch initialize seq value $config successfully.")
    }

    r
  }

  def create(
              sysRepository     : SysDao,
              seqValueConfig    : SeqValueConfig,
              seqValueMaintainer: ActorRef
            ): RecIdProvider = {
    val segment = getInitSeqValueSegment(sysRepository, seqValueConfig)
    new DbRecIdProvider(seqValueConfig, segment, seqValueMaintainer)
  }

}


class SeqValueSegment(val config: SeqValueConfig, val start: Long) {
  private var curr: Long = start
  private val next: Long = start + config.incr
  val threshold: Long = next - (next - start) / 4

  def get: Option[Long] = {
    if (curr == next)
      return None

    val r = curr
    curr += 1

    Some(r)
  }
}

case class RecIdMaintainReq(seqValueProvider: DbRecIdProvider)
