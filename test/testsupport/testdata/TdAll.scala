/*
 * Copyright 2023 HM Revenue & Customs
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

package testsupport.testdata

import actions.EnrolmentDef
import connectors.CallEligibilityApiRequest
import essttp.journey.model.{CorrelationId, JourneyId}
import essttp.rootmodel.bank.{AccountName, AccountNumber, BankDetails, DetailsAboutBankAccount, SortCode, TypeOfBankAccount, TypesOfBankAccount}
import essttp.rootmodel.dates.{InitialPayment, InitialPaymentDate}
import essttp.rootmodel.dates.extremedates.{EarliestPaymentPlanStartDate, ExtremeDatesRequest, ExtremeDatesResponse, LatestPaymentPlanStartDate}
import essttp.rootmodel.dates.startdates.{InstalmentStartDate, PreferredDayOfMonth, StartDatesRequest, StartDatesResponse}
import essttp.rootmodel.ttp.affordablequotes._
import essttp.rootmodel.ttp.eligibility._
import essttp.rootmodel.ttp._
import essttp.rootmodel.ttp.affordability.{InstalmentAmountRequest, InstalmentAmounts}
import essttp.rootmodel.ttp.arrangement._
import essttp.rootmodel.{AmountInPence, CanPayUpfront, DayOfMonth, Email, MonthlyPaymentAmount, TaxRegime, UpfrontPaymentAmount}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

import java.time.{LocalDate, ZoneOffset}
import java.util.UUID

object TdAll {

  val etmpEmail: String = "bobross@joyofpainting.com"

  val expectedServiceNamePayeEn: String = "Set up an Employers’ PAYE payment plan"
  val expectedServiceNameVatEn: String = "Set up a VAT payment plan"
  val expectedServiceNameSaEn: String = "Set up a Self Assessment payment plan"
  val expectedServiceNameGenericEn: String = "Set up a payment plan"

  val expectedServiceNamePayeCy: String = "Trefnu cynllun talu ar gyfer TWE Cyflogwyr"
  val expectedServiceNameVatCy: String = "Trefnu cynllun talu TAW"
  val expectedServiceNameSaCy: String = "Sefydlu cynllun talu ar gyfer Hunanasesiad"
  val expectedServiceNameGenericCy: String = "Trefnu cynllun talu"

  val journeyId: JourneyId = JourneyId("6284fcd33c00003d6b1f3903")
  val correlationId: CorrelationId = CorrelationId(UUID.fromString("8d89a98b-0b26-4ab2-8114-f7c7c81c3059"))

  private val `IR-PAYE-TaxOfficeNumber`: EnrolmentDef = EnrolmentDef(enrolmentKey  = "IR-PAYE", identifierKey = "TaxOfficeNumber")
  private val `IR-PAYE-TaxOfficeReference`: EnrolmentDef = EnrolmentDef(enrolmentKey  = "IR-PAYE", identifierKey = "TaxOfficeReference")
  private val `HMRC-MTD-VAT-Vrn`: EnrolmentDef = EnrolmentDef(enrolmentKey  = "HMRC-MTD-VAT", identifierKey = "VRN")
  private val `IR-SA`: EnrolmentDef = EnrolmentDef(enrolmentKey  = "IR-SA", identifierKey = "UTR")

  val payeEnrolment: Enrolment = Enrolment(
    key               = "IR-PAYE",
    identifiers       = List(
      EnrolmentIdentifier(`IR-PAYE-TaxOfficeNumber`.identifierKey, "864"),
      EnrolmentIdentifier(`IR-PAYE-TaxOfficeReference`.identifierKey, "FZ00049")
    ),
    state             = "Activated",
    delegatedAuthRule = None
  )

  val vatEnrolment: Enrolment = Enrolment(
    key               = "HMRC-MTD-VAT",
    identifiers       = List(
      EnrolmentIdentifier(`HMRC-MTD-VAT-Vrn`.identifierKey, "101747001")
    ),
    state             = "Activated",
    delegatedAuthRule = None
  )

  val saEnrolment: Enrolment = Enrolment(
    key               = "IR-SA",
    identifiers       = List(
      EnrolmentIdentifier(`IR-SA`.identifierKey, "1234567895"),
    ),
    state             = "Activated",
    delegatedAuthRule = None
  )

  val unactivePayeEnrolment: Enrolment = payeEnrolment.copy(state = "Not Activated")

  val canPayUpfront: CanPayUpfront = CanPayUpfront(true)
  val canNotPayUpfront: CanPayUpfront = canPayUpfront.copy(false)

  val amountInPence: AmountInPence = AmountInPence(1000)
  val upfrontPaymentAmount: UpfrontPaymentAmount = UpfrontPaymentAmount(amountInPence)

  val customerPostcode: Postcode = Postcode(SensitiveString("AA11AA"))

  def upfrontPaymentAmount(amount: Long): UpfrontPaymentAmount = UpfrontPaymentAmount(AmountInPence(amount))

  val eligibleEligibilityPass: EligibilityPass = EligibilityPass(true)
  val notEligibleEligibilityPass: EligibilityPass = eligibleEligibilityPass.copy(value = false)
  val eligibleEligibilityRules: EligibilityRules = EligibilityRules(
    hasRlsOnAddress                   = false,
    markedAsInsolvent                 = false,
    isLessThanMinDebtAllowance        = false,
    isMoreThanMaxDebtAllowance        = false,
    disallowedChargeLockTypes         = false,
    existingTTP                       = false,
    chargesOverMaxDebtAge             = None,
    ineligibleChargeTypes             = false,
    missingFiledReturns               = false,
    hasInvalidInterestSignals         = None,
    dmSpecialOfficeProcessingRequired = None,
    noDueDatesReached                 = false,
    cannotFindLockReason              = None,
    creditsNotAllowed                 = None,
    isMoreThanMaxPaymentReference     = None,
    chargesBeforeMaxAccountingDate    = None
  )
  val notEligibleHasRlsOnAddress: EligibilityRules = eligibleEligibilityRules.copy(hasRlsOnAddress = true)
  val notEligibleMarkedAsInsolvent: EligibilityRules = eligibleEligibilityRules.copy(markedAsInsolvent = true)
  val notEligibleIsLessThanMinDebtAllowance: EligibilityRules = eligibleEligibilityRules.copy(isLessThanMinDebtAllowance = true)
  val notEligibleIsMoreThanMaxDebtAllowance: EligibilityRules = eligibleEligibilityRules.copy(isMoreThanMaxDebtAllowance = true)
  val notEligibleDisallowedChargeLockTypes: EligibilityRules = eligibleEligibilityRules.copy(disallowedChargeLockTypes = true)
  val notEligibleExistingTTP: EligibilityRules = eligibleEligibilityRules.copy(existingTTP = true)
  val notEligibleExceedsMaxDebtAge: EligibilityRules = eligibleEligibilityRules.copy(chargesOverMaxDebtAge = Some(true))
  val notEligibleEligibleChargeType: EligibilityRules = eligibleEligibilityRules.copy(ineligibleChargeTypes = true)
  val notEligibleMissingFiledReturns: EligibilityRules = eligibleEligibilityRules.copy(missingFiledReturns = true)
  val notEligibleNoDueDatesReached: EligibilityRules = eligibleEligibilityRules.copy(noDueDatesReached = true)
  val notEligibleHasInvalidInterestSignals: EligibilityRules = eligibleEligibilityRules.copy(hasInvalidInterestSignals = Some(true))
  val notEligibleDmSpecialOfficeProcessingRequired: EligibilityRules = eligibleEligibilityRules.copy(dmSpecialOfficeProcessingRequired = Some(true))
  val notEligibleCannotFindLockReason: EligibilityRules = eligibleEligibilityRules.copy(cannotFindLockReason = Some(true))
  val notEligibleCreditsNotAllowed: EligibilityRules = eligibleEligibilityRules.copy(creditsNotAllowed = Some(true))
  val notEligibleIsMoreThanMaxPaymentReference: EligibilityRules = eligibleEligibilityRules.copy(isMoreThanMaxPaymentReference = Some(true))
  val notEligibleChargesBeforeMaxAccountingDate: EligibilityRules = eligibleEligibilityRules.copy(chargesBeforeMaxAccountingDate = Some(true))
  val notEligibleMultipleReasons: EligibilityRules = eligibleEligibilityRules.copy(missingFiledReturns = true).copy(hasRlsOnAddress = true)

  val callEligibilityApiRequestEpaye: CallEligibilityApiRequest = CallEligibilityApiRequest(
    channelIdentifier         = "eSSTTP",
    idType                    = "EMPREF",
    idValue                   = "864FZ00049",
    regimeType                = "PAYE",
    returnFinancialAssessment = true
  )

  val callEligibilityApiRequestVat: CallEligibilityApiRequest = CallEligibilityApiRequest(
    channelIdentifier         = "eSSTTP",
    idType                    = "VRN",
    idValue                   = "101747001",
    regimeType                = "VATC",
    returnFinancialAssessment = true
  )

  val callEligibilityApiRequestSa: CallEligibilityApiRequest = CallEligibilityApiRequest(
    channelIdentifier         = "eSSTTP",
    idType                    = "UTR",
    idValue                   = "1234567895",
    regimeType                = "SA",
    returnFinancialAssessment = true
  )

  def identification(taxRegime: TaxRegime): List[Identification] = Json.parse(identificationJsonString(taxRegime)).as[List[Identification]]

  def identificationJsonString(taxRegime: TaxRegime): String = taxRegime match {
    case TaxRegime.Epaye =>
      """[
        |    {
        |      "idType": "EMPREF",
        |      "idValue": "864FZ00049"
        |    },
        |    {
        |      "idType": "BROCS",
        |      "idValue": "123PA44545546"
        |    }
        |  ]""".stripMargin

    case TaxRegime.Vat =>
      """[
        |    {
        |      "idType": "VRN",
        |      "idValue": "101747001"
        |    }
        |  ]""".stripMargin

    case TaxRegime.Sa =>
      """[
        |    {
        |      "idType": "UTR",
        |      "idValue": "1234567895"
        |    }
        |  ]""".stripMargin
  }

  def taxDetailJsonString(taxRegime: TaxRegime): String = taxRegime match {
    case TaxRegime.Epaye =>
      """{
        |		"employerRef": "864FZ00049",
        |		"accountsOfficeRef": "123PA44545546"
        |	}""".stripMargin

    case TaxRegime.Vat =>
      """{ "vrn": "101747001" }"""

    case TaxRegime.Sa =>
      """{ "utr": "1234567895" }"""
  }

  def eligibilityCheckResult(
      eligibilityPass:                     EligibilityPass,
      eligibilityRules:                    EligibilityRules,
      taxRegime:                           TaxRegime,
      regimeDigitalCorrespondence:         Option[RegimeDigitalCorrespondence],
      chargeIsInterestBearingCharge:       Option[Boolean]                     = None,
      chargeUseChargeReference:            Option[Boolean]                     = None,
      chargeChargeBeforeMaxAccountingDate: Option[Boolean]                     = None
  ): EligibilityCheckResult = {

    EligibilityCheckResult(
      processingDateTime              = ProcessingDateTime("2022-03-23T13:49:51.141Z"),
      identification                  = identification(taxRegime),
      customerPostcodes               = List(CustomerPostcode(customerPostcode, PostcodeDate("2022-01-31"))),
      regimePaymentFrequency          = PaymentPlanFrequencies.Monthly,
      paymentPlanFrequency            = PaymentPlanFrequencies.Monthly,
      paymentPlanMinLength            = PaymentPlanMinLength(1),
      paymentPlanMaxLength            = PaymentPlanMaxLength(12),
      eligibilityStatus               = EligibilityStatus(eligibilityPass),
      eligibilityRules                = eligibilityRules,
      chargeTypeAssessment            = List(ChargeTypeAssessment(
        taxPeriodFrom   = TaxPeriodFrom("2020-08-13"),
        taxPeriodTo     = TaxPeriodTo("2020-08-14"),
        debtTotalAmount = DebtTotalAmount(AmountInPence(300000)),
        charges         = List(Charges(
          chargeType                    = ChargeType("InYearRTICharge-Tax"),
          mainType                      = MainType("InYearRTICharge(FPS)"),
          chargeReference               = ChargeReference("A00000000001"),
          mainTrans                     = MainTrans("mainTrans"),
          subTrans                      = SubTrans("subTrans"),
          outstandingAmount             = OutstandingAmount(AmountInPence(100000)),
          interestStartDate             = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
          dueDate                       = DueDate(LocalDate.parse("2017-03-07")),
          accruedInterest               = AccruedInterest(AmountInPence(1597)),
          ineligibleChargeType          = IneligibleChargeType(false),
          chargeOverMaxDebtAge          = Some(ChargeOverMaxDebtAge(false)),
          locks                         = Some(
            List(Lock(LockType("Payment"), LockReason("Risk/Fraud"), DisallowedChargeLockType(false)))
          ),
          dueDateNotReached             = false,
          isInterestBearingCharge       = chargeIsInterestBearingCharge.map(IsInterestBearingCharge(_)),
          useChargeReference            = chargeUseChargeReference.map(UseChargeReference(_)),
          chargeBeforeMaxAccountingDate = chargeChargeBeforeMaxAccountingDate.map(ChargeBeforeMaxAccountingDate(_))
        ))
      )),
      customerDetails                 = None,
      regimeDigitalCorrespondence     = regimeDigitalCorrespondence,
      futureChargeLiabilitiesExcluded = false
    )
  }

  def dayOfMonth(day: Int = 28): DayOfMonth = DayOfMonth(day)

  def extremeDatesRequest(initialPayment: Boolean): ExtremeDatesRequest = ExtremeDatesRequest(InitialPayment(initialPayment))

  def extremeDatesResponse(): ExtremeDatesResponse = ExtremeDatesResponse(
    Some(InitialPaymentDate(LocalDate.parse("2022-06-24"))),
    earliestPlanStartDate = EarliestPaymentPlanStartDate(LocalDate.parse("2022-07-14")),
    latestPlanStartDate   = LatestPaymentPlanStartDate(LocalDate.parse("2022-08-13"))
  )

  def startDatesRequest(initialPayment: Boolean, day: Int): StartDatesRequest = StartDatesRequest(InitialPayment(initialPayment), PreferredDayOfMonth(day))

  def startDatesResponse(): StartDatesResponse = StartDatesResponse(
    initialPaymentDate  = Some(InitialPaymentDate(LocalDate.parse("2022-07-03"))),
    instalmentStartDate = InstalmentStartDate(LocalDate.parse("2022-07-28"))
  )

  val instalmentAmountRequest: InstalmentAmountRequest = InstalmentAmountRequest(
    channelIdentifier            = ChannelIdentifiers.eSSTTP,
    paymentPlanFrequency         = PaymentPlanFrequencies.Monthly,
    paymentPlanMinLength         = PaymentPlanMinLength(1),
    paymentPlanMaxLength         = PaymentPlanMaxLength(12),
    earliestPaymentPlanStartDate = EarliestPaymentPlanStartDate(LocalDate.parse("2022-07-14")),
    latestPaymentPlanStartDate   = LatestPaymentPlanStartDate(LocalDate.parse("2022-08-13")),
    initialPaymentDate           = Some(InitialPaymentDate(LocalDate.parse("2022-06-24"))),
    initialPaymentAmount         = Some(AmountInPence(200)),
    accruedDebtInterest          = AccruedDebtInterest(AmountInPence(3194)),
    debtItemCharges              = List(
      DebtItemCharge(
        OutstandingDebtAmount(AmountInPence(50000)),
        mainTrans               = MainTrans("mainTrans"),
        subTrans                = SubTrans("subTrans"),
        isInterestBearingCharge = Some(IsInterestBearingCharge(true)),
        useChargeReference      = Some(UseChargeReference(true)),
        debtItemChargeId        = ChargeReference("A00000000001"),
        interestStartDate       = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
        debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-03-07"))
      ),
      DebtItemCharge(
        OutstandingDebtAmount(AmountInPence(100000)),
        mainTrans               = MainTrans("mainTrans"),
        subTrans                = SubTrans("subTrans"),
        isInterestBearingCharge = Some(IsInterestBearingCharge(true)),
        useChargeReference      = Some(UseChargeReference(true)),
        debtItemChargeId        = ChargeReference("A00000000002"),
        interestStartDate       = Some(InterestStartDate(LocalDate.parse("2017-02-07"))),
        debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-02-07"))
      )
    ),
    customerPostcodes            = List(CustomerPostcode(customerPostcode, PostcodeDate("2022-01-31")))
  )

  def affordableQuotesRequest(taxRegime: TaxRegime): AffordableQuotesRequest = {
    val expectedPaymentPlanMaxLength = taxRegime match {
      case TaxRegime.Epaye => PaymentPlanMaxLength(12)
      case TaxRegime.Vat   => PaymentPlanMaxLength(12)
      case TaxRegime.Sa    => PaymentPlanMaxLength(12)
    }
    AffordableQuotesRequest(
      channelIdentifier           = ChannelIdentifiers.eSSTTP,
      paymentPlanFrequency        = PaymentPlanFrequencies.Monthly,
      paymentPlanMinLength        = PaymentPlanMinLength(1),
      paymentPlanMaxLength        = expectedPaymentPlanMaxLength,
      initialPaymentDate          = Some(InitialPaymentDate(LocalDate.parse("2022-07-03"))),
      initialPaymentAmount        = Some(UpfrontPaymentAmount(AmountInPence(200))),
      accruedDebtInterest         = AccruedDebtInterest(AmountInPence(3194)),
      debtItemCharges             = List(
        DebtItemCharge(
          OutstandingDebtAmount(AmountInPence(50000)),
          mainTrans               = MainTrans("mainTrans"),
          subTrans                = SubTrans("subTrans"),
          isInterestBearingCharge = Some(IsInterestBearingCharge(true)),
          useChargeReference      = Some(UseChargeReference(true)),
          debtItemChargeId        = ChargeReference("A00000000001"),
          interestStartDate       = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
          debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-03-07"))
        ),
        DebtItemCharge(
          OutstandingDebtAmount(AmountInPence(100000)),
          mainTrans               = MainTrans("mainTrans"),
          subTrans                = SubTrans("subTrans"),
          isInterestBearingCharge = Some(IsInterestBearingCharge(true)),
          useChargeReference      = Some(UseChargeReference(true)),
          debtItemChargeId        = ChargeReference("A00000000002"),
          interestStartDate       = Some(InterestStartDate(LocalDate.parse("2017-02-07"))),
          debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-02-07"))
        )
      ),
      customerPostcodes           = List(CustomerPostcode(Postcode(SensitiveString("AA11AA")), PostcodeDate("2022-01-31"))),
      paymentPlanAffordableAmount = PaymentPlanAffordableAmount(AmountInPence(30000)),
      paymentPlanStartDate        = InstalmentStartDate(LocalDate.parse("2022-07-28"))
    )
  }

  val instalmentAmounts: InstalmentAmounts = InstalmentAmounts(AmountInPence(33333), AmountInPence(100000))
  val monthlyPaymentAmount: MonthlyPaymentAmount = MonthlyPaymentAmount(AmountInPence(30000))

  def paymentPlan(numberOfInstalments: Int, amountDue: AmountDue): PaymentPlan = PaymentPlan(
    numberOfInstalments = NumberOfInstalments(numberOfInstalments),
    planDuration        = PlanDuration(numberOfInstalments),
    totalDebt           = TotalDebt(AmountInPence(amountInPence.value * numberOfInstalments)),
    totalDebtIncInt     = TotalDebtIncludingInterest(amountInPence.+(amountInPence)),
    planInterest        = PlanInterest(amountInPence),
    collections         = Collection(
      initialCollection  = Some(InitialCollection(dueDate   = DueDate(LocalDate.parse("2022-02-01")), amountDue = AmountDue(amountInPence))),
      regularCollections = List(RegularCollection(dueDate   = DueDate(LocalDate.parse("2022-02-01")), amountDue = amountDue))
    ),
    instalments         = List(Instalment(
      instalmentNumber          = InstalmentNumber(numberOfInstalments),
      dueDate                   = DueDate(LocalDate.parse("2022-02-01")),
      instalmentInterestAccrued = InterestAccrued(amountInPence),
      instalmentBalance         = InstalmentBalance(amountInPence),
      debtItemChargeId          = ChargeReference("testchargeid"),
      amountDue                 = AmountDue(amountInPence),
      debtItemOriginalDueDate   = DebtItemOriginalDueDate(LocalDate.parse("2022-01-01"))
    ))
  )

  def typeOfBankAccount(typeOfAccount: String): TypeOfBankAccount =
    if (typeOfAccount == "Business") TypesOfBankAccount.Business else TypesOfBankAccount.Personal

  def detailsAboutBankAccount(typeOfAccount: String, isAccountHolder: Boolean): DetailsAboutBankAccount =
    DetailsAboutBankAccount(typeOfBankAccount(typeOfAccount), isAccountHolder)

  def directDebitDetails(name: String, sortCode: String, accountNumber: String): BankDetails =
    BankDetails(AccountName(SensitiveString(name)), SortCode(SensitiveString(sortCode)), AccountNumber(SensitiveString(accountNumber)))

  //string including & ' / which are the allowed symbols according to IF
  val testAccountName = "Mr. Bob Ross &'/ With Symbols"

  def arrangementRequest(
      customerDetails:             Option[List[CustomerDetail]],
      regimeDigitalCorrespondence: Option[RegimeDigitalCorrespondence],
      taxRegime:                   TaxRegime,
      accountNumber:               String                              = "12345678"
  ): ArrangementRequest = {
    val regimeType = taxRegime match {
      case TaxRegime.Epaye => RegimeType("PAYE")
      case TaxRegime.Vat   => RegimeType("VATC")
      case TaxRegime.Sa    => throw new NotImplementedError()
    }
    ArrangementRequest(
      channelIdentifier           = ChannelIdentifiers.eSSTTP,
      regimeType                  = regimeType,
      regimePaymentFrequency      = PaymentPlanFrequencies.Monthly,
      arrangementAgreedDate       = ArrangementAgreedDate(LocalDate.now(ZoneOffset.of("Z")).toString),
      identification              = identification(taxRegime),
      directDebitInstruction      = DirectDebitInstruction(
        sortCode        = SortCode(SensitiveString("123456")),
        accountNumber   = AccountNumber(SensitiveString(accountNumber)),
        accountName     = AccountName(SensitiveString(testAccountName)),
        paperAuddisFlag = PaperAuddisFlag(false)
      ),
      paymentPlan                 = EnactPaymentPlan(
        planDuration         = PlanDuration(2),
        paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
        numberOfInstalments  = NumberOfInstalments(2),
        totalDebt            = TotalDebt(AmountInPence(111141)),
        totalDebtIncInt      = TotalDebtIncludingInterest(AmountInPence(111147)),
        planInterest         = PlanInterest(AmountInPence(6)),
        collections          = Collection(
          initialCollection  = Some(InitialCollection(dueDate   = DueDate(LocalDate.parse("2022-07-03")), amountDue = AmountDue(AmountInPence(12312)))),
          regularCollections = List(
            RegularCollection(dueDate   = DueDate(LocalDate.parse("2022-09-28")), amountDue = AmountDue(AmountInPence(55573))),
            RegularCollection(dueDate   = DueDate(LocalDate.parse("2022-08-28")), amountDue = AmountDue(AmountInPence(55573)))
          )
        ),
        instalments          = List(
          Instalment(
            instalmentNumber          = InstalmentNumber(2),
            dueDate                   = DueDate(LocalDate.parse("2022-09-28")),
            instalmentInterestAccrued = InterestAccrued(AmountInPence(3)),
            instalmentBalance         = InstalmentBalance(AmountInPence(55571)),
            debtItemChargeId          = ChargeReference("A00000000001"),
            amountDue                 = AmountDue(AmountInPence(55570)),
            debtItemOriginalDueDate   = DebtItemOriginalDueDate(LocalDate.parse("2021-07-28"))
          ),
          Instalment(
            instalmentNumber          = InstalmentNumber(1),
            dueDate                   = DueDate(LocalDate.parse("2022-08-28")),
            instalmentInterestAccrued = InterestAccrued(AmountInPence(3)),
            instalmentBalance         = InstalmentBalance(AmountInPence(111141)),
            debtItemChargeId          = ChargeReference("A00000000001"),
            amountDue                 = AmountDue(AmountInPence(55570)),
            debtItemOriginalDueDate   = DebtItemOriginalDueDate(LocalDate.parse("2021-07-28"))
          )
        ),
        debtItemCharges      = List(
          DebtItemCharges(
            outstandingDebtAmount   = OutstandingDebtAmount(AmountInPence(50000)),
            debtItemChargeId        = ChargeReference("A00000000001"),
            debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-03-07")),
            accruedInterest         = AccruedInterest(AmountInPence(1597)),
            isInterestBearingCharge = Some(IsInterestBearingCharge(true)),
            useChargeReference      = Some(UseChargeReference(true))
          ),
          DebtItemCharges(
            outstandingDebtAmount   = OutstandingDebtAmount(AmountInPence(100000)),
            debtItemChargeId        = ChargeReference("A00000000002"),
            debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-02-07")),
            accruedInterest         = AccruedInterest(AmountInPence(1597)),
            isInterestBearingCharge = Some(IsInterestBearingCharge(true)),
            useChargeReference      = Some(UseChargeReference(true))
          )
        )
      ),
      customerDetails             = customerDetails,
      regimeDigitalCorrespondence = regimeDigitalCorrespondence
    )
  }

  def customerDetail(email: String = "bobross@joyofpainting.com", source: EmailSource = EmailSource.ETMP): Option[List[CustomerDetail]] =
    Some(List(CustomerDetail(Some(Email(SensitiveString(email))), Some(source))))

  val someRegimeDigitalCorrespondenceFalse: Option[RegimeDigitalCorrespondence] = Some(RegimeDigitalCorrespondence(false))
  val someRegimeDigitalCorrespondenceTrue: Option[RegimeDigitalCorrespondence] = Some(RegimeDigitalCorrespondence(true))

  def customerReference(taxRegime: TaxRegime) = taxRegime match {
    case TaxRegime.Epaye => CustomerReference("123PA44545546")
    case TaxRegime.Vat   => CustomerReference("101747001")
    case TaxRegime.Sa    => CustomerReference("1234567895")
  }

  def arrangementResponse(taxRegime: TaxRegime): ArrangementResponse =
    ArrangementResponse(ProcessingDateTime("2022-03-23T13:49:51.141Z"), customerReference(taxRegime))

  def taxDetailForAuditEvent(taxRegime: TaxRegime): String = taxRegime match {
    case TaxRegime.Epaye => """"employerRef": "864FZ00049", "accountsOfficeRef": "123PA44545546""""
    case TaxRegime.Vat   => """"vrn": "101747001""""
    case TaxRegime.Sa    => """"utr": "1234567895""""
  }
}
