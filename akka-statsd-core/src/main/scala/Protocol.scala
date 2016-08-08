package akka.statsd


sealed class Metric[+T] private[statsd](
  val bucket: Bucket,
  val value: T,
  val symbol: String,
  val samplingRate: Double
) {
  def prepend(namespace: String): Metric[T] =
    new Metric[T](bucket prepend namespace, value, symbol, samplingRate)

  protected[this] def renderValue = value.toString

  override def toString =
    samplingRate match {
      case 1.0  => s"${bucket.render}:$renderValue|$symbol"
      case _    => s"${bucket.render}:$renderValue|$symbol|@$samplingRate"
    }
}


class Count(bucket: Bucket, samplingRate: Double = 1.0)(value: Int)
  extends Metric(bucket, value, Count.SYMBOL, samplingRate)

object Count {
  val SYMBOL = "c"

  def apply(bucket: Bucket, samplingRate: Double = 1.0)(value: Int) =
    new Count(bucket, samplingRate)(value)
}


class Increment(bucket: Bucket) extends Count(bucket)(1)

object Increment {
  def apply(bucket: Bucket) = new Increment(bucket)
}


class Decrement(bucket: Bucket) extends Count(bucket)(-1)

object Decrement {
  def apply(bucket: Bucket) = new Decrement(bucket)
}


class Gauge(bucket: Bucket, samplingRate: Double)(value: Long)
  extends Metric[Long](bucket, value, Gauge.SYMBOL, samplingRate)

object Gauge {
  val SYMBOL = "g"
  def apply(bucket: Bucket, samplingRate: Double = 1.0)(value: Long) =
    new Gauge(bucket, samplingRate)(value)
}


class GaugeAdd(bucket: Bucket, samplingRate: Double)(value: Long)
  extends Gauge(bucket, samplingRate)(value) {

  override val renderValue = s"+${Math.abs(value)}"
}

object GaugeAdd {
  def apply(bucket: Bucket, samplingRate: Double = 1.0)(value: Long) =
    new GaugeAdd(bucket, samplingRate)(value)
}


class GaugeSubtract(bucket: Bucket, samplingRate: Double)(value: Long)
  extends Gauge(bucket, samplingRate)(value) {

  override val renderValue = s"-${Math.abs(value)}"
}

object GaugeSubtract {
  def apply(bucket: Bucket, samplingRate: Double = 1.0)(value: Long) =
    new GaugeSubtract(bucket, samplingRate)(value)
}


class Timing(bucket: Bucket, samplingRate: Double = 1.0)(value: Long)
  extends Metric(bucket, value, Timing.SYMBOL, samplingRate)

object Timing {
  import scala.concurrent.duration.Duration

  val SYMBOL = "ms"

  def apply(bucket: Bucket, samplingRate: Double = 1.0)(value: Duration): Timing =
    new Timing(bucket, samplingRate)(value.toMillis)
}


class StatSet(bucket: Bucket, samplingRate: Double = 1.0)(value: Long)
  extends Metric(bucket, value, StatSet.SYMBOL, samplingRate)

object StatSet {
  val SYMBOL = "s"

  def apply(bucket: Bucket, samplingRate: Double = 1.0)(value: Long) =
    new StatSet(bucket, samplingRate)(value)
}

