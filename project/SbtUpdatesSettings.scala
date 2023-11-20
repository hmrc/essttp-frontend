import com.timushev.sbt.updates.UpdatesKeys.dependencyUpdates
import com.timushev.sbt.updates.UpdatesPlugin.autoImport.{dependencyUpdatesFailBuild, dependencyUpdatesFilter, moduleFilterRemoveValue}
import sbt._
import sbt.Keys._

object SbtUpdatesSettings {

  lazy val sbtUpdatesSettings = Seq(
    dependencyUpdatesFailBuild := true,
    (Compile / compile) := ((Compile / compile) dependsOn dependencyUpdates).value,
    dependencyUpdatesFilter -= moduleFilter("org.scala-lang"),
    dependencyUpdatesFilter -= moduleFilter("com.typesafe.play"),
    // higher version of enumeratum breaks with java.lang.ClassCastException -Nov'23
    dependencyUpdatesFilter -= moduleFilter("com.beachape", "enumeratum-play"),
    // locked by version of play
    dependencyUpdatesFilter -= moduleFilter("org.scalatestplus.play", "scalatestplus-play"),
    dependencyUpdatesFilter -= moduleFilter("uk.gov.hmrc", "essttp-backend-cor-journey")
  )

}
