package testsupport.reusableassertions

import org.jsoup.select.Elements
import testsupport.RichMatchers

import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter

object ContentAssertions extends RichMatchers {

  def assertListOfContent(elements: Elements)(expectedContent: List[String]) = {
    elements.asScala.toList.zip(expectedContent)
      .map { case (element, expectedText) => element.text() shouldBe expectedText }
  }
}
