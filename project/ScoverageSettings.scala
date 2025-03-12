import sbt.Def
import scoverage.ScoverageKeys

object ScoverageSettings {
  lazy val scoverageSettings: Seq[Def.SettingsDefinition] = {
    val excludedFiles = Seq(
      """<empty>""",
      """Module.*""",
      """Reverse.*""",
      """.*.template.*""",
      """.*components.*""",
      """.*Routes.*""",
      """.*Link""",
      """.*SummaryListFluency""",
      """.*TagFluency""",
      """.*InputFluency""",
      """.*JourneyLogger""",
      """.*ClockProvider.*"""
    ).mkString(";") + ";"

    Seq(
      ScoverageKeys.coverageExcludedPackages := """.*.Reverse.*;.*.javascript.*;testOnly.*;.*viewmodels.govuk;.*Reverse.*;<empty>;.*\$anon.*""",
      ScoverageKeys.coverageExcludedFiles := excludedFiles,
      ScoverageKeys.coverageMinimumStmtTotal := 90,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }
}
