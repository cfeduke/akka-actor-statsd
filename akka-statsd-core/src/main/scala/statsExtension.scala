package akka.statsd

import akka.actor.{ActorRef, ExtendedActorSystem, Extension, ExtensionId}
import akka.statsd.{Config => StatsConfig}
import java.util.concurrent.ConcurrentHashMap
import scala.compat.java8.FunctionConverters._

class StatsExtensionImpl(system: ExtendedActorSystem) extends Extension {
  private val actors = new ConcurrentHashMap[StatsConfig, ActorRef]()

  def statsActor(config: StatsConfig): ActorRef =
    actors.computeIfAbsent(config, ((c: StatsConfig) => system.actorOf(Stats.props(c))).asJava)
}

object StatsExtension extends ExtensionId[StatsExtensionImpl] {
  override def createExtension(system: ExtendedActorSystem) =
    new StatsExtensionImpl(system)
}
