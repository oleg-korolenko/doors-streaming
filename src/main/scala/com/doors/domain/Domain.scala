package com.doors.domain

import com.doors.domain.Domain.DoorEventType.DoorEventType
import com.doors.domain.Domain.StatsType.StatsType


/**
  * Created by okorolenko on 2018-12-21.
  */
object Domain {

  object DoorEventType extends Enumeration {
    type DoorEventType = Value
    val in, out = Value
  }

  final case class DoorEvent(doorId: Int, eventType: DoorEventType, timestamp: Long) extends Serializable

  final  val DOORS_NUMBER = 200
  final  val ALL_DOORS_RANGE = 1 to DOORS_NUMBER

  type PerDoorCountsWithEndTime = (PerDoorCounts, Long)

  final case class Period(start: Long, end: Long)

  sealed trait DoorStatsValue

  final case class PerDoorCounts(doorId: String, count: Int, inCount: Int, outCount: Int) extends DoorStatsValue

  final case class TotalCounts(period: Period, count: Int) extends DoorStatsValue

  final case class DoorStats[T](key: StatsType, value: T)

  object StatsType extends Enumeration {
    type StatsType = Value
    val door_most_used, door_less_used, door_max_outs, door_max_ins,
    less_busy_window, busiest_window = Value
  }

}
