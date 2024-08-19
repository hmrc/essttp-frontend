import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "essttp-frontend"

lazy val scalaCompilerOptions = Seq(
  "-Xfatal-warnings",
  "-Xlint:-missing-interpolator,_",
  "-Xlint:adapted-args",
  "-Ypatmat-exhaust-depth:40",
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
  .settings(majorVersion := 0)
  .settings(ThisBuild / useSuperShell:= false)
  .settings(
    scalaVersion := "2.13.13",
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
    (Compile / doc / sources) := Seq.empty
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
  .settings(ScalariformSettings.scalariformSettings *)
  .settings(WartRemoverSettings.wartRemoverSettings *)
  .settings(ScoverageSettings.scoverageSettings *)
  .settings(
    (Compile / doc / scalacOptions) := Seq() //this will allow to have warnings in `doc` task
  )
  .settings(SbtUpdatesSettings.sbtUpdatesSettings *)

//Hint: Uncomment below lines if you want to work on both projects in tandem from intellj
//  .dependsOn(cor)
//  .aggregate(cor)
//lazy val cor = RootProject(file("../essttp-backend"))


