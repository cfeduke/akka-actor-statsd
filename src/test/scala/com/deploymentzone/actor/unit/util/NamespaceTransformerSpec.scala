package com.deploymentzone.actor.unit.util

import org.scalatest.{Matchers, WordSpec}
import com.deploymentzone.actor.{util, Metric}
import java.lang.IllegalArgumentException
import com.deploymentzone.actor.util.NamespaceTransformer

class NamespaceTransformerSpec
  extends WordSpec
  with Matchers {

  "invoking NamespaceTransformer" when {
    "given an empty string argument" should {
      "return only the bucket name" in new NamespaceTest {
        NamespaceTransformer("")(counter) should be (counter.toString)
      }
    }
    "given a null argument" should {
      "return only the bucket name" in new NamespaceTest {
        NamespaceTransformer(null)(counter) should be (counter.toString)
      }
    }
    "given a valid namespace" should {
      "return the namespace.bucket" in new NamespaceTest {
        NamespaceTransformer("x.y")(counter) should be (s"x.y.$counter")
      }
    }
    "given a namespace that ends with a period" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer("x.y.")(counter)
      }
    }
    "given a namespace that starts with a period" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer(".x.y")(counter)
      }
    }
    "given a namespace that contains a reserved character" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer("x@y")(counter)
      }
    }
    "given a null counter" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer("x.y")(null)
      }
    }
  }

  private class NamespaceTest {
    val counter = new Metric("bucket", 1.0)(1) { override val symbol = "&" }
  }

}
