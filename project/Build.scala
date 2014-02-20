import sbt._
import sbt.Keys._

object Build extends sbt.Build {

  lazy val project = Project(
    id = "akka-actor-statsd",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name                  := "akka-actor-statsd",
      organization          := "com.deploymentzone",
      version               := "0.1-SNAPSHOT",
      scalaVersion          := "2.10.3",
      scalacOptions         := Seq("-deprecation", "-feature", "-encoding", "utf8"),
      libraryDependencies   ++= Dependencies()
    )
  )

  object Dependencies {

    object Versions {
      val akka = "2.2.3"
      val scalatest = "2.0"
      val grizzledSlf4j = "1.0.1"
    }

    val compileDependencies = Seq(
      "com.typesafe.akka"   %%      "akka-actor"  % Versions.akka,
      "org.clapper"         %% "grizzled-slf4j"   % Versions.grizzledSlf4j
    )

    val testDependencies = Seq(
      "com.typesafe.akka"   %% "akka-testkit"     % Versions.akka       % "test",
      "org.scalatest"       %% "scalatest"        % Versions.scalatest  % "test"
    )

    def apply(): Seq[ModuleID] = compileDependencies ++ testDependencies

  }

}
