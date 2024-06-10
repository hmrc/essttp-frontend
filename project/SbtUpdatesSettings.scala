import com.timushev.sbt.updates.UpdatesKeys.dependencyUpdates
import com.timushev.sbt.updates.UpdatesPlugin.autoImport.{dependencyUpdatesFailBuild, dependencyUpdatesFilter, moduleFilterRemoveValue}
import sbt._
import sbt.Keys._

object SbtUpdatesSettings {

  lazy val sbtUpdatesSettings = Seq(
    dependencyUpdatesFailBuild := false, //MAKE THIS TRUE AGAIN WHEN YOU ARE DONE
    (Compile / compile) := ((Compile / compile) dependsOn dependencyUpdates).value,
    dependencyUpdatesFilter -= moduleFilter("org.scala-lang"),
    dependencyUpdatesFilter -= moduleFilter("org.playframework"),
    // higher version of enumeratum breaks with java.lang.ClassCastException -Nov'23
    dependencyUpdatesFilter -= moduleFilter("com.beachape", "enumeratum-play"),
    // locked by version of play
    dependencyUpdatesFilter -= moduleFilter("org.scalatestplus.play", "scalatestplus-play")
  )

}
