package com.deploymentzone.actor

import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import com.deploymentzone.actor.domain.MultiMetricQueue

private[actor] class ScheduledDispatcherActor(val packetSize: Int, val transmitInterval: Long, val receiver: ActorRef)
  extends Actor
  with ActorLogging {

  import ScheduledDispatcherActor._

  require(packetSize > 0, PACKET_SIZE_NEGATIVE_ZERO_MESSAGE)
  require(transmitInterval > 0, TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE)

  val mmq = MultiMetricQueue(packetSize)

  def receive = {
    case msg: String => mmq.enqueue(msg)
    case Flush =>
        // TODO change payload to a Some or None
//      mmq.payload() match {
//
//      }
  }
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