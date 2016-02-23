package deploymentzone.actor

import scala.concurrent.duration._
import akka.actor._
import akka.io.UdpConnected
import deploymentzone.actor.domain.MultiMetricQueue


private[actor] class ScheduledDispatcherActor(
  config: Config,
  newConnection: Props
) extends Actor
  with ActorLogging {

  import ScheduledDispatcherActor._

  import config.{packetSize, transmitInterval, enableMultiMetric}
  import context.system
  import context.dispatcher

  require(packetSize > 0, PACKET_SIZE_NEGATIVE_ZERO_MESSAGE)
  require(transmitInterval.toMillis > 0, TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE)

  log.debug(s"packetSize: $packetSize")
  log.debug(s"transmitInterval (millis): ${transmitInterval.toMillis}")

  if (transmitInterval.toMillis > 1.second.toMillis) {
    log.warning("Transmit interval set to a large value of {} milliseconds", transmitInterval)
  }

  val mmq = MultiMetricQueue(packetSize)(system)
  val connection = context.actorOf(newConnection, "underlying-udp-connection")

  val recurringTransmit =
    if (enableMultiMetric)
      system.scheduler.schedule(transmitInterval, transmitInterval, self, Transmit)
    else
      noopSchedule

  def receive = {
    case msg: String =>
      if (enableMultiMetric) mmq.enqueue(msg)
      else connection ! msg
    case Transmit =>
      mmq.payload().foreach(connection ! _)
  }

  override def postStop() {
    recurringTransmit.cancel()
  }

  private object Transmit
}

private[actor] object ScheduledDispatcherActor {
  val PACKET_SIZE_NEGATIVE_ZERO_MESSAGE = "packetSize cannot be negative or 0"
  val TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE = "transmitInterval cannot be negative or 0"

  private val noopSchedule = new Cancellable {
    def isCancelled = true
    def cancel() = true
  }

  def props(config: Config, connection: Props) =
    Props(new ScheduledDispatcherActor(config, connection))
}
