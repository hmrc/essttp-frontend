package testsupport

trait CombinationsHelper {
  private val setOfBooleanOptions = List(Some(true), Some(false), None)

  protected val allCombinationOfTwoBooleanOptions: List[(Option[Boolean], Option[Boolean])] = for {
    a <- setOfBooleanOptions
    b <- setOfBooleanOptions
  } yield (a, b)

}
