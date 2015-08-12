package deploymentzone.actor

import akka.actor._
import deploymentzone.actor.validation.StatsDBucketValidator
import java.net.InetSocketAddress
import deploymentzone.actor.domain.NamespaceTransformer

import scala.concurrent.duration.Duration

/**
 * An actor which sends counters to a StatsD instance via connected UDP.
 * @param address hostname and port (UDP) of the StatsD instance
 * @param namespace optional namespace to prefix all counter messages with
 * @param _config optional configuration settings; when not specified a default configuration is created based on what
 *                ConfigFactory loads
 */
class StatsActor(val address: InetSocketAddress, val namespace: String, private val _config: Option[Config] = None)
  extends Actor
  with StatsProtocolImplementation {

  protected[this] val config = _config.getOrElse(Config(address))

  require(address != null)
  require(StatsDBucketValidator(namespace),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in namespaces and namespaces may not start or end with a period (".")""")

  val namespaceTx = NamespaceTransformer(namespace)

  lazy val _connection: ActorRef = context.actorOf(UdpConnectedActor.props(config, self), "udp")

  override def connection = _connection

  override def process(msg: Metric[_]) = namespaceTx(msg)

}

object StatsActor {
  private val defaultConfig = Defaults.config

  def props(address: InetSocketAddress, namespace: String) = Props(new StatsActor(address, namespace))
  def props(address: InetSocketAddress) = Props(new StatsActor(address, defaultConfig.namespace))
  def props(hostname: String, port: Int, namespace: String = defaultConfig.namespace) =
    Props(new StatsActor(new InetSocketAddress(hostname, port), namespace))
  def props(hostname: String, namespace: String) =
    Props(new StatsActor(new InetSocketAddress(hostname, defaultConfig.port), namespace))
  def props(hostname: String): Props = props(hostname, defaultConfig.namespace)
  def props(config: com.typesafe.config.Config): Props = {
    val c = Config(config)
    Props(new StatsActor(c.address, c.namespace, Some(c)))
  }
  def props(): Props = {
    Props(new StatsActor(defaultConfig.address, defaultConfig.namespace, Some(defaultConfig)))
  }

}

object Stats {
  /**
   * Increments bucket by 1
   * @param bucket the bucket to increment by 1
   * @param sampleRate sampleRate defaults to 1.0
   * @param statsActor Implicitly scoped [[StatsActor]]
   */
  def increment(bucket:String, sampleRate:Double = 1.0)(implicit statsActor: ActorRef): Unit = {
    statsActor ! Count(bucket, sampleRate)(1)
  }

  /**
   * Decrements bucket by 1
   * @param bucket the bucket to decrement by 1
   * @param sampleRate sampleRate defaults to 1.0
   * @param statsActor Implicitly scoped [[StatsActor]]
   */
  def decrement(bucket:String, sampleRate:Double = 1.0)(implicit statsActor: ActorRef): Unit = {
    statsActor ! Count(bucket, sampleRate)(-1)
  }

  /**
   * adds count to bucket (subtracts if negative)
   * @param bucket the bucket to count
   * @param sampleRate sampleRate defaults to 1.0
   * @param count the number by which to increase/decrease the bucket
   * @param statsActor Implicitly scoped [[StatsActor]]
   * @return
   */
  def count(bucket:String, sampleRate:Double = 1.0)(count:Int)(implicit statsActor: ActorRef): Unit = {
    statsActor ! Count(bucket, sampleRate)(count)
  }

  /**
   * Sets the bucket to the specified value
   * @param bucket the bucket to set
   * @param sampleRate sampleRate defaults to 1.0
   * @param value the value to set the bucket too
   * @param statsActor Implicitly scoped [[StatsActor]]
   */
  def gauge(bucket:String, sampleRate:Double = 1.0)(value:Long)(implicit statsActor: ActorRef): Unit = {
    statsActor ! gauge(bucket, sampleRate)(value)
  }

  /**
   * relatively sets the bucket
   * @param bucket the bucket to set
   * @param sampleRate sampleRate defaults to 1.0
   * @param value the value to increase/decrease the bucket by
   * @param statsActor Implicitly scoped [[StatsActor]]
   */
  def gaugeDelta(bucket:String, sampleRate:Double = 1.0)(value:Long)(implicit statsActor: ActorRef): Unit = {
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
   * @param statsActor Implicitly scoped [[StatsActor]]
   */
  def set(bucket:String, sampleRate:Double = 1.0)(value:Long)(implicit statsActor:ActorRef): Unit = {
    statsActor ! Set(bucket, sampleRate)(value)
  }

  /**
   * Sets the bucket to the specified time in ms
   * @param bucket the bucket to set the time in
   * @param sampleRate sampleRate defaults to 1.0
   * @param value the duration, will be converted to milliseconds
   * @param statsActor Implicitly scoped [[StatsActor]]
   */
  def time(bucket:String, sampleRate:Double = 1.0)(value:Duration)(implicit statsActor: ActorRef): Unit = {
    statsActor ! Timing(bucket, sampleRate)(value)
  }

  /**
   * Executes the code block and times the execution time
   * @param bucket the bucket to set the time in ms
   * @param sampleRate sampleRate defaults to 1.0
   * @param timed code block to time
   * @param statsActor Implicitly scoped [[StatsActor]]
   * @tparam T return type of executed code block
   */
  def withTimer[T](bucket:String, sampleRate:Double = 1.0)(timed : => T)(implicit statsActor: ActorRef): T = {
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
   * @param statsActor Implicitly scoped [[StatsActor]]
   * @tparam T return type of executed code block
   */
  def withCountAndTimer[T](bucket:String, sampleRate:Double = 1.0)(timed : => T)(implicit statsActor: ActorRef): T = {
    import scala.concurrent.duration._

    val start = System.currentTimeMillis()
    val result = timed
    val time = System.currentTimeMillis()-start
    statsActor ! Increment(bucket)
    statsActor ! Timing(bucket, sampleRate)(time.milliseconds)
    result
  }


}

