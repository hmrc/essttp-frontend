import play.sbt.routes.RoutesKeys.routes
import sbt._
import sbt.Keys._
import wartremover.Wart
import wartremover.WartRemover.autoImport.{Warts, wartremoverErrors, wartremoverExcluded}

object WartRemoverSettings {

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
        ),
      Compile / doc / wartremoverErrors := Seq(),
    )

}
