import sbt._
import play.core.PlayVersion

object AppDependencies {
  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "5.17.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "1.31.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.10.0-play-28",
    "com.beachape"      %% "enumeratum-play"               % "1.7.0",
    "org.typelevel"     %% "cats-core"                     % "2.7.0",
    "uk.gov.hmrc"       %% "essttp-backend-cor-journey"    % "1.38.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"           %% "scalatest"               % "3.2.7",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.7.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.13.1",
    "com.typesafe.play"       %% "play-test"               % PlayVersion.current,
    "org.scalacheck"          %% "scalacheck"              % "1.15.3",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.35.10", // Required to stay at this version - see https://github.com/scalatest/scalatest/issues/1736
    "com.github.tomakehurst"  %  "wiremock-standalone"     % "2.27.1"
  ).map(_ % Test)
}