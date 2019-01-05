package com.doors.jobs

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

import com.doors.domain.Domain._
import com.doors.sinks.DoorSinks._
import com.doors.sources.DoorEventSource
import com.doors.transformations.PerDoorAggregation
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}
import org.apache.flink.streaming.util.serialization.{JSONKeyValueDeserializationSchema, KeyedSerializationSchema}
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._

import scala.util.Try


object DoorsJob {

  private val DOORS_INPUT_TOPIC = "doors"
  private val DOORS_STATS_TOPIC = "doors-stats"
  private val KAFKA_GROUP_ID = "flink"
  private val KAFKA_SERVER = "kafka:9092"
  //  private val KAFKA_SERVER = "localhost:9092"


  private val WINDOW_SIZE = Time.seconds(10)

  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


    val props = new Properties()
    props.setProperty("bootstrap.servers", KAFKA_SERVER)
    props.setProperty("group.id", KAFKA_GROUP_ID)

    // generation stream to kafka input
    val doorsGenerationStream =
      env
        .addSource(DoorEventSource()).name("Doors generation input stream")
        .addSink(createDoorsSink(props)(DOORS_INPUT_TOPIC)).name("Doors generation sink")

    // to not generate out-of-order
    doorsGenerationStream.setParallelism(1)

    //main  doors processing stream
    val source = new FlinkKafkaConsumer[ObjectNode](
      DOORS_INPUT_TOPIC,
      new JSONKeyValueDeserializationSchema(true),
      props
    )

    val doorsStream =
      env.addSource(source).name("Doors input stream")
        .map(toDoorEvent)
        .filter(_.isSuccess)
        .map(_.get)
        .assignAscendingTimestamps(_.timestamp)


    /*   val doorsDirectStream =
         env
           .addSource(DoorEventSource()).name("Doors generation input stream")
           .assignAscendingTimestamps(_.timestamp)
   */

    val countPerDoorStream = doorsStream
      .keyBy(_.doorId)
      .timeWindow(WINDOW_SIZE)
      .aggregate(new PerDoorAggregation)


    val doorStatsSink = createDoorStatsSink(props)(DOORS_STATS_TOPIC)

    // per door counts
    val allPerDoorCounts = countPerDoorStream.timeWindowAll(WINDOW_SIZE)

    allPerDoorCounts
      .maxBy("count")
      .map(counts => DoorStats[PerDoorCounts](StatsType.door_most_used, counts))
      .addSink(doorStatsSink)

    allPerDoorCounts
      .minBy("count")
      .map(counts => DoorStats[PerDoorCounts](StatsType.door_less_used, counts))
      .addSink(doorStatsSink)

    allPerDoorCounts
      .maxBy("inCount")
      .map(counts => DoorStats[PerDoorCounts](StatsType.door_max_ins, counts))
      .addSink(doorStatsSink)

    allPerDoorCounts
      .maxBy("outCount")
      .map(counts => DoorStats[PerDoorCounts](StatsType.door_max_outs, counts))
      .addSink(doorStatsSink)

    // per period total counts
    val totalSink = createTotalStatsSink(props)(DOORS_STATS_TOPIC)
    val totalCountPerWindow = allPerDoorCounts
      .reduce(
        (ev1: PerDoorCounts, ev2: PerDoorCounts) => ev1.copy(count = ev1.count + ev2.count),
        (
          window: TimeWindow,
          counts: Iterable[PerDoorCounts],
          out: Collector[TotalCounts]) => {
          out.collect(TotalCounts(Period(window.getStart, window.getEnd), counts.iterator.next().count))
        }
      )

    totalCountPerWindow
      .windowAll(SlidingEventTimeWindows.of(Time.minutes(1), WINDOW_SIZE))
      .minBy("count")
      .map(counts => DoorStats[TotalCounts](StatsType.less_busy_window, counts))
      .addSink(totalSink)

    totalCountPerWindow
      .windowAll(SlidingEventTimeWindows.of(Time.minutes(1), WINDOW_SIZE))
      .maxBy("count")
      .map(counts => DoorStats[TotalCounts](StatsType.busiest_window, counts))
      .addSink(totalSink)

    env.execute("Doors Job")
  }

  val toDoorEvent = (event: ObjectNode) =>
    Try {
      val eventVal = event.get("value")
      DoorEvent(
        eventVal.get("doorId").asInt(),
        DoorEventType.withName(eventVal.get("eventType").asText()),
        eventVal.get("timestamp").asLong()
      )
    }
}
