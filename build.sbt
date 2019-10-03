name := "POC"

version := "0.1"

scalaVersion := "2.13.0"

val akka = "com.typesafe.akka" %% "akka-actor" % "2.6.0-M3"

lazy val hello = (project in file("."))
  .settings(
    name := "Hello",
    libraryDependencies += akka
  )