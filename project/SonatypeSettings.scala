import sbt._

object SonatypeSettings {

  val nexus = "https://oss.sonatype.org/"
  val snapshotsUrl =  Some("snapshots" at nexus + "content/repositories/snapshots")
  val releasesUrl =  Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
