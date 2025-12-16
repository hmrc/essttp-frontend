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

import connectors.CallEligibilityApiRequest
import essttp.enrolments.EnrolmentDef
import essttp.journey.model.{CorrelationId, JourneyId}
import essttp.rootmodel.bank.*
import essttp.rootmodel.dates.extremedates.{EarliestPaymentPlanStartDate, ExtremeDatesRequest, ExtremeDatesResponse, LatestPaymentPlanStartDate}
import essttp.rootmodel.dates.startdates.{InstalmentStartDate, PreferredDayOfMonth, StartDatesRequest, StartDatesResponse}
import essttp.rootmodel.dates.{InitialPayment, InitialPaymentDate}
import essttp.rootmodel.pega.{GetCaseResponse, PegaCaseId, StartCaseResponse}
import essttp.rootmodel.ttp.*
import essttp.rootmodel.ttp.affordability.{InstalmentAmountRequest, InstalmentAmounts}
import essttp.rootmodel.ttp.affordablequotes.*
import essttp.rootmodel.ttp.arrangement.*
import essttp.rootmodel.ttp.eligibility.*
import essttp.rootmodel.*
import essttp.rootmodel.TaxRegime.Sa
import essttp.rootmodel.ttp.CustomerTypes.MTDITSA
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

import java.time.{LocalDate, ZoneOffset}
import java.util.UUID

object TdAll {

  val etmpEmail: String = "bobross@joyofpainting.com"

  val expectedServiceNamePayeEn: String    = "Set up an Employers’ PAYE payment plan"
  val expectedServiceNameVatEn: String     = "Set up a VAT payment plan"
  val expectedServiceNameSaEn: String      = "Set up a Self Assessment payment plan"
  val expectedServiceNameSimpEn: String    = "Set up a Simple Assessment payment plan"
  val expectedServiceNameGenericEn: String = "Set up a payment plan"

  val expectedServiceNamePayeCy: String    = "Trefnu cynllun talu ar gyfer TWE Cyflogwyr"
  val expectedServiceNameVatCy: String     = "Trefnu cynllun talu TAW"
  val expectedServiceNameSaCy: String      = "Sefydlu cynllun talu ar gyfer Hunanasesiad"
  val expectedServiceNameSimpCy: String    = "Sefydlu cynllun talu ar gyfer Asesiad Syml"
  val expectedServiceNameGenericCy: String = "Trefnu cynllun talu"

  val journeyId: JourneyId         = JourneyId("6284fcd33c00003d6b1f3903")
  val correlationId: CorrelationId = CorrelationId(UUID.fromString("8d89a98b-0b26-4ab2-8114-f7c7c81c3059"))

  private val `IR-PAYE-TaxOfficeNumber`: EnrolmentDef    =
    EnrolmentDef(enrolmentKey = "IR-PAYE", identifierKey = "TaxOfficeNumber")
  private val `IR-PAYE-TaxOfficeReference`: EnrolmentDef =
    EnrolmentDef(enrolmentKey = "IR-PAYE", identifierKey = "TaxOfficeReference")
  private val `HMRC-MTD-VAT-Vrn`: EnrolmentDef           = EnrolmentDef(enrolmentKey = "HMRC-MTD-VAT", identifierKey = "VRN")
  private val `IR-SA`: EnrolmentDef                      = EnrolmentDef(enrolmentKey = "IR-SA", identifierKey = "UTR")
  private val `HMRC-MTD-IT`: EnrolmentDef                = EnrolmentDef(enrolmentKey = "HMRC-MTD-IT", identifierKey = "MTDITID")

  val payeEnrolment: Enrolment = Enrolment(
    key = "IR-PAYE",
    identifiers = List(
      EnrolmentIdentifier(`IR-PAYE-TaxOfficeNumber`.identifierKey, "864"),
      EnrolmentIdentifier(`IR-PAYE-TaxOfficeReference`.identifierKey, "FZ00049")
    ),
    state = "Activated",
    delegatedAuthRule = None
  )

  val vatEnrolment: Enrolment = Enrolment(
    key = "HMRC-MTD-VAT",
    identifiers = List(
      EnrolmentIdentifier(`HMRC-MTD-VAT-Vrn`.identifierKey, "101747001")
    ),
    state = "Activated",
    delegatedAuthRule = None
  )

