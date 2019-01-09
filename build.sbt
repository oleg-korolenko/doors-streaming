ThisBuild / resolvers ++= Seq(
    "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
    Resolver.mavenLocal
)

name := "doors-exercise"

version := "0.1-SNAPSHOT"

organization := "com.doors"

ThisBuild / scalaVersion := "2.12.1"

val flinkDependencies = {
  val flinkVersion = "1.7.0"

  Seq(
    "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
    "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided",
    "org.apache.flink" % "flink-connector-kafka_2.12" % flinkVersion
  )
}

libraryDependencies+="org.scalatest" %% "scalatest" % "3.0.5" % "test"


lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= flinkDependencies
  )

assembly / mainClass := Some("com.doors.jobs.DoorsJob")

// make run command include the provided dependencies
Compile / run  := Defaults.runTask(Compile / fullClasspath,
                                   Compile / run / mainClass,
                                   Compile / run / runner
                                  ).evaluated

// stays inside the sbt console when we press "ctrl-c" while a Flink programme executes with "run" or "runMain"
Compile / run / fork := true
Global / cancelable := true

// exclude Scala library from assembly
assembly / assemblyOption  := (assembly / assemblyOption).value.copy(includeScala = false)

assemblyJarName in assembly := "doors-exercise-job.jar"