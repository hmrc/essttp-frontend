/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.checkpaymentchedule

import essttp.journey.model.{CanPayWithinSixMonthsAnswers, UpfrontPaymentAnswers, WhyCannotPayInFullAnswers}
import essttp.rootmodel.CannotPayReason
import messages.Messages
import models.Language
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.html.components._

object CheckPaymentScheduleRows {

  def whyCannotPayInFullRow(
      whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers,
      changeLinkCall:            Call
  )(implicit lang: Language): Option[SummaryListRow] = {
      def bullets(reasons: Set[CannotPayReason]) = {
        Html(
          s"""
           |<ul class="govuk-list govuk-list--bullet">
           |${reasons.map(reason => s"""<li>${Messages.WhyCannotPayInFull.checkboxMessage(reason).show}</li>""").mkString("\n")}
           |</ul>
           |""".stripMargin
        )
      }

    val whyCannotPayInFullReasons =
      whyCannotPayInFullAnswers match {
        case WhyCannotPayInFullAnswers.AnswerNotRequired           => None
        case WhyCannotPayInFullAnswers.WhyCannotPayInFull(reasons) => Some(reasons)
      }

    whyCannotPayInFullReasons.map { reasons =>
      SummaryListRow(
        key     = Key(
          content = HtmlContent(Html(Messages.WhyCannotPayInFull.`Why are you unable to pay in full?`.show)),
          classes = "govuk-!-width-one-half"
        ),
        value   = Value(
          content = HtmlContent(bullets(reasons))
        ),
        actions = Some(Actions(
          items = Seq(
            ActionItem(
              href               = changeLinkCall.url,
              content            = Text(Messages.change.show),
              visuallyHiddenText = Some(Messages.WhyCannotPayInFull.`Why are you unable to pay in full?`.show)
            )
          )
        ))
      )
    }
  }

  def upfrontPaymentRows(
      upfrontPaymentAnswers:          UpfrontPaymentAnswers,
      changeCanPayUpfrontCall:        Call,
      changeUpfrontPaymentAmountCall: Call
  )(implicit lang: Language): List[SummaryListRow] = {
    val upfrontPaymentAmount =
      upfrontPaymentAnswers match {
        case UpfrontPaymentAnswers.NoUpfrontPayment               => None
        case UpfrontPaymentAnswers.DeclaredUpfrontPayment(amount) => Some(amount)
      }

    val canPayUpfrontRow =
      SummaryListRow(
        key     = Key(
          content = HtmlContent(Html(Messages.UpfrontPayment.`Can you make an upfront payment?`.show)),
          classes = "govuk-!-width-one-half"
        ),
        value   = Value(
          content = Text(upfrontPaymentAmount.fold(Messages.`No`)(_ => Messages.`Yes`).show)
        ),
        actions = Some(Actions(
          items = Seq(
            ActionItem(
              href               = changeCanPayUpfrontCall.url,
              content            = Text(Messages.change.show),
              visuallyHiddenText = Some(Messages.UpfrontPaymentSummary.`Change Payment`.show)
            )
          )
        ))
      )

    val upfrontPaymentAmountRow = upfrontPaymentAmount.map { amount =>
      SummaryListRow(
        key     = Key(
          content = HtmlContent(Html(Messages.UpfrontPaymentSummary.`Upfront payment`.show)),
          classes = "govuk-!-width-one-half"
        ),
        value   = Value(
          content = Text(amount.value.gdsFormatInPounds)
        ),
        actions = Some(Actions(
          items = Seq(
            ActionItem(
              href               = changeUpfrontPaymentAmountCall.url,
              content            = Text(Messages.change.show),
              visuallyHiddenText = Some(Messages.UpfrontPaymentSummary.`Upfront payment-visually-hidden-message`.show)
            )
          )
        ))
      )
    }

    canPayUpfrontRow :: upfrontPaymentAmountRow.toList
  }

  def canPayWithinSixMonthsRow(
      canPayWithinSixMonthsAnswers: CanPayWithinSixMonthsAnswers,
      changeLinkCall:               Call
  )(implicit lang: Language): Option[SummaryListRow] = {
    val canPayWithinSixMonths =
      canPayWithinSixMonthsAnswers match {
        case CanPayWithinSixMonthsAnswers.AnswerNotRequired             => None
        case CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(answer) => Some(answer)
      }

    canPayWithinSixMonths.map {
      canPay =>
        SummaryListRow(
          key     = Key(
            content = HtmlContent(Html(Messages.CanPayWithinSixMonths.`Can you pay within 6 months?`.show)),
            classes = "govuk-!-width-one-half"
          ),
          value   = Value(
            content = Text((if (canPay) { Messages.`Yes` } else Messages.`No`).show)
          ),
          actions = Some(Actions(
            items = Seq(
              ActionItem(
                href               = changeLinkCall.url,
                content            = Text(Messages.change.show),
                visuallyHiddenText = Some(Messages.CanPayWithinSixMonths.`Can you pay within 6 months?`.show)
              )
            )
          ))
        )
    }
  }

}
