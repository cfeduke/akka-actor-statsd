import com.typesafe.sbt.pgp.PgpKeys
import ReleaseTransformations._

val commonSettings = Seq(
  organization := "com.newmotion",
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/NewMotion/akka-statsd")),
  libraryDependencies ++= Seq(
    akka("testkit"),
    "org.scalatest" %% "scalatest" % "3.0.5"
  ).map(_ % "test"),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  )
)

val `akka-statsd-core` = project
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      akka("actor"),
      akka("slf4j")
    )
  )

val `akka-statsd-http-client` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`akka-statsd-core`)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      akka("http"),
      akka("stream")
    )
  )

val `akka-statsd-http-server` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`akka-statsd-core`)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      akka("http"),
      akka("stream"),
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
      akka("http-testkit") % "test"
    )
  )

val `akka-statsd` =
  project.in(file("."))
  .enablePlugins(OssLibPlugin)
  .aggregate(
    `akka-statsd-core`,
    `akka-statsd-http-server`,
    `akka-statsd-http-client`
  )
  .settings(
    commonSettings,
    publish := {}
  )


def akka(lib: String) = {
  val version = lib match {
    case x if x.startsWith("http") => "10.1.5"
    case _ => "2.5.18"
  }

  "com.typesafe.akka" %% s"akka-$lib" % version
}

