package com.deploymentzone.actor.unit.protocol

import org.scalatest.{Matchers, WordSpec}
import com.deploymentzone.actor.protocol.CounterMessage

class CounterMessageSpec
  extends WordSpec
  with Matchers {

  "A CounterMessage implementation" when {
    "invoking toString" when {
      "using a 1.0 sampling rate" should {
        "return the expected value" in new Concrete(123) {
          subject.toString should be ("deploymentzone.sprockets:123|x")
        }
      }
      "using a 5.6 sampling rate" should {
        "return the expected value" in new Concrete(1337, 5.6) {
          subject.toString should be ("deploymentzone.sprockets:1337|x|@5.6")
        }
      }
    }
  }

  private class Concrete[T](value: T, samplingRate: Double = 1.0) {
    val subject = new CounterMessage("deploymentzone.sprockets", value, samplingRate) { override val symbol = "x" }
  }

}
