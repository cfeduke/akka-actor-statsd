package deploymentzone
package actor

import scala.concurrent.duration._
import akka.actor._
import domain.MultiMetricQueue


private[actor] class ScheduledDispatcher(
  config: Config,
  newConnection: Props
) extends Actor
  with ActorLogging {

  import ScheduledDispatcher._

  import config.{packetSize, transmitInterval, enableMultiMetric}
  import context.system
  import context.dispatcher

  require(packetSize > 0, IllegalPacketSize)
  require(transmitInterval.toMillis > 0, IllegalTransmitInterval)

  log.debug(s"packetSize: $packetSize")
  log.debug(s"transmitInterval (millis): ${transmitInterval.toMillis}")

  if (transmitInterval > 1.second) {
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

private[actor] object ScheduledDispatcher {
  val IllegalPacketSize = "Packet size must be positive number"
  val IllegalTransmitInterval = "Transmit interval must be positive number"

  private val noopSchedule = new Cancellable {
    def isCancelled = true
    def cancel() = true
  }

  def props(config: Config, connection: Props) =
    Props(new ScheduledDispatcher(config, connection))
}
