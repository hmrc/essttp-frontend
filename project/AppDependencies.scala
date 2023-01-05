import sbt._
import play.core.PlayVersion

object AppDependencies {
  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "7.12.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "5.5.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.12.0-play-28",
    "com.beachape"      %% "enumeratum-play"               % "1.7.2",
    "org.typelevel"     %% "cats-core"                     % "2.9.0",
    "uk.gov.hmrc"       %% "essttp-backend-cor-journey"    % "1.97.0",
    "uk.gov.hmrc"       %% "emailaddress"                  % "3.7.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"           %% "scalatest"               % "3.2.14",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.15.3",
    "com.typesafe.play"       %% "play-test"               % PlayVersion.current,
    "org.scalacheck"          %% "scalacheck"              % "1.17.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.62.2",
    "com.github.tomakehurst"  %  "wiremock-standalone"     % "2.27.2"
  ).map(_ % Test)
}
