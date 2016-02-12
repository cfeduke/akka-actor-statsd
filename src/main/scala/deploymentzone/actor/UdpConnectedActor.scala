package deploymentzone.actor

import akka.actor._
import akka.io._, UdpConnected._
import akka.util.ByteString

/* originated from: http://doc.akka.io/docs/akka/snapshot/scala/io-udp.html */

/* by using a connected form instead of a simple sender, security checks are cached instead of verified on every send */
private[actor] class UdpConnectedActor(val config: Config, val requester: ActorRef)
  extends Actor
  with ActorLogging {

  import context.system
  val remote = config.address

  def receive = {
    case Connect =>
      IO(UdpConnected) ! Connect(self, remote)
    case Connected =>
      context.become(ready(sender))
      requester ! Connected
  }

  def ready(connection: ActorRef): Receive = {
    case msg: String =>
      connection ! Send(ByteString(msg))
    case Disconnect => connection ! Disconnect
    case Disconnected   => context.stop(self)
    case f : CommandFailed =>
      f.cmd match {
        case send: Send => log warning s"Unable to deliver payload: ${send.payload.decodeString("utf-8")}"
        case _                       => log warning s"CommandFailed: ${f.cmd} (${f.cmd.getClass})"
      }
  }

  override def unhandled(message: Any) = {
    log.warning(s"Unhandled message: $message (${message.getClass})")
  }

}

private[actor] object UdpConnectedActor {
  def props(config: Config, requester: ActorRef) = Props(new UdpConnectedActor(config, requester))
}
