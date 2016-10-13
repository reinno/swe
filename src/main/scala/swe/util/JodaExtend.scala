package swe.util

import org.joda.time.DateTime

object JodaExtend {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}
