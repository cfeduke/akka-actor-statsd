package com.deploymentzone.actor

import akka.actor._
import com.deploymentzone.actor.validation.StatsDBucketValidator
import java.net.InetSocketAddress
import com.deploymentzone.actor.domain.NamespaceTransformer

/**
 * An actor which sends counters to a StatsD instance via connected UDP.
 * @param address hostname and port (UDP) of the StatsD instance
 * @param namespace optional namespace to prefix all counter messages with
 */
class StatsActor(val address: InetSocketAddress, val namespace: String)
  extends Actor
  with StatsProtocolImplementation {

  require(address != null)
  require(StatsDBucketValidator(namespace),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in namespaces and namespaces may not start or end with a period (".")""")

  val namespaceTx = NamespaceTransformer(namespace)

  lazy val _connection: ActorRef = context.actorOf(UdpConnectedActor.props(address, self), "udp")

  override def connection = _connection

  override def process(msg: Metric[_]) = namespaceTx(msg)

}

object StatsActor {
  private val DEFAULT_STATSD_UDP_PORT = 8125

  def props(address: InetSocketAddress, namespace: String) = Props(new StatsActor(address, namespace))
  def props(address: InetSocketAddress) = Props(new StatsActor(address, ""))
  def props(hostname: String, port: Int, namespace: String = "") =
    Props(new StatsActor(new InetSocketAddress(hostname, port), namespace))
  def props(hostname: String, namespace: String) =
    Props(new StatsActor(new InetSocketAddress(hostname, DEFAULT_STATSD_UDP_PORT), namespace))
  def props(hostname: String): Props = props(hostname, "")
}
