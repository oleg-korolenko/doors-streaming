package com.doors.domain

import com.doors.domain.Domain.DoorEventType.DoorEventType


/**
  * Created by okorolenko on 2018-12-21.
  */
object Domain {

  object DoorEventType extends Enumeration {
    type DoorEventType = Value
    val in, out = Value
  }

  final case class DoorEvent(doorId: Int, eventType: DoorEventType, timestamp: Long) extends Serializable


  type PerDoorCountsWithEndTime = (PerDoorCounts, Long)

  final case class Period(start: Long, end: Long)

  sealed trait DoorStatsValue

  final case class PerDoorCounts(doorId: String, count: Int, inCount: Int, outCount: Int) extends DoorStatsValue

  final case class TotalCounts(period: Period, count: Int) extends DoorStatsValue

  final case class DoorStats[T](key: String, value: T)


}
