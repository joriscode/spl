import sbt.Keys._

// use eliding to drop some debug code in the production build
lazy val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")

// instantiate the JVM project for SBT with some additional settings
lazy val projects = (project in file("."))
  .settings(
    name := "Spl",
    version := "1.0.0",
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    retrieveManaged := true,
    //resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    resolvers += "RoundEights" at "http://maven.spikemark.net/roundeights",
    libraryDependencies ++= Settings.dependencies.value
  )

assemblyJarName in assembly := "spl.jar"

enablePlugins(JavaAppPackaging)
mainClass in Compile := Some("center.scala.spl.Main")