  val saEnrolment: Enrolment = Enrolment(
    key = "IR-SA",
    identifiers = List(
      EnrolmentIdentifier(`IR-SA`.identifierKey, "1234567895")
    ),
    state = "Activated",
    delegatedAuthRule = None
  )

  val mtdEnrolment: Enrolment = Enrolment(
    key = "HMRC-MTD-IT",
    identifiers = List(
      EnrolmentIdentifier(`HMRC-MTD-IT`.identifierKey, "1234567895")
    ),
    state = "Activated",
    delegatedAuthRule = None
  )

  val unactivePayeEnrolment: Enrolment = payeEnrolment.copy(state = "Not Activated")

  val canPayUpfront: CanPayUpfront    = CanPayUpfront(value = true)
  val canNotPayUpfront: CanPayUpfront = canPayUpfront.copy(false)

  val amountInPence: AmountInPence               = AmountInPence(1000)
  val upfrontPaymentAmount: UpfrontPaymentAmount = UpfrontPaymentAmount(amountInPence)

  val customerPostcode: Postcode = Postcode(SensitiveString("AA11AA"))

  def upfrontPaymentAmount(amount: Long): UpfrontPaymentAmount = UpfrontPaymentAmount(AmountInPence(amount))

  val eligibleEligibilityPass: EligibilityPass     = EligibilityPass(value = true)
  val notEligibleEligibilityPass: EligibilityPass  = eligibleEligibilityPass.copy(value = false)
  val eligibleEligibilityRules: EligibilityRules   = EligibilityRules(
    hasRlsOnAddress = false,
    markedAsInsolvent = false,
    isLessThanMinDebtAllowance = false,
    isMoreThanMaxDebtAllowance = false,
    disallowedChargeLockTypes = false,
    existingTTP = false,
    chargesOverMaxDebtAge = None,
    ineligibleChargeTypes = false,
    missingFiledReturns = false,
    hasInvalidInterestSignals = None,
    hasInvalidInterestSignalsCESA = None,
    dmSpecialOfficeProcessingRequired = None,
    noDueDatesReached = false,
    cannotFindLockReason = None,
    creditsNotAllowed = None,
    isMoreThanMaxPaymentReference = None,
    chargesBeforeMaxAccountingDate = None,
    hasDisguisedRemuneration = None,
    hasCapacitor = None,
    dmSpecialOfficeProcessingRequiredCDCS = None,
    isAnMtdCustomer = None,
    dmSpecialOfficeProcessingRequiredCESA = None,
    noMtditsaEnrollment = None
  )
  val notEligibleHasRlsOnAddress: EligibilityRules =
    eligibleEligibilityRules.copy(hasRlsOnAddress = true)

  val notEligibleMarkedAsInsolvent: EligibilityRules =
    eligibleEligibilityRules.copy(markedAsInsolvent = true)

  val notEligibleIsLessThanMinDebtAllowance: EligibilityRules =
    eligibleEligibilityRules.copy(isLessThanMinDebtAllowance = true)

  val notEligibleIsMoreThanMaxDebtAllowance: EligibilityRules =
    eligibleEligibilityRules.copy(isMoreThanMaxDebtAllowance = true)

  val notEligibleDisallowedChargeLockTypes: EligibilityRules =
    eligibleEligibilityRules.copy(disallowedChargeLockTypes = true)

  val notEligibleExistingTTP: EligibilityRules =
    eligibleEligibilityRules.copy(existingTTP = true)

  val notEligibleExceedsMaxDebtAge: EligibilityRules =
    eligibleEligibilityRules.copy(chargesOverMaxDebtAge = Some(true))

  val notEligibleEligibleChargeType: EligibilityRules =
    eligibleEligibilityRules.copy(ineligibleChargeTypes = true)

  val notEligibleMissingFiledReturns: EligibilityRules =
    eligibleEligibilityRules.copy(missingFiledReturns = true)

  val notEligibleNoDueDatesReached: EligibilityRules =
    eligibleEligibilityRules.copy(noDueDatesReached = true)

  val notEligibleHasInvalidInterestSignals: EligibilityRules =
    eligibleEligibilityRules.copy(hasInvalidInterestSignals = Some(true))

  val notEligibleHasInvalidInterestSignalsCESA: EligibilityRules =
    eligibleEligibilityRules.copy(hasInvalidInterestSignalsCESA = Some(true))

