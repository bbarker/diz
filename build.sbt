inThisBuild(
  List(
    version := "0.0.1-SNAPSHOT",
    scalaVersion := Version.scala,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0",
    semanticdbEnabled := true, // enable SemanticDB
    semanticdbVersion := scalafixSemanticdb.revision, // only required for Scala 2.x
    scalacOptions ++= Seq(
      "-explaintypes" // Explain type errors in more detail.
    ),
    Compile / doc / sources := Seq.empty,
    Compile / packageDoc / publishArtifact := false,
    Test / fork := true, // JVM forking allows to apply extra JVM setting to Test
    Test / javaOptions += "-Duser.timezone=UTC",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    resolvers ++= Seq()
  )
)

val root = project
  .in(file("."))
  .settings(
    name := "diz",
    description := "diz - a discord bot in Scala 3 and ZIO, using discord4j",
    scalaVersion := Version.scala,
    libraryDependencies ++= (Dep.commonDeps ++ Seq(
      Dep.discord4j,
      Dep.immutables
    ))
  )
