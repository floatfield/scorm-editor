ThisBuild / scalaVersion := "2.13.1" // or any other Scala version >= 2.11.12

val zioVersion = "1.0.9"
val zioConfigVersion = "1.0.6"

lazy val bucketsFrontend =
  (project in file("./buckets-frontend"))
    .settings(
      name := "bucketsFrontend",
      // This is an application with a main method
      scalaJSUseMainModuleInitializer := true
    )
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "1.1.0",
        "com.raquo" %%% "laminar" % "0.13.0",
        "com.raquo" %%% "airstream" % "0.13.0"
      )
    )
    .enablePlugins(
      ScalaJSPlugin
    )

lazy val bucketsBackend =
  (project in file("./buckets-backend"))
    .settings(
      name := "bucketsBackend"
    )
    .settings(
      Compile / PB.targets := Seq(
        scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
      ),
      assembly / assemblyJarName := "backend.jar"
    )
    .settings(
      libraryDependencies ++= Seq(
        "io.d11" %% "zhttp" % "1.0.0.0-RC17",
        "dev.zio" %% "zio" % zioVersion,
        "dev.zio" %% "zio-streams" % zioVersion,
        "com.github.jwt-scala" %% "jwt-core" % "8.0.2",
        "org.rocksdb" % "rocksdbjni" % "6.22.1",
        "dev.zio" %% "zio-json" % "0.1.5",
        "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.4" % "compile",
        "dev.zio" %% "zio-config" % zioConfigVersion,
        "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
        "dev.zio" %% "zio-config-yaml" % zioConfigVersion
      )
    )