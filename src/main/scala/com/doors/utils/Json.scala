package com.doors.utils

import com.doors.domain.Domain.{TotalCounts, PerDoorCounts}

/**
  * Created by okorolenko on 2019-01-04.
  */
object Json {
  /**
    * json converters type classes
    */
  trait JsonConverter[A] {
    def json(value: A): String
  }

  object JsonConverterInstances {
    implicit val perDoorCountsConverter: JsonConverter[PerDoorCounts] =
      (value: PerDoorCounts) =>
        s"""
           |{
           | "doorId":"${value.doorId}",
           | "count":${value.count},
           | "inCount":${value.inCount},
           | "outCount":${value.outCount}
           | }
         """.stripMargin

    implicit val countPerPeriodConverter: JsonConverter[TotalCounts] =
      (value: TotalCounts) =>
        s"""
           |{
           | "period":
           | {
           |  "start":${value.period.start},
           |  "end":${value.period.end}
           | },
           | "count":${value.count}
           | }
         """.stripMargin
  }

  // exposing syntax to be used directly on the type
  object JsonSyntax {

    implicit class JsonConverterOps[A](value: A) {
      def json(implicit w: JsonConverter[A]): String =
        w.json(value)
    }

  }

}
