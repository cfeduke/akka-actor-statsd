package akka.statsd.transport

import java.net.InetSocketAddress
import akka.actor._


private[statsd] class Connection(
  connectionProps: Props,
  tolerateFailuresUpTo: Int
) extends Actor
  with Stash
  with ActorLogging {

  import Connection._

  var tolerateFailures = tolerateFailuresUpTo

  override def preStart(): Unit =
    openConnection()

  def openConnection(): Unit = {
    tolerateFailures = 0
    context become waitingForConnection
    context.actorOf(connectionProps) ! Open
  }

  def receive = waitingForConnection

  def waitingForConnection: Receive = {
    case Opened =>
      val connection = sender()
      context watch connection
      unstashAll()
      context become operational(connection)
    case msg: String =>
      stash()

  }

  def operational(connection: ActorRef): Receive = {
    case msg: String =>
      connection ! Deliver(msg)
    case Terminated(conn) =>
      val _ = context unwatch conn
    case DeliveryFailed(msg) =>
      tolerateFailures -= 1
      if (tolerateFailures < 0) {
        openConnection()
        sender() ! Kill
      } else
        log.debug(s"Delivery failed for $msg. Failures until reconnection $tolerateFailures")
  }

  override def unhandled(message: Any) = {
    log.warning(s"Unhandled message: $message (${message.getClass})")
  }
}

private[statsd] object Connection {
  type ConnectionProps = InetSocketAddress => Props

  def props(remoteAddress: InetSocketAddress, tolerateFailures: Int = 10) =
    Props(new Connection(
      udp.SingleAddressConnection.props(remoteAddress), tolerateFailures))

  sealed trait Message
  case object Open extends Message
  case object Opened extends Message
  case class Deliver(msg: String)
  case class DeliveryFailed(msg: String)
}
