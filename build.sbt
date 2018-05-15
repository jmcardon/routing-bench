val Http4sVersion = "0.18.11"
val Specs2Version = "4.0.2"
val LogbackVersion = "1.2.3"
val tsecV = "0.0.1-M11"

lazy val root = (project in file("."))
  .settings(
    organization := "com.example",
    name := "quickstart",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.4",
//    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "io.circe" %% "circe-generic" % "0.9.1",
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "io.monix" %% "monix" % "3.0.0-RC1",
//      "io.github.jmcardon" %% "tsec-common" % "0.0.1-SNAPSHOT",
      "io.github.jmcardon" %% "tsec-common" % tsecV,
      "io.github.jmcardon" %% "tsec-password" % tsecV,
      "io.github.jmcardon" %% "tsec-cipher-jca" % tsecV,
      "io.github.jmcardon" %% "tsec-cipher-bouncy" % tsecV,
      "io.github.jmcardon" %% "tsec-mac" % tsecV,
      "io.github.jmcardon" %% "tsec-signatures" % tsecV,
      "io.github.jmcardon" %% "tsec-hash-jca" % tsecV,
      "io.github.jmcardon" %% "tsec-hash-bouncy" % tsecV,
      "io.github.jmcardon" %% "tsec-libsodium" % tsecV,
      "io.github.jmcardon" %% "tsec-jwt-mac" % tsecV,
      "io.github.jmcardon" %% "tsec-jwt-sig" % tsecV,
      "io.github.jmcardon" %% "tsec-http4s" % tsecV
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
    javaOptions ++= Seq("-Xmx4g", "-Xms1g"),
    fork in run := true
  )
  .enablePlugins(JmhPlugin)
