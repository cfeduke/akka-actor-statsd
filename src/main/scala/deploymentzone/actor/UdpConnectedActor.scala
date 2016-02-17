package deploymentzone.actor

import java.net.InetSocketAddress
import akka.actor._
import akka.io._, UdpConnected._
import akka.util.ByteString


/** Communication is restricted to one specific remote socket address.
  * Thus it's named 'connected' even though UDP is connectionless protocol.
  *
  * It has benefit of cached security checks (if SecurityManager is enabled)
  * instead of verification on every message sent,
  * when datagrams can be sent/received to/from any destination.
  *
  * Based on example from http://doc.akka.io/docs/akka/2.4.1/scala/io-udp.html
  */
private[actor] class UdpConnectedActor(
  remoteAddress: InetSocketAddress
) extends Actor
  with ActorLogging {

  import context.system

  def receive = {
    case Connect =>
      IO(UdpConnected) ! Connect(self, remoteAddress)
      context.become(starting(sender()))
  }

  def starting(initiator: ActorRef): Receive = {
    case Connected =>
      context.become(ready(sender()))
      initiator ! Connected
  }

  def ready(connection: ActorRef): Receive = {
    case msg: String => connection ! Send(ByteString(msg))
    case Disconnect => connection ! Disconnect
    case Disconnected => context.stop(self)
    case CommandFailed(cmd) =>
      val msg = cmd match {
        case Send(payload, _) =>
          s"Unable to deliver payload: ${payload.utf8String} to $remoteAddress"
        case _  =>
          s"CommandFailed: $cmd (${cmd.getClass})"
      }
      log warning msg
  }

  override def unhandled(message: Any) = {
    log.warning(s"Unhandled message: $message (${message.getClass})")
  }

}

private[actor] object UdpConnectedActor {
  def props(remoteAddress: InetSocketAddress) =
    Props(new UdpConnectedActor(remoteAddress))
}
