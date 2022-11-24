import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "essttp-frontend"

lazy val scalaCompilerOptions = Seq(
  "-Xfatal-warnings",
  "-Xlint:-missing-interpolator,_",
  "-Xlint:adapted-args",
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:params",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  // required in place of silencer plugin
  "-Wconf:cat=unused-imports&src=html/.*:s",
  "-Wconf:src=routes/.*:s"
)

lazy val root = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(DefaultBuildSettings.scalaSettings: _*)
  .settings(DefaultBuildSettings.defaultSettings(): _*)
  .settings(SbtDistributablesPlugin.publishingSettings: _*)
  .settings(majorVersion := 0)
  .settings(ThisBuild / useSuperShell:= false)
  .settings(
    scalaVersion := "2.13.8",
    name := appName,
    PlayKeys.playDefaultPort := 9215,
    scalacOptions ++= Seq("-feature"),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    libraryDependencies += ws,
    retrieveManaged := false,
    update / evictionWarningOptions :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
   pipelineStages := Seq(digest),
    scalacOptions ++= scalaCompilerOptions
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
  .settings(ScalariformSettings.scalariformSettings: _*)
  .settings(WartRemoverSettings.wartRemoverSettings: _*)
  .settings(ScoverageSettings.scoverageSettings: _*)
  .settings(
    Compile / doc / scalacOptions := Seq() //this will allow to have warnings in `doc` task
  )
  .settings(SbtUpdatesSettings.sbtUpdatesSettings: _*)

//Hint: Uncomment below lines if you want to work on both projects in tandem from intellj
//  .dependsOn(cor)
//  .aggregate(cor)
//lazy val cor = RootProject(file("../essttp-backend"))


