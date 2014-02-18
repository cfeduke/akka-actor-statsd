package com.deploymentzone.actor

import akka.actor.{Stash, Actor, ActorRef}
import com.deploymentzone.actor.protocol.CounterMessage
import akka.io.UdpConnected

private[actor] trait StatsProtocolImplementation
  extends Stash { this: Actor =>
  def connection: ActorRef

  protected def process(msg: CounterMessage[_]): String

  connection ! UdpConnected.Connect

  override def receive = connectionPending

  protected def connectionPending: Actor.Receive = {
    case UdpConnected.Connected =>
      unstashAll()
      context.become(connected)
    case _ => stash()
  }
  
  protected def connected: Actor.Receive = {
    case msg: CounterMessage[_] => connection ! process(msg)
  }
}
