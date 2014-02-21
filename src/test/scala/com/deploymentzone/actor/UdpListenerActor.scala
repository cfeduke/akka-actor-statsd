package com.deploymentzone.actor

import akka.actor.{ActorLogging, Props, Actor, ActorRef}
import akka.io.{Udp, IO}
import java.net.InetSocketAddress

/* originated from: http://doc.akka.io/docs/akka/snapshot/scala/io-udp.html */
class UdpListenerActor(nextActor: ActorRef)
  extends Actor
  with ActorLogging {
  import context.system
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 0))

  def receive = {
    case Udp.Bound(local) =>
      context.become(ready(sender))
      nextActor ! local
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      log.debug("received data length {}", data.length)
      val str = data.decodeString("utf-8")
      nextActor ! str
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }
}

object UdpListenerActor {
  def props(nextActor: ActorRef) = Props(new UdpListenerActor(nextActor))
}
