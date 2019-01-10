package com.doors.transformations

import com.doors.domain.Domain.{DoorEvent, DoorEventType, PerDoorCounts}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by okorolenko on 2019-01-09.
  */
class PerDoorAggregationTest extends FlatSpec with Matchers {

  val aggregateFunction = new PerDoorAggregation

  "createAccumulator" should "return initial accumulator" in {
    aggregateFunction.createAccumulator() should be(PerDoorCounts(0, 0, 0, 0))
  }

  behavior of "add"
  it should "add a door IN event to  door counts" in {
    val event = DoorEvent(1, DoorEventType.in, 123456789l)
    val counts = PerDoorCounts(1, 2, 1, 1)
    val expectedCounts = PerDoorCounts(1, 3, 2, 1)

    aggregateFunction.add(event, counts) should be(expectedCounts)
  }


  it should "add a door OUT event to  door counts" in {
    val event = DoorEvent(1, DoorEventType.out, 123456789l)
    val counts = PerDoorCounts(1, 2, 1, 1)
    val expectedCounts = PerDoorCounts(1, 3, 1, 2)

    aggregateFunction.add(event, counts) should be(expectedCounts)
  }

  it should "accept a door event with a <> doorId but current counters shouldn't be updated" in {
    val event = DoorEvent(2, DoorEventType.out, 123456789l)
    val counts = PerDoorCounts(1, 2, 1, 1)
    val expectedCounts = counts

    aggregateFunction.add(event, counts) should be(expectedCounts)
  }


  "getResult" should "return current accumulator" in {

    val acc = PerDoorCounts(1, 2, 1, 1)

    aggregateFunction.getResult(acc) should be(acc)
  }

  "merge" should "merge 2 door counts with the same doorId" in {

    val count1 = PerDoorCounts(1, 2, 1, 1)
    val count2 = PerDoorCounts(1, 1, 0, 1)
    val expected = PerDoorCounts(1, 3, 1, 2)

    aggregateFunction.merge(count1, count2) should be(expected)
  }
  it should "return 1st count if door ids are <>" in {
    val count1 = PerDoorCounts(1, 2, 1, 1)
    val count2 = PerDoorCounts(2, 1, 0, 1)
    val expected = count1

    aggregateFunction.merge(count1, count2) should be(expected)
  }
}
