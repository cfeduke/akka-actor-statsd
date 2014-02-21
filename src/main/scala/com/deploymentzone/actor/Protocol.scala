package com.deploymentzone.actor

import com.deploymentzone.actor.validation.StatsDBucketValidator

abstract class Metric[+T](val bucket: String, val samplingRate: Double)(val value: T) {
  val symbol: String
  protected[this] val renderValue: String = value.toString

  require(bucket != null)
  require(StatsDBucketValidator(bucket),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in buckets and buckets may not start or end with a period (".")""")

  override def toString =
    samplingRate match {
      case 1.0  => s"$bucket:$renderValue|$symbol"
      case _    => s"$bucket:$renderValue|$symbol|@$samplingRate"
    }
}

class Count(bucket: String, samplingRate: Double = 1.0)(value: Int)
  extends Metric[Int](bucket, samplingRate)(value) {

  override val symbol = Count.SYMBOL
}

object Count {
  val SYMBOL = "c"

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
  extends Metric[Long](bucket, samplingRate)(value) {

  override val symbol = Gauge.SYMBOL
}

object Gauge {
  val SYMBOL = "g"
  def apply(bucket: String, samplingRate: Double = 1.0)(value: Long) =
    new Gauge(bucket, samplingRate)(value)
}

class GaugeAdd(bucket: String, samplingRate: Double)(value: Long)
  extends Gauge(bucket, samplingRate)(value) {

  override val renderValue = s"+${Math.abs(value)}"
}

object GaugeAdd {
  def apply(bucket: String, samplingRate: Double = 1.0)(value: Long) =
    new GaugeAdd(bucket, samplingRate)(value)
}

class GaugeSubtract(bucket: String, samplingRate: Double)(value: Long)
  extends Gauge(bucket, samplingRate)(value) {

  override val renderValue = s"-${Math.abs(value)}"
}

object GaugeSubtract {
  def apply(bucket: String, samplingRate: Double = 1.0)(value: Long) =
    new GaugeSubtract(bucket, samplingRate)(value)
}

class Timing(bucket: String, samplingRate: Double = 1.0)(value: Long)
  extends Metric(bucket, samplingRate)(value) {

  override val symbol = Timing.SYMBOL
}

class Set(bucket: String, samplingRate: Double = 1.0)(value: Long)
  extends Metric(bucket, samplingRate)(value) {

  override val symbol = Set.SYMBOL
}

object Set {
  val SYMBOL = "s"

  def apply(bucket: String, samplingRate: Double = 1.0)(value: Long) =
    new Set(bucket, samplingRate)(value)
}

object Timing {
  import scala.concurrent.duration.Duration

  val SYMBOL = "ms"

  def apply(bucket: String, samplingRate: Double = 1.0)(value: Duration) =
    new Timing(bucket, samplingRate)(value.toMillis)
}
