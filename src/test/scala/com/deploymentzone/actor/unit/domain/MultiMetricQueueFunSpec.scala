package com.deploymentzone.actor.unit.domain

import org.scalatest.FunSpec
import com.deploymentzone.actor.domain.MultiMetricQueue
import scala.collection.immutable

class MultiMetricQueueFunSpec
  extends FunSpec {

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
  }
}
