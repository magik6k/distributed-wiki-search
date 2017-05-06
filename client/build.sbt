resolvers += "ipfs" at "https://ipfs.io/ipfs/QmZwTcMFFZ1SrM6R6pA5SKGd3Pfr8w6Mey5dG88UBLZA48"

lazy val root = (project in file(".")).settings(
  //npmDependencies in Compile += "ipfs" -> "0.23.1",
  webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),

  scalaVersion := "2.12.1",
  libraryDependencies ++= Seq(
    "eu.devtty" %%% "api-ipfs-node" % "0.2.4-SNAPSHOT",
    "org.scala-js" %%% "scalajs-dom" % "0.9.1"
  )
).enablePlugins(ScalaJSBundlerPlugin)
