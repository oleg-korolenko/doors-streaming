package com.doors

/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */


import java.util.Properties

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}
import org.apache.flink.streaming.util.serialization.{JSONKeyValueDeserializationSchema, KeyedSerializationSchema}
import org.apache.flink.streaming.api.scala._

import scala.util.Try

case class DoorEvent(id: Int, state: Int, timestamp: Long)

object DoorsJob {


  private val DOORS_INPUT_TOPIC = "doors"
  private val DOORS_CLEANED_TOPIC = "doors-cleaned"
  private val KAFKA_GROUP_ID = "flink"
  private val KAFKA_SERVER = "kafka:9092"
 // private val KAFKA_SERVER = "localhost:9092"

  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val props = new Properties()
    props.setProperty("bootstrap.servers", KAFKA_SERVER)
    props.setProperty("group.id", KAFKA_GROUP_ID)

    val source = new FlinkKafkaConsumer[ObjectNode](
      DOORS_INPUT_TOPIC,
      new JSONKeyValueDeserializationSchema(true),
      props
    )

    val sink = new FlinkKafkaProducer[DoorEvent](
      DOORS_CLEANED_TOPIC,
      new KeyedSerializationSchema[DoorEvent] {
        override def serializeKey(element: DoorEvent): Array[Byte] = {
          element.id.toString.getBytes
        }

        override def serializeValue(element: DoorEvent): Array[Byte] = {
          s"id:${element.id.toString},state:${element.state.toString},timestamp:${element.timestamp.toString}".getBytes
        }

        override def getTargetTopic(element: DoorEvent): String = null
      },
      props
    )

    val doorsInput = env.addSource(source).name("Doors input stream")

    val toDoorEvent = (event: ObjectNode) =>
      Try {
        val eventVal = event.get("value")
        DoorEvent(
          eventVal.get("id").asInt(),
          eventVal.get("state").asInt(),
          eventVal.get("timestamp").asLong()
        )
      }

    val doorsStream = doorsInput
      .map(toDoorEvent)
      .filter(_.isSuccess)
      .map(_.get)

    doorsStream
      .addSink(sink)
      .name("Doors cleaned sink")

    env.execute("Doors Job")

  }
}