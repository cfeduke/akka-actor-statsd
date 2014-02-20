package com.deploymentzone.actor

import scala.concurrent.duration.Duration
import akka.actor.{ActorLogging, Props, Actor}

private[actor] class ScheduledDispatcherActor(val packetSize: Int, val transmitInterval: Long)
  extends Actor
  with ActorLogging {

  import ScheduledDispatcherActor._

  require(packetSize > 0, PACKET_SIZE_NEGATIVE_ZERO_MESSAGE)
  require(transmitInterval > 0, TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE)

  def receive = {
    case _ =>
  }
}

private[actor] object ScheduledDispatcherActor {
  val PACKET_SIZE_NEGATIVE_ZERO_MESSAGE = "packetSize cannot be negative or 0"
  val TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE = "transmitInterval cannot be negative or 0"
  // TODO retrieve values from config

  def props(packetSize: Int, transmitInterval: Duration): Props =
    props(packetSize, transmitInterval.toMillis)
  def props(packetSize: Int, transmitInterval: Long) =
    Props(new ScheduledDispatcherActor(packetSize, transmitInterval))
}