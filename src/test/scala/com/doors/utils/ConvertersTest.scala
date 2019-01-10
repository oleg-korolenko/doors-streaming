package com.doors.utils

import com.doors.domain.Domain.{DoorEvent, DoorEventType}
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import org.scalatest.{FlatSpec, Matchers}
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper

import scala.util.{Failure, Success}

/**
  * Created by okorolenko on 2019-01-09.
  */
class ConvertersTest extends FlatSpec with Matchers {

  behavior of "toDoorEvent"

  it should "return Success on successful parsing of ObjectNode " in {
    val node: ObjectNode = JsonNodeFactory.instance.objectNode()
    val doorId = 1
    val eventType = DoorEventType.in
    val timestamp = 1234567l

    val json = s"""{"doorId":$doorId,"eventType":"${eventType.toString}","timestamp":$timestamp}"""
    node.set("value", new ObjectMapper().readTree(json))

    val result = Converters.toDoorEvent(node)
    result should be(Success(DoorEvent(doorId, eventType, timestamp)))
  }

  it should "return Failure on failure parsing of ObjectNode " in {
    val node: ObjectNode = JsonNodeFactory.instance.objectNode()
    val doorId = 1
    val timestamp = 1234567l

    val jsonWithNotEventType = s"""{"doorId":$doorId,"timestamp":$timestamp}"""
    node.set("value", new ObjectMapper().readTree(jsonWithNotEventType))

    val result = Converters.toDoorEvent(node)
    result shouldBe a [Failure[_]]
  }

}
