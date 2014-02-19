package com.deploymentzone.actor

import com.deploymentzone.actor.validation.StatsDBucketValidator

abstract class CounterMessage[+T](val bucket: String, val samplingRate: Double)(val value: T) {
  val symbol: String

  require(bucket != null)
  require(StatsDBucketValidator(bucket),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in buckets and buckets may not start or end with a period (".")""")

  override def toString =
    samplingRate match {
      case 1.0  => s"$bucket:$value|$symbol"
      case _    => s"$bucket:$value|$symbol|@$samplingRate"
    }
}

class Count(bucket: String, samplingRate: Double = 1.0)(value: Int)
  extends CounterMessage[Int](bucket, samplingRate)(value) {

  override val symbol = "c"
}

object Count {
  def apply(bucket: String, samplingRate: Double = 1.0)(value: Int) =
    new Count(bucket, samplingRate)(value)
}

class Increment(bucket: String) extends Count(bucket)(1)

object Increment {
  def apply(bucket: String) = new Increment(bucket)
}

class Decrement(bucket: String) extends Count(bucket)(-1)

object Decrement {
  def apply(bucket: String) = new Decrement(bucket)
}

class Gauge(bucket: String, samplingRate: Double)(value: Long)
  extends CounterMessage[Long](bucket, samplingRate = 1.0)(value) {

  override val symbol = "g"
}

object Gauge {
  def apply(bucket: String, samplingRate: Double = 1.0)(value: Long) =
    new Gauge(bucket, samplingRate)(value)
}

class Timing(bucket: String, samplingRate: Double = 1.0)(value: Long)
  extends CounterMessage(bucket, samplingRate = 1.0)(value) {

  override val symbol = "ms"
}

object Timing {
  import scala.concurrent.duration.Duration

  def apply(bucket: String, samplingRate: Double = 1.0)(value: Duration) =
    new Timing(bucket, samplingRate)(value.toMillis)
}
