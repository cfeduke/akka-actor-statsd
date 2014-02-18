package com.deploymentzone.actor

import org.scalatest.{Suite, BeforeAndAfterAll}
import akka.actor.ActorSystem

trait ImplicitActorSystem
  extends BeforeAndAfterAll { this: Suite =>
  implicit val system = ActorSystem()
  override protected def afterAll() {
    system.shutdown()
  }
}
