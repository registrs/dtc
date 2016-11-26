package dtc.laws


import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDate, LocalTime}

import cats.kernel.laws._
import dtc._
import dtc.syntax.zonedDateTime._
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen, Prop}

/**
  * Laws, that must be obeyed by any ZonedDateTimeTC instance
  */
trait ZonedDateTimeLaws[A] {
  implicit def D: ZonedDateTimeTC[A]

  val genA: Gen[A]
  val genDateAndDurationWithinSameOffset: Gen[(A, Duration)]
  val genDataSuite: Gen[ZonedDateTimeTestData[A]]
  val genLocalDate: Gen[LocalDate]
  val genLocalTime: Gen[LocalTime]
  val genValidYear: Gen[Int]
  val genTimeZone: Gen[TimeZoneId]

  def twoConsequentNowCalls: Prop = forAll(genTimeZone) { zone: TimeZoneId =>
    val prev = D.now(zone)
    val current = D.now(zone)
    prev ?<= current
  }

  def constructorConsistency: Prop = forAll(genLocalDate, genLocalTime, genTimeZone) {
    (date: LocalDate, time: LocalTime, zone: TimeZoneId) =>
      val dt = D.of(date, time, zone)
      (dt.date ?== date) &&
        (dt.time ?== time.truncatedTo(ChronoUnit.MILLIS)) &&
        (dt.zone ?== zone)
  }

  def crossOffsetAddition: Prop = forAll(genDataSuite) { data =>
    val target = D.plus(data.source, data.diff)
    (D.offset(target) ?== data.targetOffset) &&
      (D.date(target) ?== data.targetDate) &&
      (D.time(target) ?== data.targetTime.truncatedTo(ChronoUnit.MILLIS))
  }
}

object ZonedDateTimeLaws {
  def apply[A](
    gDateAndDurationWithinSameDST: Gen[(A, Duration)],
    gDataSuite: Gen[ZonedDateTimeTestData[A]],
    gLocalTime: Gen[LocalTime],
    gLocalDate: Gen[LocalDate],
    gValidYear: Gen[Int],
    gTimeZone: Gen[TimeZoneId])(
    implicit ev: ZonedDateTimeTC[A],
    arbA: Arbitrary[A]): ZonedDateTimeLaws[A] = new ZonedDateTimeLaws[A] {

    def D: ZonedDateTimeTC[A] = ev

    val genTimeZone: Gen[TimeZoneId] = gTimeZone
    val genDateAndDurationWithinSameOffset: Gen[(A, Duration)] = gDateAndDurationWithinSameDST
    val genDataSuite: Gen[ZonedDateTimeTestData[A]] = gDataSuite
    val genLocalDate: Gen[LocalDate] = gLocalDate
    val genLocalTime: Gen[LocalTime] = gLocalTime
    val genValidYear: Gen[Int] = gValidYear
    val genA: Gen[A] = arbA.arbitrary
  }
}
