import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "essttp-frontend"

lazy val scalaCompilerOptions = Seq(
//  "-Xfatal-warnings",
  "-Wvalue-discard",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-language:strictEquality",
  // required in place of silencer plugin
  "-Wconf:msg=unused-imports&src=html/.*:s",
  "-Wconf:src=routes/.*:s"
)

lazy val root = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(majorVersion := 1)
  .settings(ThisBuild / useSuperShell:= false)
  .settings(
    scalaVersion := "3.5.1",
    name := appName,
    PlayKeys.playDefaultPort := 9215,
    scalacOptions ++= Seq("-feature"),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    libraryDependencies += ws,
    retrieveManaged := false,
    (update / evictionWarningOptions) :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    pipelineStages := Seq(digest),
    scalacOptions ++= scalaCompilerOptions,
    (Compile / doc / sources) := Seq.empty,
    routesImport ++= Seq("essttp.rootmodel.TaxRegime", "models.Language"),
    scalafmtOnCompile := true
  )
  .settings(
    commands += Command.command("runTestOnly") { state =>
      state.globalLogging.full.info("running play using 'testOnlyDoNotUseInAppConf' routes...")
      s"""set javaOptions += "-Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"""" ::
        "run" ::
        s"""set javaOptions -= "-Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"""" ::
        state
    }
  )
  .settings(TwirlKeys.templateImports := Seq.empty)
  .settings(WartRemoverSettings.wartRemoverSettings *)
  .settings(ScoverageSettings.scoverageSettings *)
  .settings(
    (Compile / doc / scalacOptions) := Seq() //this will allow to have warnings in `doc` task
  )
  .settings(SbtUpdatesSettings.sbtUpdatesSettings *)



