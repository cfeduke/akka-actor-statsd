package akka.statsd

import scala.concurrent.duration.Duration
import akka.actor._
import transport._


/**
 * Collects and sends various metrics to a StatsD instance.
 * See [[akka.statsd.Protocol]] for supported Metrics
 */
class Stats(
  val config: Config,
  connectionProps: Props
) extends Actor
  with ActorLogging {

  val connection = context.actorOf(connectionProps, "statsd-connection")

  val namespaced: Metric[_] => Metric[_] =
    if (config.namespace.isEmpty) identity
    else m => m.prepend(config.namespace)

  def receive = {
    case msg: Metric[_] => connection ! namespaced(msg).toString
  }
}


object Stats {
  def bufferedConnection(cfg: Config): Props =
    ScheduledDispatcher.props(cfg, Connection.props(cfg.address))

  def props(cfg: Config = Config(), conn: Config => Props = bufferedConnection): Props =
    Props(new Stats(cfg, conn(cfg)))

  /**
   * Increments bucket by 1
   * @param bucket the bucket to increment by 1
   * @param sampleRate sampleRate defaults to 1.0
   * @param statsActor Implicitly scoped [[Stats]]
   */
  def increment(bucket: Bucket, sampleRate: Double = 1.0)(implicit statsActor: ActorRef): Unit = {
    statsActor ! Count(bucket, sampleRate)(1)
  }

  /**
   * Decrements bucket by 1
   * @param bucket the bucket to decrement by 1
   * @param sampleRate sampleRate defaults to 1.0
   * @param statsActor Implicitly scoped [[Stats]]
   */
  def decrement(bucket: Bucket, sampleRate: Double = 1.0)(implicit statsActor: ActorRef): Unit = {
    statsActor ! Count(bucket, sampleRate)(-1)
  }

  /**
   * adds count to bucket (subtracts if negative)
   * @param bucket the bucket to count
   * @param sampleRate sampleRate defaults to 1.0
   * @param count the number by which to increase/decrease the bucket
   * @param statsActor Implicitly scoped [[Stats]]
   * @return
   */
  def count(bucket: Bucket, sampleRate: Double = 1.0)(count:Int)(implicit statsActor: ActorRef): Unit = {
    statsActor ! Count(bucket, sampleRate)(count)
  }

  /**
   * Sets the bucket to the specified value
   * @param bucket the bucket to set
   * @param sampleRate sampleRate defaults to 1.0
   * @param value the value to set the bucket too
   * @param statsActor Implicitly scoped [[Stats]]
   */
  def gauge(bucket: Bucket, sampleRate: Double = 1.0)(value:Long)(implicit statsActor: ActorRef): Unit = {
    statsActor ! gauge(bucket, sampleRate)(value)
  }

  /**
   * relatively sets the bucket
   * @param bucket the bucket to set
   * @param sampleRate sampleRate defaults to 1.0
   * @param value the value to increase/decrease the bucket by
   * @param statsActor Implicitly scoped [[Stats]]
   */
  def gaugeDelta(bucket: Bucket, sampleRate: Double = 1.0)(value:Long)(implicit statsActor: ActorRef): Unit = {
    value match {
      case v if v < 0 => statsActor ! GaugeSubtract(bucket, sampleRate)(-v)
      case v if v >= 0 => statsActor ! GaugeAdd(bucket, sampleRate)(v)
    }
  }

  /**
   * Sets the bucket to the value
   * @param bucket the bucket to set
   * @param sampleRate sampleRate defaults to 1.0
   * @param value the value to set the bucket to
   * @param statsActor Implicitly scoped [[Stats]]
   */
  def set(bucket: Bucket, sampleRate: Double = 1.0)(value:Long)(implicit statsActor:ActorRef): Unit = {
    statsActor ! Set(bucket, sampleRate)(value)
  }

  /**
   * Sets the bucket to the specified time in ms
   * @param bucket the bucket to set the time in
   * @param sampleRate sampleRate defaults to 1.0
   * @param value the duration, will be converted to milliseconds
   * @param statsActor Implicitly scoped [[Stats]]
   */
  def time(bucket: Bucket, sampleRate: Double = 1.0)(value:Duration)(implicit statsActor: ActorRef): Unit = {
    statsActor ! Timing(bucket, sampleRate)(value)
  }

  /**
   * Executes the code block and times the execution time
   * @param bucket the bucket to set the time in ms
   * @param sampleRate sampleRate defaults to 1.0
   * @param timed code block to time
   * @param statsActor Implicitly scoped [[Stats]]
   * @tparam T return type of executed code block
   */
  def withTimer[T](bucket: Bucket, sampleRate: Double = 1.0)(timed : => T)(implicit statsActor: ActorRef): T = {
    import scala.concurrent.duration._

    val start = System.currentTimeMillis()
    val result = timed
    val time = System.currentTimeMillis()-start
    statsActor ! Timing(bucket, sampleRate)(time.milliseconds)
    result
  }

  /**
   * Executes the code block and times the execution time
   * @param bucket the bucket to set the time in ms
   * @param sampleRate sampleRate defaults to 1.0
   * @param timed code block to time
   * @param statsActor Implicitly scoped [[Stats]]
   * @tparam T return type of executed code block
   */
  def withCountAndTimer[T](bucket: Bucket, sampleRate: Double = 1.0)(timed : => T)(implicit statsActor: ActorRef): T = {
    import scala.concurrent.duration._

    val start = System.currentTimeMillis()
    val result = timed
    val time = System.currentTimeMillis()-start
    statsActor ! Increment(bucket)
    statsActor ! Timing(bucket, sampleRate)(time.milliseconds)
    result
  }


}

