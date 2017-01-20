val commonSettings = Seq(
  organization := "com.thenewmotion",
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/NewMotion/akka-statsd")),
  libraryDependencies ++= Seq(
    akka("testkit"),
    "org.scalatest" %% "scalatest" % "3.0.1"
  ).map(_ % "test")
)

val `akka-statsd-core` = project
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      akka("actor"),
      akka("slf4j"),
      "com.iheart" %%  "ficus" % "1.4.0",
      "ch.qos.logback" % "logback-classic" % "1.1.8"
    )
  )

val `akka-statsd-http-server` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`akka-statsd-core`)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      akka("http"),
      akka("http-testkit") % "test"
    )
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


def akka(lib: String) = {
  val version = lib match {
    case x if x.startsWith("http") => "10.0.1"
    case _ => "2.4.16"
  }

  "com.typesafe.akka" %% s"akka-$lib" % version
}
