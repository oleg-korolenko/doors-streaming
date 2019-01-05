package com.doors.generators

import com.doors.domain.Domain.DoorEventType.DoorEventType
import com.doors.domain.Domain.{DoorEventType, _}
import com.doors.utils.RandomUtils._

import scala.util.Random

/**
  * Created by okorolenko on 2018-12-21.
  */

final case class EventsPerDoor(events: Array[DoorEvent])


object DoorGenerators {

  final case class GeneratorCfg(maxDelayMSecs: Long = 100)

  type DoorId = Int

  type DoorGenerator = GeneratorCfg => DoorId => DoorEvent

  val chooseEventType = choiceSeq[DoorEventType](Seq[DoorEventType](DoorEventType.in, DoorEventType.out))


  /**
    * Default Generator with
    * random delay within maxDelayMSecs parameter
    * Event type 50% IN/OUT
    */
  val   defaultGenerator: DoorGenerator = (cfg: GeneratorCfg) => (id: DoorId) => {
    val rand = new Random
    val now = System.currentTimeMillis()
    val eventTimeStamp = someLongWithin((now, now + cfg.maxDelayMSecs))
    DoorEvent(id, chooseEventType(rand),eventTimeStamp)
  }

}
