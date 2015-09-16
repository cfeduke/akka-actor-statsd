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
      crossScalaVersions    := Seq("2.10.4", "2.11.7"),
      scalacOptions         := Seq("-deprecation", "-feature", "-encoding", "utf8"),
      libraryDependencies   <++= (scalaVersion) { sv => Dependencies(sv) },
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

  object Dependencies {

    object Versions {
      val akka              = "2.3.4"
      val scalatest         = "2.2.1"
      val logback           = "1.0.13"
      def ficus(scalaVersion : String) = scalaVersion match {
        case "2.11.7" => "1.1.2"
        case "2.10.4" => "1.0.1"
      }
    }

    def compileDependencies(scalaVersion : String) = Seq(
      "com.typesafe.akka"   %%  "akka-actor"      % Versions.akka,
      "com.typesafe.akka"   %%  "akka-slf4j"      % Versions.akka,
      "ch.qos.logback"      %   "logback-classic" % Versions.logback,
      "net.ceedubs"         %%  "ficus"           % Versions.ficus(scalaVersion)
    )

    val testDependencies = Seq(
      "com.typesafe.akka"   %% "akka-testkit"     % Versions.akka         % "test",
      "org.scalatest"       %% "scalatest"        % Versions.scalatest    % "test"
    )

    def apply(scalaVersion : String): Seq[ModuleID] = compileDependencies(scalaVersion) ++ testDependencies

  }

}
