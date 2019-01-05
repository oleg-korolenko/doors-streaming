package com.doors.sources

import com.doors.domain.Domain._
import com.doors.generators.DoorGenerators
import com.doors.generators.DoorGenerators.{DoorGenerator, GeneratorCfg}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import com.doors.utils.RandomUtils._


/**
  * Created by okorolenko on 2018-12-21.
  */
final case class DoorEventSource() extends SourceFunction[DoorEvent] {
  private var running = true

  override def run(ctx: SourceFunction.SourceContext[DoorEvent]): Unit = {
    val generateEvent = DoorGenerators.defaultGenerator(GeneratorCfg())
    val someId  = someIntIn(DOORS_NUMBER)
    while (running){
      Thread.sleep(1000)
      val event = generateEvent(someId())
      ctx.collectWithTimestamp(event, event.timestamp)
    }
  }

  override def cancel(): Unit = this.running = false
}
