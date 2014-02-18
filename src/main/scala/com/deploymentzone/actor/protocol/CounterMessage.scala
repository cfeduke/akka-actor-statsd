package com.deploymentzone.actor.protocol

import com.deploymentzone.actor.util.StatsDBucketValidator

abstract class CounterMessage[T](private[this] var _bucket: String)(val value: T, val samplingRate: Double = 1.0) {
  val symbol: String

  require(_bucket != null)
  require(StatsDBucketValidator(_bucket),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in buckets and buckets may not start or end with a period (".")""")

  private[actor] def bucket = _bucket

  private[actor] def namespace(namespace: String) = {
    _bucket = namespace match {
      case "" => _bucket
      case null => _bucket
      case _ => s"$namespace.${_bucket}"
    }
    this
  }
  override def toString =
    samplingRate match {
      case 1.0  => s"${_bucket}:$value|$symbol"
      case _    => s"${_bucket}:$value|$symbol|@$samplingRate"
    }
}

class Count(bucket: String)(value: Int, samplingRate: Double = 1.0)
  extends CounterMessage[Int](bucket)(value, samplingRate) {

  override val symbol = "c"
}

object Count {
  def apply(bucket: String)(value: Int, samplingRate: Double = 1.0) = new Count(bucket)(value, samplingRate)
}

class Increment(bucket: String) extends Count(bucket)(1)

object Increment {
  def apply(bucket: String) = new Increment(bucket)
}

class Decrement(bucket: String) extends Count(bucket)(-1)

object Decrement {
  def apply(bucket: String) = new Decrement(bucket)
}

class Gauge(bucket: String)(value: Long, samplingRate: Double = 1.0)
  extends CounterMessage[Long](bucket)(value, samplingRate) {

  override val symbol = "g"
}

object Gauge {
  def apply(bucket: String)(value: Long, samplingRate: Double = 1.0) = new Gauge(bucket)(value, samplingRate)
}

class Timing(bucket: String)(value: Long, samplingRate: Double = 1.0)
  extends CounterMessage(bucket)(value, samplingRate) {

  override val symbol = "ms"
}

object Timing {
  import scala.concurrent.duration.Duration

  def apply(bucket: String)(value: Duration, samplingRate: Double = 1.0) = new Timing(bucket)(value.toMillis, samplingRate)
}
