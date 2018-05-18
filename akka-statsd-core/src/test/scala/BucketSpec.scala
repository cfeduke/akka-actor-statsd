package akka.statsd

import org.scalatest._


class BucketSpec
  extends FunSpec
  with MustMatchers {

  describe("Bucket") {

    it("has forward slash (/) substituted with dot (.)") {
      Bucket("a/b").render must equal(Bucket("a.b").render)
    }

    it("has colon (:) substituted with underscore (_)") {
      Bucket("a:b").render must equal(Bucket("a_b").render)
    }

    it("has pipe (|) substituted with underscore (_)") {
      Bucket("a|b").render must equal(Bucket("a_b").render)
    }

    it("has commerce at (@) substituted with underscore (_)") {
      Bucket("a@b").render must equal(Bucket("a_b").render)
    }

    it("has backslash (\\) substituted with underscore (_)") {
      Bucket("a\\b").render must equal(Bucket("a_b").render)
    }

    it("has UUIDs substituted for [id]") {
      Bucket(java.util.UUID.randomUUID.toString).render must equal(Bucket("[id]").render)
    }

    it("favors provided transformations over default ones") {
      val ts = Seq(Transformation(".+", "anything"))

      Bucket(java.util.UUID.randomUUID.toString, ts) must equal(
        Bucket("anything", ts))
    }

    it("transforms over several path parts") {
      val ts = Seq(Transformation("""[a-z]+\.\d+""", "alphanum"))

      (Bucket("abc", ts) / "999").render must equal(
        Bucket("alphanum", ts).render)
    }

    it("has no leading delimiters") {
      Bucket(".abc").render must equal(Bucket("abc").render)
      Bucket("/abc").render must equal(Bucket("abc").render)
    }

    it("has no trailing delimiters") {
      Bucket("abc.").render must equal(Bucket("abc").render)
      Bucket("abc/").render must equal(Bucket("abc").render)
    }

    it("may be empty") {
      Bucket("").render must equal("")
    }

    it("prepended with namespace same as namespace followed by bucket") {
      Bucket("bucket").prepend("ns").render must equal((Bucket("ns")/"bucket").render)
    }
  }
}
