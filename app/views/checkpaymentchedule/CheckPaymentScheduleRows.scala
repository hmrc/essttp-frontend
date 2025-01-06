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

import cats.syntax.eq._
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, UpfrontPaymentAnswers, WhyCannotPayInFullAnswers}
import essttp.rootmodel.CannotPayReason
import essttp.rootmodel.ttp.affordablequotes.PaymentPlan
import messages.{DateMessages, Messages}
import models.Language
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.html.components._

import java.time.LocalDate

object CheckPaymentScheduleRows {

  def whyCannotPayInFullRow(
      whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers,
      changeLinkCall:            Call
  )(implicit lang: Language): Option[SummaryListRow] = {

      def formatWhyCannotPayAnswers(reasons: Set[CannotPayReason]) = {
        reasons.toList match {
          case oneReason :: Nil => Html(s"${Messages.WhyCannotPayInFull.checkboxMessageWithHint(oneReason)._1.show}")
          case _ => Html(
            s"""
           |<ul class="govuk-list govuk-list--bullet">
           |${reasons.map(reason => s"""<li>${Messages.WhyCannotPayInFull.checkboxMessageWithHint(reason)._1.show}</li>""").mkString("\n")}
           |</ul>
           |""".stripMargin
          )
        }
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
          content = HtmlContent(formatWhyCannotPayAnswers(reasons))
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

  def paymentPlanMonthRows(paymentPlan: PaymentPlan, changeLinkCall: Call)(implicit lang: Language, ord: Ordering[LocalDate]): List[SummaryListRow] = {
    paymentPlan.collections.regularCollections.sortBy(_.dueDate.value).zipWithIndex.map {
      case (p, index) =>
        SummaryListRow(
          classes = s"grouped-row ${
            if (index > (paymentPlan.collections.regularCollections.size - 2)) { "last-grouped-row" } else { "" }
          }",
          key     = Key(
            content = Text(s"${DateMessages.monthName(p.dueDate.value.getMonthValue).show} ${p.dueDate.value.getYear.toString}"),
            classes = s"govuk-!-width-one-half${if (index === 0) " govuk-!-padding-top-2" else ""}"
          ),
          value   = Value(
            content = Text(p.amountDue.value.gdsFormatInPounds)
          ),
          actions = Some(Actions(
            items = Seq(
              ActionItem(
                href               = changeLinkCall.url,
                classes            = s"${if (index === 0) "" else "grouped-change"}",
                content            = Text(Messages.change.show),
                visuallyHiddenText = Some(Messages.PaymentSchedule.`Change months duration`.show)
              )
            )
          ))
        )
    }
  }

  def paymentplanTotalRow(paymentPlan: PaymentPlan)(implicit lang: Language): SummaryListRow =
    SummaryListRow(
      key     = Key(
        content = Text(Messages.PaymentSchedule.`Total to pay`.show),
        classes = "govuk-!-width-one-half"
      ),
      value   = Value(
        content = Text(paymentPlan.totalDebtIncInt.value.gdsFormatInPounds)
      ),
      actions = None
    )

}
