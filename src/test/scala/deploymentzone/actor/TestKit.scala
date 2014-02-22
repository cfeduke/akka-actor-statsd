package deploymentzone.actor

import org.scalatest.{BeforeAndAfterAll, Suite}
import akka.actor.ActorSystem

trait BeforeAndAfterAllTestKit
  extends Suite with BeforeAndAfterAll {

  implicit def system: ActorSystem

  override protected def afterAll() {
    akka.testkit.TestKit.shutdownActorSystem(system)
  }

}

abstract class TestKit(actorSystemName: String)
  extends akka.testkit.TestKit(ActorSystem(actorSystemName))
  with BeforeAndAfterAllTestKit { this: Suite =>
}


