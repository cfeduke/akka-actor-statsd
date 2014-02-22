package deploymentzone.actor

import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import deploymentzone.actor.domain.MultiMetricQueue
import java.util.concurrent.TimeUnit

private[actor] class ScheduledDispatcherActor(val packetSize: Int, val transmitInterval: Long, val receiver: ActorRef)
  extends Actor
  with ActorLogging {

  import ScheduledDispatcherActor._
  import context.system

  require(packetSize > 0, PACKET_SIZE_NEGATIVE_ZERO_MESSAGE)
  require(transmitInterval > 0, TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE)

  if (transmitInterval > 1.second.toMillis) {
    log.warning("Transmit interval set to a large value of {} milliseconds", transmitInterval)
  }

  val mmq = MultiMetricQueue(packetSize)(system)

  private val duration = Duration(transmitInterval, TimeUnit.MILLISECONDS)
  val recurringTransmit = system.scheduler.schedule(duration, duration, self, Transmit)(system.dispatcher, self)

  def receive = {
    case msg: String => mmq.enqueue(msg)
    case Transmit =>
      mmq.payload().foreach(payload => receiver ! payload)
  }

  override def postStop() {
    recurringTransmit.cancel()
  }

  private object Transmit
}

private[actor] object ScheduledDispatcherActor {
  val PACKET_SIZE_NEGATIVE_ZERO_MESSAGE = "packetSize cannot be negative or 0"
  val TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE = "transmitInterval cannot be negative or 0"
  private val DEFAULT_TRANSMIT_INTERVAL = 100.milliseconds

  def props(packetSize: Int, transmitInterval: Long, receiver: ActorRef) =
    Props(new ScheduledDispatcherActor(packetSize, transmitInterval, receiver))
  def props(packetSize: Int, transmitInterval: Duration, receiver: ActorRef): Props =
    props(packetSize, transmitInterval.toMillis, receiver)
  // TODO have this constructor attempt to retrieve values from config before using defaults
  def props(receiver: ActorRef) =
    Props(new ScheduledDispatcherActor(PacketSize.FAST_ETHERNET, DEFAULT_TRANSMIT_INTERVAL.toMillis, receiver))
  //  TODO have this constructor attempt to retrieve values from config before using default
  def props(transmitInterval: Duration, receiver: ActorRef): Props =
    props(PacketSize.FAST_ETHERNET, transmitInterval.toMillis, receiver)
}