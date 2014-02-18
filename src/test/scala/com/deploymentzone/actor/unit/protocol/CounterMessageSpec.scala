package com.deploymentzone.actor.unit.protocol

import org.scalatest.{Matchers, WordSpec}
import com.deploymentzone.actor.protocol._
import scala.concurrent.duration._

class CounterMessageSpec
  extends WordSpec
  with Matchers {

  "A CounterMessage implementation" when {
    "creating a new instance" should {
      "not permit null buckets" in {
        an [IllegalArgumentException] should be thrownBy new CounterMessage(null)(1) { override val symbol = "y" }
      }
      "not permit buckets with reserved character names" in {
        an [IllegalArgumentException] should be thrownBy new CounterMessage("a:name")(1) { override val symbol = "z" }
      }
    }
    "invoking toString" when {
      "using a 1.0 sampling rate" should {
        "return the expected value" in new Implementation(123) {
          subject.toString should be ("deploymentzone.sprockets:123|x")
        }
      }
      "using a 5.6 sampling rate" should {
        "return the expected value" in new Implementation(1337, 5.6) {
          subject.toString should be ("deploymentzone.sprockets:1337|x|@5.6")
        }
      }
    }
    "invoking namespace" when {
      "given an empty string argument" should {
        "return only the bucket name" in new NamespaceTest {
          subject.namespace("").bucket should be ("bucket")
        }
      }
      "given a null argument" should {
        "return only the bucket name" in new NamespaceTest {
          subject.namespace(null).bucket should be ("bucket")
        }
      }
      "given a valid namespace" should {
        "return the namespace.bucket" in new NamespaceTest {
          subject.namespace("deploymentzone.ninjas").bucket should be ("deploymentzone.ninjas.bucket")
        }
      }
    }
  }

  "Count" when {
    "invoking toString" should {
      "return the expected value" in {
        Count("x.z")(9).toString should be ("x.z:9|c")
      }
    }
  }

  "Increment" when {
    "invoking toString" should {
      "return the expected value" in {
        Increment("a.b").toString should be ("a.b:1|c")
      }
    }
  }

  "Decrement" when {
    "invoking toString" should {
      "return the expected value" in {
        Decrement("c.d").toString should be ("c.d:-1|c")
      }
    }
  }

  "Gauge" when {
    "invoking toString" should {
      "return the expected value" in {
        Gauge("e.f")(900L).toString should be ("e.f:900|g")
      }
    }
  }

  "Timing" when {
    "invoking toString" when {
      "using a Long to represent milliseconds" should {
        "return the expected value" in {
          Timing("q.z")(4000.millis).toString should be ("q.z:4000|ms")
        }
      }
      "using 23 seconds" should {
        "return the expected value" in {
          Timing("r.x")(23.seconds).toString should be ("r.x:23000|ms")
        }
      }

    }
  }

  private class Implementation[T](value: T, samplingRate: Double = 1.0) {
    val subject = new CounterMessage("deploymentzone.sprockets")(value, samplingRate) { override val symbol = "x" }
  }

  private class NamespaceTest {
    val subject = new CounterMessage("bucket")(1) { override val symbol = "&" }
  }

}
