package deploymentzone.actor
package udp

import java.net.InetSocketAddress
import akka.actor._
import akka.io._, UdpConnected._
import akka.util.ByteString


private[actor] object SingleAddressConnection {
  def props(remoteAddress: InetSocketAddress) =
    Props(new SingleAddressConnection(remoteAddress))
}

/** Communication is restricted to one specific remote socket address.
  * Thus it's using UdpConnected, even though UDP is connectionless protocol.
  *
  * If SecurityManager is enabled it has benefit of cached security checks
  * instead of verification on every message sent,
  * when datagrams can be sent/received to/from any destination.
  *
  * Based on example from http://doc.akka.io/docs/akka/2.4.2/scala/io-udp.html
  */
private[actor] class SingleAddressConnection(
  remoteAddress: InetSocketAddress
) extends Actor
  with ActorLogging {

  import context.system
  import Connection._

  def receive = initial

  def initial: Receive = {
    case Open =>
      IO(UdpConnected) ! Connect(self, remoteAddress)
      context become opening(sender())
  }

  def opening(initiator: ActorRef): Receive = {
    case UdpConnected.Connected =>
      initiator ! Opened
      context become working(initiator, sender())
    case Deliver(msg) =>
      self ! msg
  }

  def working(initiator: ActorRef, socket: ActorRef): Receive = {
    case Deliver(msg) =>
      println(s"UDP deliver $msg")
      socket ! Send(ByteString(msg))
    case CommandFailed(Send(payload, _)) =>
      val msg = payload.utf8String
      log warning s"Unable to deliver message: $msg to $remoteAddress"
      DeliveryFailed(msg)
  }
}
