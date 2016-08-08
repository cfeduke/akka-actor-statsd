package akka.statsd

import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._


class ProtocolSpec
  extends WordSpec
  with Matchers {

  "A Metric implementation" when {
    "invoking toString" when {
      class Implementation[T](value: T, samplingRate: Double = 1.0) {
        val subject = new Metric(Bucket("deploymentzone.sprockets"), value, "x", samplingRate)
      }

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
        Count(Bucket("x.z"))(9).toString should be ("x.z:9|c")
      }
    }
  }

  "Increment" when {
    "invoking toString" should {
      "return the expected value" in {
        Increment(Bucket("a.b")).toString should be ("a.b:1|c")
      }
    }
  }

  "Decrement" when {
    "invoking toString" should {
      "return the expected value" in {
        Decrement(Bucket("c.d")).toString should be ("c.d:-1|c")
      }
    }
  }

  "Gauge" when {
    "invoking toString" should {
      "return the expected value" in {
        Gauge(Bucket("e.f"))(900L).toString should be ("e.f:900|g")
      }
    }
  }

  "GaugeAdd" when {
    "invoking toString" should {
      "return the expected value" in {
        GaugeAdd(Bucket("m.n"))(34).toString should be ("m.n:+34|g")
      }
    }
    "invoking toString on a negative number" should {
      "replace the negation sign with a positive sign" in {
        GaugeAdd(Bucket("n.o"))(-96).toString should be ("n.o:+96|g")
      }
    }
  }

  "GaugeSubtract" when {
    "invoking toString" should {
      "return the expected value" in {
        GaugeSubtract(Bucket("m.n"))(34).toString should be ("m.n:-34|g")
      }
    }
    "invoking toString on a negative number" should {
      "not have two negative signs" in {
        GaugeSubtract(Bucket("n.o"))(-96).toString should be ("n.o:-96|g")
      }
    }
  }

  "Timing" when {
    "invoking toString" when {
      "using a Long to represent milliseconds" should {
        "return the expected value" in {
          Timing(Bucket("q.z"))(4000.millis).toString should be ("q.z:4000|ms")
        }
      }
      "using 23 seconds" should {
        "return the expected value" in {
          Timing(Bucket("r.x"))(23.seconds).toString should be ("r.x:23000|ms")
        }
      }

    }
  }

  "Set" when {
    "invoking toString" should {
      "return the expected value" in {
        StatSet(Bucket("n.q"))(12).toString should be ("n.q:12|s")
      }
    }
  }
}
