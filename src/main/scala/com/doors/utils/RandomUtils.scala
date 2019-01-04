package com.doors.utils

import java.util.concurrent.ThreadLocalRandom

import scala.util.Random

/**
  * Created by okorolenko on 2018-12-21.
  */
object RandomUtils {
  // parameterized generator: outputs random data based on parameter known at construction time (e.g. probability p in case of a coin flip)
  type ParameterizedGen[Param, Out] = Param ⇒ Random ⇒ Out

  /**
    * generator of a random Long within the specified range
    **/
  val someLongWithin: ((Long, Long)) => Long = {
    case (min: Long, max: Long) ⇒ ThreadLocalRandom.current().nextLong(min, max)
  }

  /**
    * generator of a random Int within the specified range
    **/
  val someIntIn: Int => ()=> Int = (max:Int) => {
    ()=> Random.nextInt(max)+1
  }

  /**
    * choice between seq of values of T
    */
  def choiceSeq[T]: ParameterizedGen[Seq[T], T] = (among: Seq[T]) ⇒ {
    random: Random ⇒ among(random.nextInt(among.size))
  }
}
