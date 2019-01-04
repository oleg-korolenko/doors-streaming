package com.doors.transformations

import com.doors.domain.Domain.{DoorEvent, DoorEventType, PerDoorCounts}
import org.apache.flink.api.common.functions.AggregateFunction

/**
  * Created by okorolenko on 2019-01-03.
  */
class PerDoorAggregation
  extends AggregateFunction[DoorEvent, PerDoorCounts, PerDoorCounts] {
  override def createAccumulator(): PerDoorCounts = PerDoorCounts("", 0, 0, 0)

  override def add(event: DoorEvent, acc: PerDoorCounts): PerDoorCounts = {
    val inCount = if (event.eventType.equals(DoorEventType.in)) 1 else 0
    val outCount = if (inCount == 1) 0 else 1
    PerDoorCounts(event.doorId.toString, acc.count + 1, acc.inCount + inCount, acc.outCount + outCount)
  }

  override def getResult(acc: PerDoorCounts): PerDoorCounts = acc

  override def merge(a: PerDoorCounts, b: PerDoorCounts): PerDoorCounts = {
    PerDoorCounts(a.doorId, a.count + b.count, a.inCount + b.inCount, a.outCount + b.outCount)
  }
}