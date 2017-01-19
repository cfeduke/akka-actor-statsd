val commonSettings = Seq(
  organization := "com.thenewmotion",
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/NewMotion/akka-statsd")),
  libraryDependencies ++= Dependencies(scalaVersion.value).commonTest
)

val `akka-statsd-core` = project
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Dependencies(scalaVersion.value).core
  )

val `akka-statsd-http-server` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`akka-statsd-core`)
  .settings(
    commonSettings,
    libraryDependencies ++= Dependencies(scalaVersion.value).akkaHttpServer
  )

val `akka-statsd` =
  project.in(file("."))
  .enablePlugins(OssLibPlugin)
  .aggregate(
    `akka-statsd-core`,
    `akka-statsd-http-server`)
  .settings(
    commonSettings,
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

  val akkaHttpServer = Seq(
    akka("http"),
    akka("http-testkit") % "test"
  )

  val commonTest = Seq(
    akka("testkit"),
    "org.scalatest" %% "scalatest" % "3.0.1"
  ).map(_ % "test")
}
