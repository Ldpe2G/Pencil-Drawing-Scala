lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.8"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "pencil_style_scala"
  )


libraryDependencies  ++= Seq(
  "org.scalanlp" %% "breeze" % "latest.integration",
  "org.scalanlp" %% "breeze-natives" % "0.12",
  "org.scalanlp" %% "breeze-viz" % "0.12",
  "nu.pattern" % "opencv" % "2.4.9-7"
)


resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
