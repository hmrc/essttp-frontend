import sbt.*

object AppDependencies {

  val bootstrapVersion = "9.1.0"

  val compile: Seq[ModuleID] = Seq(
    // format: OFF
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"              % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"              % "10.5.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30"   % "3.1.0",
    "com.beachape"      %% "enumeratum-play"                         % "1.8.0",
    "org.typelevel"     %% "cats-core"                               % "2.12.0",
    "uk.gov.hmrc"       %% "essttp-backend-cor-journey"              % "1.143.0",
    "uk.gov.hmrc"       %% "domain-play-30"                          % "10.0.0"
  // format: ON
  )

  val test: Seq[ModuleID] = Seq(
    // format: OFF
    "org.scalatest"           %% "scalatest"               % "3.2.19",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "7.0.1",
    "org.jsoup"               %  "jsoup"                   % "1.18.1",
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.scalacheck"          %% "scalacheck"              % "1.18.0",
    "org.wiremock"            %  "wiremock-standalone"     % "3.9.0"
  // format: ON
  ).map(_ % Test)
}
