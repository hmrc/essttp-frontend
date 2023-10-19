import sbt.*

object AppDependencies {

  val bootstrapVersion = "7.22.0"

  val compile: Seq[ModuleID] = Seq(
    // format: OFF
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"      % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"              % "7.23.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"   % "1.13.0-play-28",
    "com.beachape"      %% "enumeratum-play"                 % "1.7.3",
    "org.typelevel"     %% "cats-core"                       % "2.10.0",
    "uk.gov.hmrc"       %% "essttp-backend-cor-journey"      % "1.117.0",
    "uk.gov.hmrc"       %% "emailaddress"                    % "3.8.0"
  // format: ON
  )

  val test: Seq[ModuleID] = Seq(
    // format: OFF
    "org.scalatest"           %% "scalatest"               % "3.2.17",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.16.1",
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"  % bootstrapVersion,
    "org.scalacheck"          %% "scalacheck"              % "1.17.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.64.6",
    "com.github.tomakehurst"  %  "wiremock-standalone"     % "3.0.1"
  // format: ON
  ).map(_ % Test)
}
