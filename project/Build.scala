import sbt._
import sbt.Keys._

object Build extends sbt.Build {

  lazy val project = Project(
    id = "akka-actor-statsd",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name                  := "akka-actor-statsd",
      organization          := "com.deploymentzone",
      version               := "0.4-SNAPSHOT",
      licenses              := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
      homepage              := Some(url("https://github.com/cfeduke/akka-actor-statsd/")),
      scalaVersion          := CrossBuild.Versions.scala_211,
      crossScalaVersions    := Seq(CrossBuild.Versions.scala_211, CrossBuild.Versions.scala_210),
      //ReleaseKeys.crossBuild := true,
      scalacOptions         := Seq(
                                "-encoding", "UTF-8",
                                "-unchecked",
                                "-deprecation",
                                "-feature",
                                "-Xlog-reflective-calls"
                              ),

      libraryDependencies   ++= Dependencies(),

      libraryDependencies   += ficusVersion(scalaVersion).value,

      publishMavenStyle     := true,
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (version.value.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      },
      pomIncludeRepository := { _ => false },
      pomExtra :=
          <scm>
            <url>git@github.com:cfeduke/akka-actor-statsd.git</url>
            <connection>scm:git:git@github.com:cfeduke/akka-actor-statsd.git</connection>
          </scm>
          <developers>
            <developer>
              <id>cfeduke</id>
              <name>Charles Feduke</name>
              <url>http://www.deploymentzone.com</url>
            </developer>
          </developers>
    )
  )

  object CrossBuild {
    object Versions {
      val scala_210 = "2.10.4"
      val scala_211 = "2.11.5"
    }
  }

  object Dependencies {

    object Versions {
      val akka              = "2.3.4"
      val scalatest         = "2.2.1"
      val logback           = "1.0.13"
    }

    val compileDependencies = Seq(
      "com.typesafe.akka"   %%  "akka-actor"      % Versions.akka,
      "com.typesafe.akka"   %%  "akka-slf4j"      % Versions.akka,
      "ch.qos.logback"      %   "logback-classic" % Versions.logback
    )

    val testDependencies = Seq(
      "com.typesafe.akka"   %% "akka-testkit"     % Versions.akka         % "test",
      "org.scalatest"       %% "scalatest"        % Versions.scalatest    % "test"
    )

    def apply(): Seq[ModuleID] = compileDependencies ++ testDependencies

  }

  def ficusVersion(scalaVersion:SettingKey[String]) = Def.setting {
    scalaVersion.value match {
      case CrossBuild.Versions.scala_210 => "net.ceedubs" %%  "ficus" %"1.0.1"
      case CrossBuild.Versions.scala_211 => "net.ceedubs" %%  "ficus" %"1.1.2"
    }
  }
}
