import sbt.*

object Version {
  val scala = "3.2.2"
  val zio = "2.0.5"
}

object Dep {
  val zio = "dev.zio" %% "zio" % Version.zio
  val zioPrelude = "dev.zio" %% "zio-prelude" % "1.0.0-RC16"
  val zioTest = "dev.zio" %% "zio-test" % Version.zio
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zio
  val zioInteropReactiveStreams =
    "dev.zio" %% "zio-interop-reactivestreams" % "2.0.0"
//val zioInteropCats = "dev.zio" %% "zio-interop-cats" % "3.1.1.0"

  val discord4j = "com.discord4j" % "discord4j-core" % "3.2.6"
  val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.4"

  // For some reason this isn't picked up by the dependency resolver
  // https://stackoverflow.com/questions/69166256/bad-symbolic-reference-with-discord4j
  val immutables = "org.immutables" % "value" % "2.9.3"

  val commonDeps: Seq[ModuleID] = Seq(zio, zioPrelude)

// use for modules that require zio-test only
  val testDeps: Seq[ModuleID] = Seq(
    zioTest,
    zioTestSbt
  )
}
