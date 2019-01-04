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
  val DOORS_COUNT = 200


  val allDoors = () => {
    val now = System.currentTimeMillis()

    val chooseEventType = choiceSeq[DoorEventType](Seq[DoorEventType](DoorEventType.in, DoorEventType.out))
    val rand = new Random

    var currentEventTs = now
    (1 to 1).flatMap {
      id => {
        // within 1 sec range
        currentEventTs = someLongWithin((currentEventTs, currentEventTs + 10000))
        val event = DoorEvent(id, chooseEventType(rand), currentEventTs)

        // within 1 sec range
        currentEventTs = someLongWithin((currentEventTs, currentEventTs + 10000))
        val event2 = DoorEvent(id, chooseEventType(rand), currentEventTs)

        List(event, event2)
      }
    }
  }

  val chooseEventType = choiceSeq[DoorEventType](Seq[DoorEventType](DoorEventType.in, DoorEventType.out))

/*
  case class GeneratorPattern(betweenEvents)
  val randomDoors = () => {

    val rand = new Random

    ()=> {
      var currentEventTs = System.currentTimeMillis()
      val someDoorId  = someIntIn(200)

      // within 1 sec range
      currentEventTs = someLongWithin((currentEventTs, currentEventTs + 10000))
      val event = DoorEvent(id, chooseEventType(rand), currentEventTs)

      // within 1 sec range
      currentEventTs = someLongWithin((currentEventTs, currentEventTs + 10000))
      val event2 = DoorEvent(id, chooseEventType(rand), currentEventTs)

    }*/



}
