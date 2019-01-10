package com.doors.utils

import com.doors.domain.Domain.{DoorEvent, DoorEventType}
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode

import scala.util.Try

/**
  * Created by okorolenko on 2019-01-09.
  */
object Converters {
  val toDoorEvent: ObjectNode => Try[DoorEvent] = (event: ObjectNode) =>
    Try {
      val eventVal = event.get("value")
      DoorEvent(
        eventVal.get("doorId").asInt(),
        DoorEventType.withName(eventVal.get("eventType").asText()),
        eventVal.get("timestamp").asLong()
      )
    }
}