  val notEligibleDmSpecialOfficeProcessingRequired: EligibilityRules =
    eligibleEligibilityRules.copy(dmSpecialOfficeProcessingRequired = Some(true))

  val notEligibleCannotFindLockReason: EligibilityRules =
    eligibleEligibilityRules.copy(cannotFindLockReason = Some(true))

  val notEligibleCreditsNotAllowed: EligibilityRules =
    eligibleEligibilityRules.copy(creditsNotAllowed = Some(true))

  val notEligibleIsMoreThanMaxPaymentReference: EligibilityRules =
    eligibleEligibilityRules.copy(isMoreThanMaxPaymentReference = Some(true))

  val notEligibleChargesBeforeMaxAccountingDate: EligibilityRules =
    eligibleEligibilityRules.copy(chargesBeforeMaxAccountingDate = Some(true))

  val notEligibleMultipleReasons: EligibilityRules =
    eligibleEligibilityRules.copy(missingFiledReturns = true, hasRlsOnAddress = true)

  val notEligibleHasDisguisedRemuneration: EligibilityRules =
    eligibleEligibilityRules.copy(hasDisguisedRemuneration = Some(true))

  val notEligibleHasCapacitor: EligibilityRules =
    eligibleEligibilityRules.copy(hasCapacitor = Some(true))

  val notEligibleDmSpecialOfficeProcessingRequiredCDCS: EligibilityRules =
    eligibleEligibilityRules.copy(dmSpecialOfficeProcessingRequiredCDCS = Some(true))

  val notEligibleIsAnMtdCustomer: EligibilityRules =
    eligibleEligibilityRules.copy(isAnMtdCustomer = Some(true))

  val notEligibleDmSpecialOfficeProcessingRequiredCESA: EligibilityRules =
    eligibleEligibilityRules.copy(dmSpecialOfficeProcessingRequiredCESA = Some(true))

  val noMtditsaEnrollment: EligibilityRules =
    eligibleEligibilityRules.copy(noMtditsaEnrollment = Some(true))

  val callEligibilityApiRequestEpaye: CallEligibilityApiRequest = CallEligibilityApiRequest(
    channelIdentifier = "eSSTTP",
    identification = List(Identification(IdType("EMPREF"), IdValue("864FZ00049"))),
    regimeType = RegimeType.EPAYE,
    returnFinancialAssessment = true
  )

  val callEligibilityApiRequestVat: CallEligibilityApiRequest = CallEligibilityApiRequest(
    channelIdentifier = "eSSTTP",
    identification = List(Identification(IdType("VRN"), IdValue("101747001"))),
    regimeType = RegimeType.VAT,
    returnFinancialAssessment = true
  )

  val callEligibilityApiRequestSa: CallEligibilityApiRequest = CallEligibilityApiRequest(
    channelIdentifier = "eSSTTP",
    identification = List(Identification(IdType("UTR"), IdValue("1234567895"))),
    regimeType = RegimeType.SA,
    returnFinancialAssessment = true
  )

  val callEligibilityApiRequestSimp: CallEligibilityApiRequest = CallEligibilityApiRequest(
    channelIdentifier = "eSSTTP",
    identification = List(Identification(IdType("NINO"), IdValue("QQ123456A"))),
    regimeType = RegimeType.SIMP,
    returnFinancialAssessment = true
  )

  def identification(taxRegime: TaxRegime): List[Identification] =
    Json.parse(identificationJsonString(taxRegime)).as[List[Identification]]

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

