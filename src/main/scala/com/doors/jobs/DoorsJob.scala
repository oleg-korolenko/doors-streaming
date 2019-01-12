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
import com.doors.utils.Converters._
import com.doors.sources.DoorEventSource
import com.doors.transformations.PerDoorAggregation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._

/**
  * Parsed command-line config
  **/
case class DoorsJobConfig(
                           kafkaSever: String,
                           kafkaGroupId: String,
                           kafkaTopicDoorsIn: String,
                           kafkaTopicDoorsStats: String
                         )

object DoorsJobConfig {

  def parse(args: Array[String]): DoorsJobConfig = {
    val parameters = ParameterTool.fromArgs(args)

    DoorsJobConfig(
      parameters.get("kafkaServer", "kafka:9092"),
      parameters.get("kafkaGroupId", "flink"),
      parameters.get("kafkaTopicDoorsIn", "doors"),
      parameters.get("kafkaTopicDoorsStats", "doors-stats")
    )
  }
}


object DoorsJob {
  private val WINDOW_SIZE =Time.seconds(10)
  private val TOTAL_STATS_WINDOW_SLIDE = Time.seconds(10)
  private val TOTAL_STATS_WINDOW_SIZE = Time.minutes(1)

  def main(args: Array[String]) {
    val config = DoorsJobConfig.parse(args)

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


    val props = new Properties()
    props.setProperty("bootstrap.servers", config.kafkaSever)
    props.setProperty("group.id", config.kafkaGroupId)

    // generation stream to kafka input
    val doorsGenerationStream =
      env
        .addSource(
          DoorEventSource())
        .name("Doors generation input stream")
        .addSink(createDoorsSink(props)(config.kafkaTopicDoorsIn)).name("Doors generation sink")

    // to not generate out-of-order
    doorsGenerationStream.setParallelism(1)

    //main  doors processing stream
    val source = new FlinkKafkaConsumer[ObjectNode](
      config.kafkaTopicDoorsIn,
      new JSONKeyValueDeserializationSchema(true),
      props
    )

    val doorsStream =
      env.addSource(source).name("Doors input stream")
        .map(toDoorEvent)
        .filter(_.isSuccess)
        .map(_.get)
        .assignAscendingTimestamps(_.timestamp)

    val countPerDoorStream = doorsStream
      .keyBy(_.doorId)
      .timeWindow(WINDOW_SIZE)
      .aggregate(new PerDoorAggregation)


    val doorStatsSink = createDoorStatsSink(props)(config.kafkaTopicDoorsStats)

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
    val totalSink = createTotalStatsSink(props)(config.kafkaTopicDoorsStats)
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
      .windowAll(SlidingEventTimeWindows.of(TOTAL_STATS_WINDOW_SIZE, TOTAL_STATS_WINDOW_SLIDE))
      .minBy("count")
      .map(counts => DoorStats[TotalCounts](StatsType.less_busy_window, counts))
      .addSink(totalSink)

    totalCountPerWindow
      .windowAll(SlidingEventTimeWindows.of(TOTAL_STATS_WINDOW_SIZE, TOTAL_STATS_WINDOW_SLIDE))
      .maxBy("count")
      .map(counts => DoorStats[TotalCounts](StatsType.busiest_window, counts))
      .addSink(totalSink)

    env.execute("Doors Job")
  }
}
