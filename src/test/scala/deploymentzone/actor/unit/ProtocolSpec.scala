package deploymentzone.actor.unit

import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._
import deploymentzone.actor._

class ProtocolSpec
  extends WordSpec
  with Matchers {

  "A Metric implementation" when {
    "creating a new instance" should {
      "not permit null buckets" in {
        an [IllegalArgumentException] should be thrownBy new Metric(null, 1.0)(1) { override val symbol = "y" }
      }
      "not permit buckets with reserved character names" in {
        an [IllegalArgumentException] should be thrownBy new Metric("a:name", 1.0)(1) { override val symbol = "z" }
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

  "GaugeAdd" when {
    "invoking toString" should {
      "return the expected value" in {
        GaugeAdd("m.n")(34).toString should be ("m.n:+34|g")
      }
    }
    "invoking toString on a negative number" should {
      "replace the negation sign with a positive sign" in {
        GaugeAdd("n.o")(-96).toString should be ("n.o:+96|g")
      }
    }
  }

  "GaugeSubtract" when {
    "invoking toString" should {
      "return the expected value" in {
        GaugeSubtract("m.n")(34).toString should be ("m.n:-34|g")
      }
    }
    "invoking toString on a negative number" should {
      "not have two negative signs" in {
        GaugeSubtract("n.o")(-96).toString should be ("n.o:-96|g")
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

  "Set" when {
    "invoking toString" should {
      "return the expected value" in {
        Set("n.q")(12).toString should be ("n.q:12|s")
      }
    }
  }

  private class Implementation[T](value: T, samplingRate: Double = 1.0) {
    val subject = new Metric("deploymentzone.sprockets", samplingRate)(value) { override val symbol = "x" }
  }



}
