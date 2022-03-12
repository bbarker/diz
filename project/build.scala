import sbt.*

object Version {
  val scala = "3.1.1"
  val zio = "2.0.0-RC2"
}

object Dep {
  val zio = "dev.zio" %% "zio" % Version.zio
  val zioPrelude = "dev.zio" %% "zio-prelude" % "1.0.0-RC10"
  val zioTest = "dev.zio" %% "zio-test" % Version.zio
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zio
//val zioInteropCats = "dev.zio" %% "zio-interop-cats" % "3.1.1.0"

  val discord4j = "com.discord4j" % "discord4j-core" % "3.2.2"

  val commonDeps: Seq[ModuleID] = Seq(zio, zioPrelude)

// use for modules that require zio-test only
  val testDeps: Seq[ModuleID] = Seq(
    zioTest,
    zioTestSbt
  )
}
