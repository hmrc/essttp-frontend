package services

import connectors.EligibilityStubConnector
import essttp.rootmodel.{TaxId, TaxRegime}
import models.{InvoicePeriod, OverDuePayments, OverduePayment}
import models.ttp.TtpEligibilityData
import moveittocor.corcommon.model.AmountInPence
import services.EligibilityDataService.overDuePayments
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EligibilityDataService @Inject() (connector: EligibilityStubConnector) {

  def data(idType: String, regime: TaxRegime, id: TaxId, showFinancials: Boolean)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OverDuePayments] = for{
    items <- connector.eligibilityData(idType, regime, id, showFinancials)
  } yield (overDuePayments(items))

}

object EligibilityDataService{

  def overDuePayments(ttpData: TtpEligibilityData): OverDuePayments = {
    val qualifyingDebt: AmountInPence = AmountInPence(296345)
    OverDuePayments(
      total = qualifyingDebt,
      payments = List(
        OverduePayment(
          InvoicePeriod(
            monthNumber = 7,
            dueDate = LocalDate.of(2022, 1, 22),
            start = LocalDate.of(2021, 11, 6),
            end = LocalDate.of(2021, 12, 5)),
          amount = AmountInPence((qualifyingDebt.value * 0.4).longValue())),
        OverduePayment(
          InvoicePeriod(
            monthNumber = 8,
            dueDate = LocalDate.of(2021, 12, 22),
            start = LocalDate.of(2021, 10, 6),
            end = LocalDate.of(2021, 11, 5)),
          amount = AmountInPence((qualifyingDebt.value * 0.6).longValue()))))
  }
}
