package com.deploymentzone.actor

import akka.actor.{Props, ActorLogging, Actor}
import com.deploymentzone.actor.util.StatsDBucketValidator

class StatsActor(val namespace: String = "")
  extends Actor
  with ActorLogging {

  require(StatsDBucketValidator(namespace),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in namespaces and namespaces may not start or end with a period (".")""")

  def receive = {
    case _ =>
  }

}

object StatsActor {
  def props(namespace: String) = Props(new StatsActor(namespace))
}
