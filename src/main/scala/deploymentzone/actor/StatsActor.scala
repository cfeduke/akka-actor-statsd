package deploymentzone.actor

import akka.actor._
import deploymentzone.actor.validation.StatsDBucketValidator
import java.net.InetSocketAddress
import deploymentzone.actor.domain.NamespaceTransformer
import com.typesafe.config.ConfigFactory

/**
 * An actor which sends counters to a StatsD instance via connected UDP.
 * @param address hostname and port (UDP) of the StatsD instance
 * @param namespace optional namespace to prefix all counter messages with
 * @param _config optional configuration settings; when not specified a default configuration is created based on what
 *                [[ConfigFactory]] loads
 */
class StatsActor(val address: InetSocketAddress, val namespace: String, private val _config: Option[Config] = None)
  extends Actor
  with StatsProtocolImplementation {

  private val config = _config.getOrElse(Defaults.config)

  require(address != null)
  require(StatsDBucketValidator(namespace),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in namespaces and namespaces may not start or end with a period (".")""")

  val namespaceTx = NamespaceTransformer(namespace)

  lazy val _connection: ActorRef = context.actorOf(UdpConnectedActor.props(address, self), "udp")

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
