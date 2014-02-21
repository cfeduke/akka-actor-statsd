package com.deploymentzone.actor

import akka.actor.{ActorLogging, Stash, Actor, ActorRef}
import akka.io.UdpConnected

private[actor] trait StatsProtocolImplementation
  extends Stash
  with ActorLogging { this: Actor =>

  protected def connection: ActorRef
  private var scheduledDispatcher: ActorRef = _

  protected def process(msg: Metric[_]): String

  override def preStart() {
    connection ! UdpConnected.Connect
    scheduledDispatcher = context.actorOf(ScheduledDispatcherActor.props(connection), "scheduled")
  }

  override def receive = connectionPending

  protected def connectionPending: Actor.Receive = {
    case UdpConnected.Connected =>
      unstashAll()
      context.become(connected)
    case _ => stash()
  }
  
  protected def connected: Actor.Receive = {
    case msg: Metric[_] =>
      scheduledDispatcher ! process(msg)
  }
}
