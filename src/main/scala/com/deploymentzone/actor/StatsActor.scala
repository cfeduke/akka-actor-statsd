package com.deploymentzone.actor

import akka.actor._
import com.deploymentzone.actor.util.StatsDBucketValidator
import java.net.InetSocketAddress
import com.deploymentzone.actor.protocol.CounterMessage

class StatsActor(val address: InetSocketAddress, val namespace: String = "")
  extends Actor
  with StatsProtocolImplementation {

  require(StatsDBucketValidator(namespace),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in namespaces and namespaces may not start or end with a period (".")""")

  lazy val _connection: ActorRef = context.actorOf(UdpConnectedActor.props(address, self), "udp")

  override def connection = _connection

  override def process(msg: CounterMessage[_]) = msg.namespace(namespace).toString

}

object StatsActor {
  def props(address: InetSocketAddress, namespace: String) = Props(new StatsActor(address, namespace))
  def props(address: InetSocketAddress) = Props(new StatsActor(address))
}
