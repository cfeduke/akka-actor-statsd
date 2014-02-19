package com.deploymentzone.actor.unit.util

import org.scalatest.{Matchers, WordSpec}
import com.deploymentzone.actor.util.StatsDBucketValidator

class StatsDBucketValidatorSpec
  extends WordSpec
  with Matchers {

  "StatsDValidatorSanitizer" should {
    "return true if no reserved characters are used" in {
      StatsDBucketValidator("plaintext") should be(true)
    }
    "return false if a reserved character is found" in {
      StatsDBucketValidator("replaced@character") should be(false)
    }
    "return false if any reserved character is found" in {
      StatsDBucketValidator("counter_:@|\\") should be(false)
    }
    "return false if string starts with a period" in {
      StatsDBucketValidator(".counter") should be(false)
    }
    "return false if string ends with a period" in {
      StatsDBucketValidator("counter.") should be(false)
    }
    "return true if string is null" in {
      StatsDBucketValidator(null) should be(true)
    }
  }
}
