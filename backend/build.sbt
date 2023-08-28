name := """backend"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
.settings(
   Test / javaOptions += "-Dconfig.resource=application.test.conf"
)



scalaVersion := "2.13.11"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies +=  "org.mockito" % "mockito-core" % "5.5.0" % Test
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0",
  "io.swagger" % "swagger-core" % "1.6.2",
  "org.webjars" % "swagger-ui" % "3.52.5",
  "org.apache.directory.api" % "api-all" % "2.1.3",
  "com.github.jwt-scala" %% "jwt-core" % "8.0.2",
  "com.github.jwt-scala" %% "jwt-play-json" % "8.0.2",
  "com.github.scredis" %% "scredis" % "2.4.3",
  "org.postgresql" % "postgresql" % "42.6.0"
)

