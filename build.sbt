val scalaVer = "3.6.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "wordsearch",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:implicitConversions",
      "-language:existentials",
      "-Werror",
    ),
    libraryDependencies +=
      "org.jline" % "jline" % "3.28.0"
  )
