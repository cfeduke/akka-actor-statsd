package akka.statsd.transport

import java.net.InetSocketAddress
import akka.actor._
import akka.io.{IO, Udp}
import akka.util.ByteString

private[statsd] class UdpUnconnected(remoteAddress: InetSocketAddress) extends Actor
  with Stash
  with ActorLogging {

  import context.system

  override def preStart(): Unit =
    IO(Udp) ! Udp.SimpleSender

  def receive: Receive = waitingForConnection

  def waitingForConnection: Receive = {
    case Udp.SimpleSenderReady ⇒
      unstashAll()
      context become operational(sender())
    case _: String =>
      stash()

  }

  def operational(connection: ActorRef): Receive = {
    case msg: String =>
      connection ! Udp.Send(ByteString(msg), remoteAddress)
  }

  override def unhandled(message: Any): Unit = {
    log.warning(s"Unhandled message: $message (${message.getClass})")
  }
}

private[statsd] object UdpUnconnected {
  def props(remoteAddress: InetSocketAddress): Props = Props(new UdpUnconnected(remoteAddress))
}
