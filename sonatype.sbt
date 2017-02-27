pomExtra in Global := {
  <scm>
    <connection>scm:git:git@github.com:NewMotion/akka-statsd.git</connection>
    <developerConnection>scm:git:git@github.com:NewMotion/akka-statsd.git</developerConnection>
    <url>git@github.com:NewMotion/akka-statsd.git</url>
  </scm>
  <developers>
    <developer>
      <id>fedgehog</id>
      <name>Maxim Fedorov</name>
      <url>https://github.com/fedgehog</url>
    </developer>
    <developer>
      <id>kiequoo</id>
      <name>Dan Brooke</name>
      <url>https://github.com/kiequoo</url>
    </developer>
    <developer>
      <id>SamanSattary</id>
      <name>Saman Sattari</name>
      <url>https://github.com/SamanSattary</url>
    </developer>
  </developers>
}

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
