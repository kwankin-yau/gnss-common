/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.db.trksaver

import io.prometheus.metrics.core.datapoints.Timer
import io.prometheus.metrics.core.metrics.{Counter, Histogram}

case class TrkSaverMetrics(
                         histogram: Histogram,
                         timer: Timer,
                         successTrkCounter: Counter,
                         failureTrkCounter: Counter
                       )
