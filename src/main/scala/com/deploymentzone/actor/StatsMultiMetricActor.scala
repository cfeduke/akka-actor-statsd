package com.deploymentzone.actor

import scala.concurrent.duration.Duration
import akka.actor.Actor

private[actor] class StatsMultiMetricActor(val packetSize: Int, val transmitInterval: Long)
  extends Actor {

  def receive = {
    case _ =>
  }
}

private[actor] object StatsMultiMetricActor {

  def apply(packetSize: Int, transmitInterval: Long) = new StatsMultiMetricActor(packetSize, transmitInterval)
}