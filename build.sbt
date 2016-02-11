name := "akka-actor-statsd"
organization := "com.thenewmotion"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
homepage := Some(url("https://github.com/thenewmotion/akka-actor-statsd"))

enablePlugins(OssLibPlugin)

libraryDependencies := {
  def akka(lib: String) = "com.typesafe.akka" %% s"akka-$lib" % {
    scalaVersion.value match {
      case tnm.ScalaVersion.curr => "2.4.2"
      case tnm.ScalaVersion.prev => "2.3.14"
    }
  }

  val ficus = "net.ceedubs" %%  "ficus" % {
    scalaVersion.value match {
      case tnm.ScalaVersion.curr => "1.1.2"
      case tnm.ScalaVersion.prev => "1.0.1"
    }
  }

  val compileDependencies = Seq(
    akka("actor"),
    akka("slf4j"),
    ficus,
    "ch.qos.logback" % "logback-classic" % "1.1.5"
  )

  val testDependencies = Seq(
    akka("testkit"),
    "org.scalatest" %% "scalatest" % "2.2.6"
  ).map(_ % "test")

  compileDependencies ++
  testDependencies
}
