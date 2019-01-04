package com.doors.sinks

import java.util.Properties

import com.doors.domain.Domain.{DoorEvent, DoorStats, PerDoorCounts, TotalCounts}
import com.doors.utils.Json.JsonSyntax._
import com.doors.utils.Json.JsonConverterInstances._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema

/**
  * Created by okorolenko on 2019-01-04.
  */
object DoorSinks {

  val createDoorsSink = (props: Properties) => (topic: String) =>
    new FlinkKafkaProducer[DoorEvent](
      topic,
      new KeyedSerializationSchema[DoorEvent] {
        override def serializeKey(element: DoorEvent): Array[Byte] = {
          element.doorId.toString.getBytes
        }

        override def serializeValue(element: DoorEvent): Array[Byte] = {
          s"""
             |{
             |  "doorId":${element.doorId.toString},
             |  "eventType":"${element.eventType.toString}",
             |  "timestamp":${element.timestamp.toString}
             }""".stripMargin.getBytes
        }

        override def getTargetTopic(element: DoorEvent): String = null
      },
      props
    )


  def createDoorStatsSink (props: Properties) = (topic: String) =>
    new FlinkKafkaProducer[DoorStats[PerDoorCounts]](
      topic,
      new KeyedSerializationSchema[DoorStats[PerDoorCounts]] {
        override def serializeKey(element: DoorStats[PerDoorCounts]): Array[Byte] = {
          element.key.toString.getBytes
        }

        override def serializeValue(element: DoorStats[PerDoorCounts]): Array[Byte] = {
          s"""
             |
             |{"key":"${element.key}",
             |"value":${element.value.json}
             }""".stripMargin.getBytes
        }

        override def getTargetTopic(element: DoorStats[PerDoorCounts]): String = null
      },
      props
    )


  def createTotalStatsSink(props: Properties) = (topic: String) =>
    new FlinkKafkaProducer[DoorStats[TotalCounts]](
      topic,
      new KeyedSerializationSchema[DoorStats[TotalCounts]] {
        override def serializeKey(element: DoorStats[TotalCounts]): Array[Byte] = {
          element.key.toString.getBytes
        }

        override def serializeValue(element: DoorStats[TotalCounts]): Array[Byte] = {
          s"""
             |
             |{"key":"${element.key}",
             |"value":${element.value.json}
             }""".stripMargin.getBytes
        }

        override def getTargetTopic(element: DoorStats[TotalCounts]): String = null
      },
      props
    )
}
