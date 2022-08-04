import sbt.Def
import scoverage.ScoverageKeys

object ScoverageSettings {
  lazy val scoverageSettings: Seq[Def.SettingsDefinition] = {
    val excludedFiles = Seq(
      """<empty>""",
      """Reverse.*""",
      """.*.template.*""",
      """.*components.*""",
      """.*Routes.*""",
      """.*Link""",
      """.*SummaryListFluency""",
      """.*TagFluency""",
      """.*InputFluency""",
      """.*JourneyLogger"""
    ).mkString(";") + ";"

    Seq(
      ScoverageKeys.coverageExcludedPackages := """.*.Reverse.*;.*.javascript.*;testOnly.*;.*viewmodels.govuk;.*Reverse.*""",
      ScoverageKeys.coverageExcludedFiles := excludedFiles,
      ScoverageKeys.coverageMinimumStmtTotal := 80,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }
}
