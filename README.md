# Example sbt project that compiles using Scala 3

[![Continuous Integration](https://github.com/scala/scala3-example-project/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/scala/scala3-example-project/actions/workflows/ci.yml)

## Usage

This is a normal sbt project. You can compile code with `sbt compile` and run it
with `sbt run`. `sbt console` will start a Scala 3 REPL.

If compiling this example project fails, you probably have a global sbt plugin
that does not work with Scala 3. You might try disabling plugins in
`~/.sbt/1.0/plugins` and `~/.sbt/1.0`.

## Making a new Scala 3 project

The fastest way to start a new Scala 3 project is to use one of the following templates:

* [Minimal Scala 3 project](https://github.com/scala/scala3.g8)
* [Scala 3 project that cross-compiles with Scala 2](https://github.com/scala/scala3-cross.g8)

## Using Scala 3 in an existing project

You will need to make the following adjustments to your build:

### project/build.properties

```
sbt.version=1.6.2
```

You must use sbt 1.5.5 or newer; older versions of sbt are not supported.

### build.sbt

Set up the Scala 3 version:

```scala
scalaVersion := "3.1.1"
```

### Getting your project to compile with Scala 3

For help with porting an existing Scala 2 project to Scala 3, see the
[Scala 3 migration guide](https://scalacenter.github.io/scala-3-migration-guide/).

## Need help?

https://www.scala-lang.org/community/ has links.
