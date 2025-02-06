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
import essttp.rootmodel.ttp.affordablequotes.{DueDate, PaymentPlan}
import messages.{DateMessages, Message, Messages}
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
              visuallyHiddenText = Some(Messages.PaymentSchedule.`why you are unable to pay in full`.show)
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
                visuallyHiddenText = Some(Messages.PaymentSchedule.`whether you can pay within 6 months`.show)
              )
            )
          ))
        )
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def paymentPlanInstalmentsRows(
      paymentPlan:    PaymentPlan,
      changeLinkCall: Call
  )(implicit lang: Language, ord: Ordering[LocalDate]): List[SummaryListRow] = {
      def row(key: Message, value: String) =
        SummaryListRow(
          key   = Key(content = Text(key.show), classes = "govuk-!-width-one-half"),
          value = Value(content = Text(value))
        )

      def monthAndYear(d: DueDate): String =
        s"${DateMessages.monthName(d.value.getMonthValue).show} ${d.value.getYear.toString}"

    // compiler doesn't deal with :+ and +: very well and can't tell the pattern match is
    // actually exhaustive - @unchecked suppresses that warning
    (paymentPlan.collections.regularCollections.sortBy(_.dueDate.value): @unchecked) match {
      case Nil =>
        List.empty

      case onlyCollection :: Nil =>
        List(
          paymentPlanDurationRow(1, changeLinkCall),
          row(Messages.PaymentSchedule.`Start month`, monthAndYear(onlyCollection.dueDate)),
          row(Messages.PaymentSchedule.Payment, onlyCollection.amountDue.value.gdsFormatInPounds),
          totalToPayRow(paymentPlan)
        )

      case firstCollection :: secondCollection :: Nil =>
        List(
          paymentPlanDurationRow(2, changeLinkCall),
          row(Messages.PaymentSchedule.`Start month`, monthAndYear(firstCollection.dueDate)),
          row(Messages.PaymentSchedule.`First monthly payment`, firstCollection.amountDue.value.gdsFormatInPounds),
          row(Messages.PaymentSchedule.`Final month`, monthAndYear(secondCollection.dueDate)),
          row(Messages.PaymentSchedule.`Final payment`, secondCollection.amountDue.value.gdsFormatInPounds),
          totalToPayRow(paymentPlan)
        )

      case firstCollection +: _ :+ lastCollection =>
        val numberOfCollections = paymentPlan.collections.regularCollections.size

        List(
          paymentPlanDurationRow(numberOfCollections, changeLinkCall),
          row(Messages.PaymentSchedule.`Start month`, monthAndYear(firstCollection.dueDate)),
          row(Messages.PaymentSchedule.`First ... montly payments`(numberOfCollections - 1), firstCollection.amountDue.value.gdsFormatInPounds),
          row(Messages.PaymentSchedule.`Final month`, monthAndYear(lastCollection.dueDate)),
          row(Messages.PaymentSchedule.`Final payment`, lastCollection.amountDue.value.gdsFormatInPounds),
          totalToPayRow(paymentPlan)
        )
    }
  }

  private def totalToPayRow(paymentPlan: PaymentPlan)(implicit lang: Language) =
    SummaryListRow(
      key   = Key(
        content = Text(Messages.PaymentSchedule.`Total to pay`.show),
        classes = "govuk-!-width-one-half"
      ),
      value = Value(
        content = Text(
          s"${paymentPlan.totalDebtIncInt.value.gdsFormatInPounds} ${Messages.PaymentSchedule.`including ... interest`(paymentPlan.planInterest.value).show}"
        )
      )
    )

  private def paymentPlanDurationRow(planDuration: Int, changeLinkCall: Call)(implicit lang: Language) =
    SummaryListRow(
      Key(
        content = Text(Messages.PaymentSchedule.`Payment plan duration`.show),
        classes = "govuk-!-width-one-half"
      ),
      value   = Value(
        content = Text(Messages.PaymentSchedule.`... months`(planDuration).show)
      ),
      actions = Some(Actions(
        items = Seq(
          ActionItem(
            href               = changeLinkCall.url,
            content            = Text(Messages.change.show),
            visuallyHiddenText = Some(Messages.PaymentSchedule.`Change months duration`.show)
          )
        )
      ))
    )

}
