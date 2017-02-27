package akka.statsd

import akka.actor.{ActorLogging, Props, Actor, ActorRef}
import akka.io.{Udp, IO}
import java.net.InetSocketAddress


/* originated from: http://doc.akka.io/docs/akka/snapshot/scala/io-udp.html */
class UdpListener(nextActor: ActorRef, port: Int)
  extends Actor
  with ActorLogging {
  import context.system
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", port))

  def receive = {
    case Udp.Bound(local) =>
      context.become(ready(sender()))
      nextActor ! local
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      val str = data.utf8String
      log.debug("received data {}", str)
      nextActor ! str
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }
}

object UdpListener {
  def props(nextActor: ActorRef, port: Int = 0) = Props(new UdpListener(nextActor, port))
}