    case TaxRegime.Simp =>
      """[
        |    {
        |      "idType": "NINO",
        |      "idValue": "QQ123456A"
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

    case TaxRegime.Simp =>
      """{ "nino": "QQ123456A" }"""
  }

  def eligibilityCheckResult(
    eligibilityPass:                     EligibilityPass,
    eligibilityRules:                    EligibilityRules,
    taxRegime:                           TaxRegime,
    regimeDigitalCorrespondence:         RegimeDigitalCorrespondence,
    chargeIsInterestBearingCharge:       Option[Boolean] = None,
    chargeUseChargeReference:            Option[Boolean] = None,
    chargeChargeBeforeMaxAccountingDate: Option[Boolean] = None,
    ddInProgress:                        Option[Boolean] = None,
    maybeIndividalDetails:               Option[IndividualDetails] = None
  ): EligibilityCheckResult =
    EligibilityCheckResult(
      processingDateTime = ProcessingDateTime("2022-03-23T13:49:51.141Z"),
      identification = identification(taxRegime),
      invalidSignals = None,
      customerPostcodes = List(CustomerPostcode(customerPostcode, PostcodeDate(LocalDate.of(2022, 1, 31)))),
      regimePaymentFrequency = PaymentPlanFrequencies.Monthly,
      paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
      paymentPlanMinLength = PaymentPlanMinLength(1),
      paymentPlanMaxLength = PaymentPlanMaxLength(12),
      eligibilityStatus = EligibilityStatus(eligibilityPass),
      eligibilityRules = eligibilityRules,
      chargeTypeAssessment = List(
        ChargeTypeAssessment(
          taxPeriodFrom = TaxPeriodFrom("2020-08-13"),
          taxPeriodTo = TaxPeriodTo("2020-08-14"),
          debtTotalAmount = DebtTotalAmount(AmountInPence(300000)),
          chargeReference = ChargeReference("A00000000001"),
          charges = List(
            Charges(
              chargeType = ChargeType("InYearRTICharge-Tax"),
              mainType = MainType("InYearRTICharge(FPS)"),
              mainTrans = MainTrans("mainTrans"),
              subTrans = SubTrans("subTrans"),
              outstandingAmount = OutstandingAmount(AmountInPence(100000)),
              interestStartDate = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
              dueDate = DueDate(LocalDate.parse("2017-03-07")),
              accruedInterest = AccruedInterest(AmountInPence(1597)),
              ineligibleChargeType = IneligibleChargeType(value = false),
              chargeOverMaxDebtAge = Some(ChargeOverMaxDebtAge(value = false)),
              locks = Some(
                List(Lock(LockType("Payment"), LockReason("Risk/Fraud"), DisallowedChargeLockType(value = false)))
              ),
              dueDateNotReached = false,
              isInterestBearingCharge = chargeIsInterestBearingCharge.map(IsInterestBearingCharge(_)),
              useChargeReference = chargeUseChargeReference.map(UseChargeReference(_)),
              chargeBeforeMaxAccountingDate = chargeChargeBeforeMaxAccountingDate.map(ChargeBeforeMaxAccountingDate(_)),
              ddInProgress = ddInProgress.map(DdInProgress(_)),
              chargeSource = None,
              parentChargeReference = None,
              parentMainTrans = None,
              originalCreationDate = None,
              tieBreaker = None,
              originalTieBreaker = None,
              saTaxYearEnd = None,
              creationDate = None,
              originalChargeType = None
            )
          )
        )
      ),
      customerDetails = List(CustomerDetail(None, None)),
      individualDetails = maybeIndividalDetails,
      addresses = List(
        Address(
          addressType = AddressType("Residential"),
          addressLine1 = None,
          addressLine2 = None,
          addressLine3 = None,
          addressLine4 = None,
          rls = None,
          contactDetails = Some(
            ContactDetail(
              telephoneNumber = None,
              fax = None,
              mobile = None,
              emailAddress = Some(Email(SensitiveString("some@email"))),
              emailSource = None,
              altFormat = None
            )
          ),
          postCode = None,
          country = None,
          postcodeHistory = List(
            PostcodeHistory(
              addressPostcode = Postcode(SensitiveString("AA11AA")),
              postcodeDate = PostcodeDate(LocalDate.now())
            )
          )
        )
      ),
      regimeDigitalCorrespondence = regimeDigitalCorrespondence,
      futureChargeLiabilitiesExcluded = false,
      chargeTypesExcluded = None
    )

  val whyCannotPayReasons: Set[CannotPayReason] =
    Set(CannotPayReason.ChangeToPersonalCircumstances, CannotPayReason.NoMoneySetAside)

  val whyCannotPayReasonsError: Set[CannotPayReason] =
    Set(CannotPayReason.NoMoneySetAside, CannotPayReason.Other)

  def dayOfMonth(day: Int = 28): DayOfMonth = DayOfMonth(day)

  def extremeDatesRequest(initialPayment: Boolean): ExtremeDatesRequest = ExtremeDatesRequest(
    InitialPayment(initialPayment)
  )

  def extremeDatesResponse(): ExtremeDatesResponse = ExtremeDatesResponse(
    Some(InitialPaymentDate(LocalDate.parse("2022-06-24"))),
    earliestPlanStartDate = EarliestPaymentPlanStartDate(LocalDate.parse("2022-07-14")),
    latestPlanStartDate = LatestPaymentPlanStartDate(LocalDate.parse("2022-08-13"))
  )

  def startDatesRequest(initialPayment: Boolean, day: Int): StartDatesRequest =
    StartDatesRequest(InitialPayment(initialPayment), PreferredDayOfMonth(day))

  def startDatesResponse(): StartDatesResponse = StartDatesResponse(
    initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2022-07-03"))),
    instalmentStartDate = InstalmentStartDate(LocalDate.parse("2022-07-28"))
  )

  def instalmentAmountRequest(taxRegime: TaxRegime, maxPlanLength: Int): InstalmentAmountRequest = {
    val regimeType = taxRegime match {
      case TaxRegime.Epaye => RegimeType.EPAYE
      case TaxRegime.Vat   => RegimeType.VAT
      case TaxRegime.Sa    => RegimeType.SA
      case TaxRegime.Simp  => RegimeType.SIMP
    }

    InstalmentAmountRequest(
      channelIdentifier = ChannelIdentifiers.eSSTTP,
      regimeType = regimeType,
      paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
      paymentPlanMinLength = PaymentPlanMinLength(1),
      paymentPlanMaxLength = PaymentPlanMaxLength(maxPlanLength),
      earliestPaymentPlanStartDate = EarliestPaymentPlanStartDate(LocalDate.parse("2022-07-14")),
      latestPaymentPlanStartDate = LatestPaymentPlanStartDate(LocalDate.parse("2022-08-13")),
      initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2022-06-24"))),
      initialPaymentAmount = Some(AmountInPence(200)),
      accruedDebtInterest = AccruedDebtInterest(AmountInPence(3194)),
      debtItemCharges = List(
        DebtItemCharge(
          OutstandingDebtAmount(AmountInPence(50000)),
          mainTrans = MainTrans("mainTrans"),
          subTrans = SubTrans("subTrans"),
          isInterestBearingCharge = Some(IsInterestBearingCharge(value = true)),
          useChargeReference = Some(UseChargeReference(value = true)),
          debtItemChargeId = ChargeReference("A00000000001"),
          interestStartDate = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
          debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-03-07"))
        ),
        DebtItemCharge(
          OutstandingDebtAmount(AmountInPence(100000)),
          mainTrans = MainTrans("mainTrans"),
          subTrans = SubTrans("subTrans"),
          isInterestBearingCharge = Some(IsInterestBearingCharge(value = true)),
          useChargeReference = Some(UseChargeReference(value = true)),
          debtItemChargeId = ChargeReference("A00000000002"),
          interestStartDate = Some(InterestStartDate(LocalDate.parse("2017-02-07"))),
          debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-02-07"))
        )
      ),
      customerPostcodes = Some(List(CustomerPostcode(customerPostcode, PostcodeDate(LocalDate.of(2022, 1, 31)))))
    )
  }

  def affordableQuotesRequest(taxRegime: TaxRegime): AffordableQuotesRequest = {
    val expectedPaymentPlanMaxLength = taxRegime match {
      case TaxRegime.Epaye => PaymentPlanMaxLength(12)
      case TaxRegime.Vat   => PaymentPlanMaxLength(12)
      case TaxRegime.Sa    => PaymentPlanMaxLength(12)
      case TaxRegime.Simp  => PaymentPlanMaxLength(12)
    }
    val regimeType                   = taxRegime match {
      case TaxRegime.Epaye => RegimeType.EPAYE
      case TaxRegime.Vat   => RegimeType.VAT
      case TaxRegime.Sa    => RegimeType.SA
      case TaxRegime.Simp  => RegimeType.SIMP
    }
    AffordableQuotesRequest(
      channelIdentifier = ChannelIdentifiers.eSSTTP,
      regimeType = regimeType,
      paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
      paymentPlanMinLength = PaymentPlanMinLength(1),
      paymentPlanMaxLength = expectedPaymentPlanMaxLength,
      initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2022-07-03"))),
      initialPaymentAmount = Some(UpfrontPaymentAmount(AmountInPence(200))),
      accruedDebtInterest = AccruedDebtInterest(AmountInPence(3194)),
      debtItemCharges = List(
        DebtItemCharge(
          OutstandingDebtAmount(AmountInPence(50000)),
          mainTrans = MainTrans("mainTrans"),
          subTrans = SubTrans("subTrans"),
          isInterestBearingCharge = Some(IsInterestBearingCharge(value = true)),
          useChargeReference = Some(UseChargeReference(value = true)),
          debtItemChargeId = ChargeReference("A00000000001"),
          interestStartDate = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
          debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-03-07"))
        ),
        DebtItemCharge(
          OutstandingDebtAmount(AmountInPence(100000)),
          mainTrans = MainTrans("mainTrans"),
          subTrans = SubTrans("subTrans"),
          isInterestBearingCharge = Some(IsInterestBearingCharge(value = true)),
          useChargeReference = Some(UseChargeReference(value = true)),
          debtItemChargeId = ChargeReference("A00000000002"),
          interestStartDate = Some(InterestStartDate(LocalDate.parse("2017-02-07"))),
          debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-02-07"))
        )
      ),
      customerPostcodes =
        List(CustomerPostcode(Postcode(SensitiveString("AA11AA")), PostcodeDate(LocalDate.of(2022, 1, 31)))),
      paymentPlanAffordableAmount = PaymentPlanAffordableAmount(AmountInPence(30000)),
      paymentPlanStartDate = InstalmentStartDate(LocalDate.parse("2022-07-28"))
    )
  }

  val instalmentAmounts: InstalmentAmounts = InstalmentAmounts(AmountInPence(33333), AmountInPence(100000))

  val pegaStartCaseResponse: StartCaseResponse = StartCaseResponse(PegaCaseId("case"), "testCorrelationId")

  val monthlyPaymentAmount: MonthlyPaymentAmount = MonthlyPaymentAmount(AmountInPence(30000))

  def paymentPlan(numberOfInstalments: Int, amountDue: AmountDue): PaymentPlan = PaymentPlan(
    numberOfInstalments = NumberOfInstalments(numberOfInstalments),
    planDuration = PlanDuration(numberOfInstalments),
    totalDebt = TotalDebt(AmountInPence(amountInPence.value * numberOfInstalments)),
    totalDebtIncInt = TotalDebtIncludingInterest(amountInPence.+(amountInPence)),
    planInterest = PlanInterest(amountInPence),
    collections = Collection(
      initialCollection = Some(
        InitialCollection(dueDate = DueDate(LocalDate.parse("2022-02-01")), amountDue = AmountDue(amountInPence))
      ),
      regularCollections =
        List(RegularCollection(dueDate = DueDate(LocalDate.parse("2022-02-01")), amountDue = amountDue))
    ),
    instalments = List(
      Instalment(
        instalmentNumber = InstalmentNumber(numberOfInstalments),
        dueDate = DueDate(LocalDate.parse("2022-02-01")),
        instalmentInterestAccrued = InterestAccrued(amountInPence),
        instalmentBalance = InstalmentBalance(amountInPence),
        debtItemChargeId = ChargeReference("testchargeid"),
        amountDue = AmountDue(amountInPence),
        debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2022-01-01")),
        expectedPayment = None
      )
    )
  )

  val pegaGetCaseResponse: GetCaseResponse = GetCaseResponse(
    dayOfMonth(),
    paymentPlan(1, AmountDue(AmountInPence(14323))),
    Map("zeroExpenditure" -> BigDecimal(0), "expenditure" -> BigDecimal(2)),
    Map("zeroIncome"      -> BigDecimal(0), "income"      -> BigDecimal(1.23)),
    "testCorrelationId"
  )

  def typeOfBankAccount(typeOfAccount: String): TypeOfBankAccount =
    if (typeOfAccount == "Business") TypesOfBankAccount.Business else TypesOfBankAccount.Personal

  def detailsAboutBankAccount(isAccountHolder: Boolean): CanSetUpDirectDebit =
    CanSetUpDirectDebit(isAccountHolder)

  def directDebitDetails(name: String, sortCode: String, accountNumber: String): BankDetails =
    BankDetails(
      AccountName(SensitiveString(name)),
      SortCode(SensitiveString(sortCode)),
      AccountNumber(SensitiveString(accountNumber))
    )

  // string including & ' / which are the allowed symbols according to IF
  val testAccountName = "Mr. Bob Ross &'/ With Symbols"

  def arrangementRequest(
    customerDetails:             Option[List[CustomerDetail]],
    contactDetails:              Option[ContactDetail],
    regimeDigitalCorrespondence: Option[RegimeDigitalCorrespondence],
    taxRegime:                   TaxRegime,
    accountNumber:               String = "12345678",
    hasAffordabilityAssessment:  Boolean = false,
    caseID:                      Option[PegaCaseId] = None,
    additionalIdentification:    Option[Identification] = None
  ): ArrangementRequest = {
    val regimeType = taxRegime match {
      case TaxRegime.Epaye => RegimeType.EPAYE
      case TaxRegime.Vat   => RegimeType.VAT
      case TaxRegime.Sa    => RegimeType.SA
      case TaxRegime.Simp  => RegimeType.SIMP
    }

    ArrangementRequest(
      channelIdentifier = ChannelIdentifiers.eSSTTP,
      regimeType = regimeType,
      hasAffordabilityAssessment = hasAffordabilityAssessment,
      caseID = caseID,
      regimePaymentFrequency = PaymentPlanFrequencies.Monthly,
      arrangementAgreedDate = ArrangementAgreedDate(LocalDate.now(ZoneOffset.of("Z")).toString),
      identification = {
        val id = identification(taxRegime)
        additionalIdentification.fold(id)(_ :: id)
      },
      directDebitInstruction = DirectDebitInstruction(
        sortCode = SortCode(SensitiveString("123456")),
        accountNumber = AccountNumber(SensitiveString(accountNumber)),
        accountName = AccountName(SensitiveString(testAccountName)),
        paperAuddisFlag = PaperAuddisFlag(value = false)
      ),
      paymentPlan = EnactPaymentPlan(
        planDuration = PlanDuration(2),
        paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
        numberOfInstalments = NumberOfInstalments(2),
        totalDebt = TotalDebt(AmountInPence(111141)),
        totalDebtIncInt = TotalDebtIncludingInterest(AmountInPence(111147)),
        planInterest = PlanInterest(AmountInPence(6)),
        collections = Collection(
          initialCollection = Some(
            InitialCollection(
              dueDate = DueDate(LocalDate.parse("2022-07-03")),
              amountDue = AmountDue(AmountInPence(12312))
            )
          ),
          regularCollections = List(
            RegularCollection(
              dueDate = DueDate(LocalDate.parse("2022-09-28")),
              amountDue = AmountDue(AmountInPence(55573))
            ),
            RegularCollection(
              dueDate = DueDate(LocalDate.parse("2022-08-28")),
              amountDue = AmountDue(AmountInPence(55573))
            )
          )
        ),
        instalments = List(
          Instalment(
            instalmentNumber = InstalmentNumber(2),
            dueDate = DueDate(LocalDate.parse("2022-09-28")),
            instalmentInterestAccrued = InterestAccrued(AmountInPence(3)),
            instalmentBalance = InstalmentBalance(AmountInPence(55571)),
            debtItemChargeId = ChargeReference("A00000000001"),
            amountDue = AmountDue(AmountInPence(55570)),
            debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2021-07-28")),
            expectedPayment = None
          ),
          Instalment(
            instalmentNumber = InstalmentNumber(1),
            dueDate = DueDate(LocalDate.parse("2022-08-28")),
            instalmentInterestAccrued = InterestAccrued(AmountInPence(3)),
            instalmentBalance = InstalmentBalance(AmountInPence(111141)),
            debtItemChargeId = ChargeReference("A00000000001"),
            amountDue = AmountDue(AmountInPence(55570)),
            debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2021-07-28")),
            expectedPayment = None
          )
        ),
        debtItemCharges = List(
          DebtItemCharges(
            outstandingDebtAmount = OutstandingDebtAmount(AmountInPence(50000)),
            debtItemChargeId = ChargeReference("A00000000001"),
            debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-03-07")),
            accruedInterest = AccruedInterest(AmountInPence(1597)),
            isInterestBearingCharge = Some(IsInterestBearingCharge(value = true)),
            useChargeReference = Some(UseChargeReference(value = true)),
            mainTrans = Some(MainTrans("mainTrans")),
            subTrans = Some(SubTrans("subTrans")),
            parentChargeReference = None,
            parentMainTrans = None,
            creationDate = None,
            originalCreationDate = None,
            saTaxYearEnd = None,
            tieBreaker = None,
            originalTieBreaker = None,
            chargeType = Some(ChargeType("InYearRTICharge-Tax")),
            originalChargeType = None,
            chargeSource = None,
            interestStartDate = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
            taxPeriodFrom = Some(TaxPeriodFrom("2020-08-13")),
            taxPeriodTo = Some(TaxPeriodTo("2020-08-14"))
          ),
          DebtItemCharges(
            outstandingDebtAmount = OutstandingDebtAmount(AmountInPence(100000)),
            debtItemChargeId = ChargeReference("A00000000002"),
            debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.parse("2017-02-07")),
            accruedInterest = AccruedInterest(AmountInPence(1597)),
            isInterestBearingCharge = Some(IsInterestBearingCharge(value = true)),
            useChargeReference = Some(UseChargeReference(value = true)),
            mainTrans = Some(MainTrans("mainTrans")),
            subTrans = Some(SubTrans("subTrans")),
            parentChargeReference = None,
            parentMainTrans = None,
            creationDate = None,
            originalCreationDate = None,
            saTaxYearEnd = None,
            tieBreaker = None,
            originalTieBreaker = None,
            chargeType = Some(ChargeType("InYearRTICharge-Tax")),
            originalChargeType = None,
            chargeSource = None,
            interestStartDate = Some(InterestStartDate(LocalDate.parse("2017-02-07"))),
            taxPeriodFrom = Some(TaxPeriodFrom("2020-07-13")),
            taxPeriodTo = Some(TaxPeriodTo("2020-07-14"))
          )
        )
      ),
      customerDetails = customerDetails,
      individualDetails =
        if taxRegime == Sa then Some(IndividualDetails(None, None, None, None, None, Some(MTDITSA), None)) else None,
      addresses = Some(
        List(
          Address(
            addressType = AddressType("Residential"),
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            rls = None,
            contactDetails = contactDetails,
            postCode = None,
            country = None,
            postcodeHistory = List(
              PostcodeHistory(
                addressPostcode = Postcode(SensitiveString("AA11AA")),
                postcodeDate = PostcodeDate(LocalDate.now())
              )
            )
          )
        )
      ),
      regimeDigitalCorrespondence = regimeDigitalCorrespondence
    )
  }

  def customerDetail(
    email:  String = "bobross@joyofpainting.com",
    source: EmailSource = EmailSource.ETMP
  ): Option[List[CustomerDetail]] =
    Some(List(CustomerDetail(Some(Email(SensitiveString(email))), Some(source))))

  def contactDetails(
    email:  String = "bobross@joyofpainting.com",
    source: EmailSource = EmailSource.ETMP
  ): Option[ContactDetail] =
    Some(ContactDetail(None, None, None, Some(Email(SensitiveString(email))), Some(source), None))

  val someRegimeDigitalCorrespondenceFalse: Option[RegimeDigitalCorrespondence] = Some(
    RegimeDigitalCorrespondence(value = false)
  )
  val someRegimeDigitalCorrespondenceTrue: Option[RegimeDigitalCorrespondence]  = Some(
    RegimeDigitalCorrespondence(value = true)
  )

  def customerReference(taxRegime: TaxRegime): CustomerReference = taxRegime match {
    case TaxRegime.Epaye => CustomerReference("123PA44545546")
    case TaxRegime.Vat   => CustomerReference("101747001")
    case TaxRegime.Sa    => CustomerReference("1234567895")
    case TaxRegime.Simp  => CustomerReference("QQ123456A")
  }

  def arrangementResponse(taxRegime: TaxRegime): ArrangementResponse =
    ArrangementResponse(ProcessingDateTime("2022-03-23T13:49:51.141Z"), customerReference(taxRegime))

  def taxDetailForAuditEvent(taxRegime: TaxRegime): String = taxRegime match {
    case TaxRegime.Epaye => """"employerRef": "864FZ00049", "accountsOfficeRef": "123PA44545546""""
    case TaxRegime.Vat   => """"vrn": "101747001""""
    case TaxRegime.Sa    => """"utr": "1234567895""""
    case TaxRegime.Simp  => """"nino": "QQ123456A""""
  }
}
