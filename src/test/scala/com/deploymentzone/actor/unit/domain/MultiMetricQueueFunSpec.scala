package com.deploymentzone.actor.unit.domain

import org.scalatest.FunSpec
import com.deploymentzone.actor.domain.MultiMetricQueue
import com.deploymentzone.actor.ImplicitActorSystem

class MultiMetricQueueFunSpec
  extends FunSpec
  with ImplicitActorSystem {

  describe("A MultiMetricQueue") {
    describe("when empty") {
      it("returns an empty payload") {
        assert(MultiMetricQueue().payload() == "")
      }
    }

    describe("when having a single element") {
      it("returns that element with no newline") {
        val subject = MultiMetricQueue().enqueue("message")
        assert(subject.payload() == "message")
      }
    }

    describe("when having two elements") {
      it("returns the elements separated by a newline") {
        val subject = MultiMetricQueue().enqueue("message1").enqueue("message2")
        assert(subject.payload() ==
          """message1
            |message2""".stripMargin)
      }
    }

    describe("when the elements cross the packetSize boundary") {
      it("first returns one element then another") {
        val subject = MultiMetricQueue(4).enqueue("dog").enqueue("cat")
        assert(subject.payload() == "dog")
        assert(subject.payload() == "cat")
      }
    }

    describe("when a UTF-8 character crosses the packetSize boundary") {
      it("first returns one element then another") {
        val subject = MultiMetricQueue(2).enqueue("ü").enqueue("u")
        assert(subject.payload() == "ü")
        assert(subject.payload() == "u")
      }
    }

    describe("when a single message goes over the packetSize boundary") {
      it("drops the message") {
        val subject = MultiMetricQueue(4).enqueue("12345")
        assert(subject.payload() == "")
        assert(subject.size == 0)
      }
    }

    describe("when the first message goes over the packetSize boundary") {
      it("drops the oversized message but continues with other messages") {
        val subject = MultiMetricQueue(4).enqueue("12345").enqueue("1").enqueue("2")
        assert(subject.payload() ==
          """1
            |2""".stripMargin)
      }
    }

    describe("when any message goes over the packetSize boundary") {
      it("drops the oversized message but continues with other messages") {
        val subject = MultiMetricQueue(4).enqueue("1").enqueue("12345").enqueue("2")
        assert(subject.payload() ==
          """1
            |2""".stripMargin)
      }
    }
  }
}
