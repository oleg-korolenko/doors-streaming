package com.doors.sources
import com.doors.domain.Domain.DoorEventType.DoorEventType
import com.doors.domain.Domain.{DoorEvent, DoorEventType}
import com.doors.utils.RandomUtils.choiceSeq
import org.apache.flink.streaming.api.functions.source.SourceFunction
import com.doors.utils.RandomUtils._
import scala.util.Random

/**
  * Created by okorolenko on 2018-12-21.
  */
final case class DoorEventSource() extends SourceFunction[DoorEvent] {
  private var running = true

  private val maxDelayMsecs = 0
  private val watermarkDelayMSecs = 0
  private val servingSpeed = 0
  override def run(ctx: SourceFunction.SourceContext[DoorEvent]): Unit = {
    val chooseEventType = choiceSeq[DoorEventType](Seq[DoorEventType](DoorEventType.in, DoorEventType.out))

    var currentEventTs = System.currentTimeMillis()
    val someId  = someIntIn(2)
    while (running){
      val rand = new Random
      Thread.sleep(1000)
      currentEventTs=someLongWithin((currentEventTs,currentEventTs+1000))
      val event = DoorEvent(someId(), chooseEventType(rand),currentEventTs)
      ctx.collectWithTimestamp(event, event.timestamp)
    }

  }

  override def cancel(): Unit = this.running = false
}
