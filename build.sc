import mill._
import mill.define.Sources
import mill.scalajslib.ScalaJSModule
import scalalib._

val zioVersion = "1.0.11"
val zioConfigVersion = "1.0.6"
val zioJsonVersion = "0.1.5"
val fastParseVersion = "2.3.3"

//noinspection ScalaFileName
object webApp extends JavaModule { web =>

  trait Shared extends ScalaModule {
    def scalaVersion = "2.13.1"
    def sharedSources = web.millSourcePath / "shared" / "src"
  }

  object frontend extends ScalaJSModule with Shared {

    override def scalaJSVersion = "1.7.0"

    override def sources: Sources = T.sources(
      sharedSources,
      millSourcePath / "src"
    )

    override def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-dom::1.1.0",
      ivy"com.raquo::laminar::0.13.0",
      ivy"com.raquo::airstream::0.13.0",
      ivy"com.raquo::waypoint::0.4.2",
      ivy"dev.zio::zio::$zioVersion",
      ivy"dev.zio::zio-json::$zioJsonVersion",
      ivy"com.lihaoyi::fastparse::$fastParseVersion"
    )
  }

  object backend extends ScalaModule with Shared {

    override def sources: Sources = T.sources(
      sharedSources,
      millSourcePath / "src"
    )

    override def ivyDeps = Agg(
      ivy"com.sksamuel.avro4s::avro4s-core:4.0.10",
      ivy"com.lihaoyi::fastparse:$fastParseVersion",
      ivy"io.d11::zhttp:1.0.0.0-RC17",
      ivy"dev.zio::zio:$zioVersion",
      ivy"dev.zio::zio-streams:$zioVersion",
      ivy"com.github.jwt-scala::jwt-core:9.0.1",
      ivy"org.rocksdb:rocksdbjni:6.22.1",
      ivy"com.lihaoyi::fastparse:$fastParseVersion",
      ivy"dev.zio::zio-config:$zioConfigVersion",
      ivy"dev.zio::zio-config-magnolia:$zioConfigVersion",
      ivy"dev.zio::zio-config-yaml:$zioConfigVersion",
      ivy"dev.zio::zio-cache:0.1.0",
      ivy"org.postgresql:postgresql:42.2.8",
      ivy"io.getquill::quill-jdbc-zio:3.9.0",
      ivy"org.flywaydb:flyway-core:7.12.1",
      ivy"com.lihaoyi::fastparse:$fastParseVersion",
      ivy"dev.zio::zio-json:$zioJsonVersion"
    )
  }

}