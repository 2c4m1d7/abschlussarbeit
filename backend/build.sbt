name := """backend"""
organization := "htw"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.11"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0",
  "io.swagger" % "swagger-core" % "1.6.2",
  "org.webjars" % "swagger-ui" % "3.52.5",
  "org.apache.directory.api" % "api-all" % "2.1.3",
  "com.github.jwt-scala" %% "jwt-core" % "8.0.2",
  "com.github.jwt-scala" %% "jwt-play-json" % "8.0.2",
)

libraryDependencies += ws
libraryDependencies += ehcache

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "htw.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "htw.binders._"
