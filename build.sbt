import tnm.ScalaVersion

val statsdSettings = Seq(
  organization := "com.thenewmotion",
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/thenewmotion/akka-actor-statsd")),
  scalaVersion := ScalaVersion.prev,
  releaseCrossBuild := false,
  libraryDependencies := {
    Dependencies(scalaVersion.value).commonTest
  }
)

val `akka-statsd-core` = project
  .enablePlugins(OssLibPlugin)
  .settings(
    statsdSettings,
    libraryDependencies ++= Dependencies(scalaVersion.value).core
  )

val `akka-statsd-spray-server` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`akka-statsd-core`)
  .settings(
    statsdSettings,
    libraryDependencies ++= Dependencies(scalaVersion.value).sprayServer
  )

val `akka-statsd-http-server` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`akka-statsd-core`)
  .settings(
    statsdSettings,
    libraryDependencies ++= Dependencies(scalaVersion.value).akkaHttpServer
  )

val `akka-statsd-spray-client` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`akka-statsd-core`)
  .settings(
    statsdSettings,
    libraryDependencies ++= Dependencies(scalaVersion.value).sprayClient
  )

val `akka-statsd` =
  project.in(file("."))
  .enablePlugins(OssLibPlugin)
  .aggregate(
    `akka-statsd-core`,
    `akka-statsd-spray-server`,
    `akka-statsd-spray-client`,
    `akka-statsd-http-server`)
  .settings(
    statsdSettings,
    publish := {}
  )


def Dependencies(scalaVersion: String) = new {

  def akka(lib: String) = {
    val version = lib match {
      case x if x.startsWith("http") => "10.0.1"
      case _ => "2.4.16"
    }

    "com.typesafe.akka" %% s"akka-$lib" % version
  }

  def spray(lib: String, v: String = "1.3.3") = "io.spray" %% s"spray-$lib" % v

  val ficus = "com.iheart" %%  "ficus" % "1.4.0"

  val core = Seq(
    akka("actor"),
    akka("slf4j"),
    ficus,
    "ch.qos.logback" % "logback-classic" % "1.1.8"
  )

  val sprayServer = core ++ Seq(
    spray("http"),
    spray("routing-shapeless2"),
    spray("testkit") % "test"
  )

  val sprayClient = core ++ Seq(
    spray("http"),
    spray("client")
  )

  val akkaHttpServer = core ++ Seq(
    akka("http"),
    akka("http-testkit") % "test"
  )

  val commonTest = Seq(
    akka("testkit"),
    "org.scalatest" %% "scalatest" % "3.0.1"
  ).map(_ % "test")
}
