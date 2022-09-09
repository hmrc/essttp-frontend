import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Def
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import scalariform.formatter.preferences._
import wartremover.Wart
import wartremover.WartRemover.autoImport.{wartremoverErrors, wartremoverExcluded}

lazy val appName: String = "essttp-frontend"

lazy val scalariformSettings: Def.SettingsDefinition = {
  // description of options found here -> https://github.com/scala-ide/scalariform
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignArguments, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AllowParamGroupsOnNewlines, true)
    .setPreference(CompactControlReadability, false)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(DanglingCloseParenthesis, Force)
    .setPreference(DoubleIndentConstructorArguments, true)
    .setPreference(DoubleIndentMethodDeclaration, true)
    .setPreference(FirstArgumentOnNewline, Force)
    .setPreference(FirstParameterOnNewline, Force)
    .setPreference(FormatXml, true)
    .setPreference(IndentLocalDefs, true)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentWithTabs, false)
    .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
    .setPreference(NewlineAtEndOfFile, true)
    .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, false)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceBeforeContextColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(SpacesAroundMultiImports, false)
    .setPreference(SpacesWithinPatternBinders, true)
}


lazy val wartRemoverSettings =
  Seq(
    (Compile / compile / wartremoverErrors) ++= Warts.allBut(
      Wart.DefaultArguments,
      Wart.ImplicitConversion,
      Wart.ImplicitParameter,
      Wart.Nothing,
      Wart.Overloading,
      Wart.SizeIs,
      Wart.SortedMaxMinOption,
      Wart.Throw,
      Wart.ToString
    ),
    Test / compile / wartremoverErrors --= Seq(
      Wart.Any,
      Wart.Equals,
      Wart.GlobalExecutionContext,
      Wart.Null,
      Wart.NonUnitStatements,
      Wart.PublicInference
    ),
    wartremoverExcluded ++= (
      (baseDirectory.value ** "*.sc").get ++
        (Compile / routes).value
      )
  )

lazy val scalaCompilerOptions = Seq(
  "-Xfatal-warnings",
  "-Xlint:-missing-interpolator,_",
  "-Yno-adapted-args",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-Ypartial-unification", //required by cats,
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
    scalaVersion := "2.12.15",
    name := appName,
    PlayKeys.playDefaultPort := 9215,
    scalacOptions ++= Seq("-feature"),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    libraryDependencies += ws,
    routesImport ++= Seq("crypto._"),
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
  .settings(scalariformSettings: _*)
  .settings(wartRemoverSettings: _*)
  .settings(ScoverageSettings.scoverageSettings: _*)
  .settings(
    wartremoverExcluded ++= (Compile / routes).value,
    Compile / doc / wartremoverErrors := Seq(),
    Compile / doc / scalacOptions := Seq() //this will allow to have warnings in `doc` task
  )

//Hint: Uncomment below lines if you want to work on both projects in tandem from intellj
//  .dependsOn(cor)
//  .aggregate(cor)
//lazy val cor = RootProject(file("../essttp-backend"))


