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

package controllers

import config.AppConfig
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.TaxRegime.Sa
import essttp.rootmodel.ttp.CustomerTypes
import essttp.rootmodel.ttp.eligibility.{AssessmentCategory, EligibilityRules, IndividualDetails, RegimeDigitalCorrespondence}
import models.{AssessmentCategoryInfo, EligibilityReqIdentificationFlag, Languages}
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks.*
import play.api.http.Status
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.test.Helpers.*
import testsupport.TdRequest.*
import testsupport.reusableassertions.ContentAssertions
import testsupport.stubs.{AuditConnectorStub, EssttpBackend, Ttp}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll, TtpJsonResponses}
import testsupport.{CombinationsHelper, ItSpec}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.jdk.CollectionConverters.IterableHasAsScala

class DetermineEligibilityControllerSpec extends ItSpec, CombinationsHelper {
  given EligibilityReqIdentificationFlag = app.injector.instanceOf[AppConfig].eligibilityReqIdentificationFlag

  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]

  "Determine eligibility endpoint" - {
    forAll(
      Table(
        (
          "Scenario flavour",
          "eligibility rules",
          "assessment category info",
          "ineligibility reason audit string",
          "expected redirect",
          "updated journey json",
          "origin"
        ),
        (
          "HasRlsOnAddress - EPAYE",
          TdAll.notEligibleHasRlsOnAddress,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasRlsOnAddress",
          PageUrls.epayeRLSUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "HasRlsOnAddress - VAT",
          TdAll.notEligibleHasRlsOnAddress,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasRlsOnAddress",
          PageUrls.vatRLSUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "HasRlsOnAddress - SA",
          TdAll.notEligibleHasRlsOnAddress,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasRlsOnAddress",
          PageUrls.saRLSUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "HasRlsOnAddress - SIMP",
          TdAll.notEligibleHasRlsOnAddress,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasRlsOnAddress",
          PageUrls.simpRLSUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "MarkedAsInsolvent - EPAYE",
          TdAll.notEligibleMarkedAsInsolvent,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "markedAsInsolvent",
          PageUrls.payeNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - MarkedAsInsolvent`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "MarkedAsInsolvent - VAT",
          TdAll.notEligibleMarkedAsInsolvent,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "markedAsInsolvent",
          PageUrls.vatNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - MarkedAsInsolvent`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "MarkedAsInsolvent - SA",
          TdAll.notEligibleMarkedAsInsolvent,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "markedAsInsolvent",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - MarkedAsInsolvent`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "MarkedAsInsolvent - SIMP",
          TdAll.notEligibleMarkedAsInsolvent,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "markedAsInsolvent",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - MarkedAsInsolvent`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "IsLessThanMinDebtAllowance - EPAYE - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleIsLessThanMinDebtAllowance)),
          "isLessThanMinDebtAllowance",
          PageUrls.epayeDebtTooSmallUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "IsLessThanMinDebtAllowance - VAT - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleIsLessThanMinDebtAllowance)),
          "isLessThanMinDebtAllowance",
          PageUrls.vatDebtTooSmallUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "IsLessThanMinDebtAllowance - SA - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleIsLessThanMinDebtAllowance)),
          "isLessThanMinDebtAllowance",
          PageUrls.saDebtTooSmallUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "IsLessThanMinDebtAllowance - SIMP - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleIsLessThanMinDebtAllowance)),
          "isLessThanMinDebtAllowance",
          PageUrls.simpDebtTooSmallUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "IsLessThanMinDebtAllowance - VAT - debts",
          TdAll.eligibleEligibilityRules,
          Seq(
            AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities, TdAll.assessmentEligibilityRules),
            AssessmentCategoryInfo(AssessmentCategory.Liabilities, TdAll.assessmentEligibilityRules),
            AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsLessThanMinDebtAllowance)
          ),
          "isLessThanMinDebtAllowance",
          PageUrls.vatDebtTooSmallUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ineligibility in ChargeTypesAssessments`(
            Origins.Vat.Bta,
            Seq(
              AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities, TdAll.assessmentEligibilityRules),
              AssessmentCategoryInfo(AssessmentCategory.Liabilities, TdAll.assessmentEligibilityRules),
              AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsLessThanMinDebtAllowance)
            )
          ),
          Origins.Vat.Bta
        ),
        (
          "IsLessThanMinDebtAllowance - SA - debts",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsLessThanMinDebtAllowance)),
          "isLessThanMinDebtAllowance",
          PageUrls.saDebtTooSmallUrl,
          JourneyJsonTemplates
            .`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(Origins.Sa.Bta, AssessmentCategory.Debts),
          Origins.Sa.Bta
        ),
        (
          "IsLessThanMinDebtAllowance - SIMP - debts",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsLessThanMinDebtAllowance)),
          "isLessThanMinDebtAllowance",
          PageUrls.simpDebtTooSmallUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(
            Origins.Simp.Pta,
            AssessmentCategory.Debts
          ),
          Origins.Simp.Pta
        ),
        (
          "IsLessThanMinDebtAllowance - SIMP - liabilities",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Liabilities, TdAll.notEligibleIsLessThanMinDebtAllowance)),
          "isLessThanMinDebtAllowance",
          PageUrls.simpDebtTooSmallUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(
            Origins.Simp.Pta,
            AssessmentCategory.Liabilities
          ),
          Origins.Simp.Pta
        ),
        (
          "IsMoreThanMaxDebtAllowance - EPAYE - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleIsMoreThanMaxDebtAllowance)),
          "isMoreThanMaxDebtAllowance",
          PageUrls.epayeDebtTooLargeUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "IsMoreThanMaxDebtAllowance - VAT - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleIsMoreThanMaxDebtAllowance)),
          "isMoreThanMaxDebtAllowance",
          PageUrls.vatDebtTooLargeUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "IsMoreThanMaxDebtAllowance - SA - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleIsMoreThanMaxDebtAllowance)),
          "isMoreThanMaxDebtAllowance",
          PageUrls.saDebtTooLargeUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "IsMoreThanMaxDebtAllowance - SIMP - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleIsMoreThanMaxDebtAllowance)),
          "isMoreThanMaxDebtAllowance",
          PageUrls.simpDebtTooLargeUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "IsMoreThanMaxDebtAllowance - EPAYE - debts",
          // test allChargeTypeAssessmentsFailed doesn't contribute an extra eligibility failure when
          // seeing if there are multiple eligibility failures
          TdAll.eligibleEligibilityRules.copy(allChargeTypeAssessmentsFailed = Some(true)),
          Seq(AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsMoreThanMaxDebtAllowance)),
          "isMoreThanMaxDebtAllowance",
          PageUrls.epayeDebtTooLargeUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(
            Origins.Epaye.Bta,
            AssessmentCategory.Debts,
            TdAll.eligibleEligibilityRules.copy(allChargeTypeAssessmentsFailed = Some(true))
          ),
          Origins.Epaye.Bta
        ),
        (
          "IsMoreThanMaxDebtAllowance - VAT - debts",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsMoreThanMaxDebtAllowance)),
          "isMoreThanMaxDebtAllowance",
          PageUrls.vatDebtTooLargeUrl,
          JourneyJsonTemplates
            .`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Vat.Bta, AssessmentCategory.Debts),
          Origins.Vat.Bta
        ),
        (
          "IsMoreThanMaxDebtAllowance - SA - debts",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsMoreThanMaxDebtAllowance)),
          "isMoreThanMaxDebtAllowance",
          PageUrls.saDebtTooLargeUrl,
          JourneyJsonTemplates
            .`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Sa.Bta, AssessmentCategory.Debts),
          Origins.Sa.Bta
        ),
        (
          "IsMoreThanMaxDebtAllowance - SIMP - debts",
          TdAll.eligibleEligibilityRules,
          Seq(
            AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsMoreThanMaxDebtAllowance),
            AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities, TdAll.assessmentEligibilityRules),
            AssessmentCategoryInfo(AssessmentCategory.Liabilities, TdAll.assessmentEligibilityRules)
          ),
          "isMoreThanMaxDebtAllowance",
          PageUrls.simpDebtTooLargeUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ineligibility in ChargeTypesAssessments`(
            Origins.Simp.Pta,
            Seq(
              AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleIsMoreThanMaxDebtAllowance),
              AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities, TdAll.assessmentEligibilityRules),
              AssessmentCategoryInfo(AssessmentCategory.Liabilities, TdAll.assessmentEligibilityRules)
            )
          ),
          Origins.Simp.Pta
        ),
        (
          "IsMoreThanMaxDebtAllowance - SA - liabilities",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Liabilities, TdAll.notEligibleIsMoreThanMaxDebtAllowance)),
          "isMoreThanMaxDebtAllowance",
          PageUrls.saDebtTooLargeUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(
            Origins.Sa.Bta,
            AssessmentCategory.Liabilities
          ),
          Origins.Sa.Bta
        ),
        (
          "DisallowedChargeLockTypes - EPAYE",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleDisallowedChargeLockTypes)),
          "disallowedChargeLockTypes",
          PageUrls.payeNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "DisallowedChargeLockTypes - VAT",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleDisallowedChargeLockTypes)),
          "disallowedChargeLockTypes",
          PageUrls.vatNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "DisallowedChargeLockTypes - SA",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleDisallowedChargeLockTypes)),
          "disallowedChargeLockTypes",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "DisallowedChargeLockTypes - SIMP",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleDisallowedChargeLockTypes)),
          "disallowedChargeLockTypes",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "ExistingTTP - EPAYE",
          TdAll.notEligibleExistingTTP,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "existingTTP",
          PageUrls.epayeAlreadyHaveAPaymentPlanUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "ExistingTTP - VAT",
          TdAll.notEligibleExistingTTP,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "existingTTP",
          PageUrls.vatAlreadyHaveAPaymentPlanUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "ExistingTTP - SA",
          TdAll.notEligibleExistingTTP,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "existingTTP",
          PageUrls.saAlreadyHaveAPaymentPlanUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "ExistingTTP - SIMP",
          TdAll.notEligibleExistingTTP,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "existingTTP",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "ExceedsMaxDebtAge - EPAYE - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleExceedsMaxDebtAge)),
          "chargesOverMaxDebtAge",
          PageUrls.epayeDebtTooOldUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "ExceedsMaxDebtAge - VAT - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleExceedsMaxDebtAge)),
          "chargesOverMaxDebtAge",
          PageUrls.vatDebtTooOldUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "ExceedsMaxDebtAge - SA - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleExceedsMaxDebtAge)),
          "chargesOverMaxDebtAge",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "ExceedsMaxDebtAge - SIMP - standard",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleExceedsMaxDebtAge)),
          "chargesOverMaxDebtAge",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "ExceedsMaxDebtAge - EPAYE - debts",
          TdAll.eligibleEligibilityRules,
          Seq(
            AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleExceedsMaxDebtAge),
            AssessmentCategoryInfo(AssessmentCategory.Liabilities, TdAll.assessmentEligibilityRules),
            AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities, TdAll.assessmentEligibilityRules)
          ),
          "chargesOverMaxDebtAge",
          PageUrls.epayeDebtTooOldUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ineligibility in ChargeTypesAssessments`(
            Origins.Epaye.Bta,
            Seq(
              AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleExceedsMaxDebtAge),
              AssessmentCategoryInfo(AssessmentCategory.Liabilities, TdAll.assessmentEligibilityRules),
              AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities, TdAll.assessmentEligibilityRules)
            )
          ),
          Origins.Epaye.Bta
        ),
        (
          "ExceedsMaxDebtAge - VAT - debts",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleExceedsMaxDebtAge)),
          "chargesOverMaxDebtAge",
          PageUrls.vatDebtTooOldUrl,
          JourneyJsonTemplates
            .`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Vat.Bta, AssessmentCategory.Debts),
          Origins.Vat.Bta
        ),
        (
          "ExceedsMaxDebtAge - SA - debts",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleExceedsMaxDebtAge)),
          "chargesOverMaxDebtAge",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates
            .`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Sa.Bta, AssessmentCategory.Debts),
          Origins.Sa.Bta
        ),
        (
          "ExceedsMaxDebtAge - SIMP - debts",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Debts, TdAll.notEligibleExceedsMaxDebtAge)),
          "chargesOverMaxDebtAge",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates
            .`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Simp.Pta, AssessmentCategory.Debts),
          Origins.Simp.Pta
        ),
        (
          "IneligibleChargeType - EPAYE",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleEligibleChargeType)),
          "ineligibleChargeTypes",
          PageUrls.payeNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - EligibleChargeType`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "IneligibleChargeType - VAT",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleEligibleChargeType)),
          "ineligibleChargeTypes",
          PageUrls.vatNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - EligibleChargeType`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "IneligibleChargeType - SA",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleEligibleChargeType)),
          "ineligibleChargeTypes",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - EligibleChargeType`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "IneligibleChargeType - SIMP",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleEligibleChargeType)),
          "ineligibleChargeTypes",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - EligibleChargeType`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "MissingFiledReturns - EPAYE",
          TdAll.notEligibleMissingFiledReturns,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "missingFiledReturns",
          PageUrls.epayeFileYourReturnUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "MissingFiledReturns - VAT",
          TdAll.notEligibleMissingFiledReturns,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "missingFiledReturns",
          PageUrls.vatFileYourReturnUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "MissingFiledReturns - SA",
          TdAll.notEligibleMissingFiledReturns,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "missingFiledReturns",
          PageUrls.saFileYourReturnUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "MissingFiledReturns - SIMP",
          TdAll.notEligibleMissingFiledReturns,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "missingFiledReturns",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "NoDueDatesReached - EPAYE",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleNoDueDatesReached)),
          "noDueDatesReached",
          PageUrls.payeNoDueDatesReachedUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "NoDueDatesReached - VAT",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleNoDueDatesReached)),
          "noDueDatesReached",
          PageUrls.vatNoDueDatesReachedUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "NoDueDatesReached - SA",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleNoDueDatesReached)),
          "noDueDatesReached",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "NoDueDatesReached - SIMP",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleNoDueDatesReached)),
          "noDueDatesReached",
          PageUrls.simpNoDueDatesReachedUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "HasInvalidInterestSignals - EPAYE",
          TdAll.notEligibleHasInvalidInterestSignals,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasInvalidInterestSignals",
          PageUrls.payeNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "HasInvalidInterestSignals - VAT",
          TdAll.notEligibleHasInvalidInterestSignals,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasInvalidInterestSignals",
          PageUrls.vatNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "HasInvalidInterestSignals - SA",
          TdAll.notEligibleHasInvalidInterestSignals,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasInvalidInterestSignals",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "HasInvalidInterestSignals - SIMP",
          TdAll.notEligibleHasInvalidInterestSignals,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasInvalidInterestSignals",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "HasInvalidInterestSignalsCESA - SA",
          TdAll.notEligibleHasInvalidInterestSignalsCESA,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasInvalidInterestSignalsCESA",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignalsCESA`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "DmSpecialOfficeProcessingRequired - EPAYE",
          TdAll.notEligibleDmSpecialOfficeProcessingRequired,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "dmSpecialOfficeProcessingRequired",
          PageUrls.payeNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(
            Origins.Epaye.Bta
          ),
          Origins.Epaye.Bta
        ),
        (
          "DmSpecialOfficeProcessingRequired - VAT",
          TdAll.notEligibleDmSpecialOfficeProcessingRequired,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "dmSpecialOfficeProcessingRequired",
          PageUrls.vatNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "DmSpecialOfficeProcessingRequired - SA",
          TdAll.notEligibleDmSpecialOfficeProcessingRequired,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "dmSpecialOfficeProcessingRequired",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "DmSpecialOfficeProcessingRequired - SIMP",
          TdAll.notEligibleDmSpecialOfficeProcessingRequired,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "dmSpecialOfficeProcessingRequired",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "CannotFindLockReason - EPAYE",
          TdAll.notEligibleCannotFindLockReason,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "cannotFindLockReason",
          PageUrls.payeNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "CannotFindLockReason - VAT",
          TdAll.notEligibleCannotFindLockReason,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "cannotFindLockReason",
          PageUrls.vatNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "CannotFindLockReason - SA",
          TdAll.notEligibleCannotFindLockReason,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "cannotFindLockReason",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "CannotFindLockReason - SIMP",
          TdAll.notEligibleCannotFindLockReason,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "cannotFindLockReason",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "CreditsNotAllowed - EPAYE",
          TdAll.notEligibleCreditsNotAllowed,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "creditsNotAllowed",
          PageUrls.payeNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "CreditsNotAllowed - VAT",
          TdAll.notEligibleCreditsNotAllowed,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "creditsNotAllowed",
          PageUrls.vatNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "CreditsNotAllowed - SA",
          TdAll.notEligibleCreditsNotAllowed,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "creditsNotAllowed",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "CreditsNotAllowed - SIMP",
          TdAll.notEligibleCreditsNotAllowed,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "creditsNotAllowed",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "IsMoreThanMaxPaymentReference - EPAYE",
          TdAll.notEligibleIsMoreThanMaxPaymentReference,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "isMoreThanMaxPaymentReference",
          PageUrls.payeNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxPaymentReference`(Origins.Epaye.Bta),
          Origins.Epaye.Bta
        ),
        (
          "IsMoreThanMaxPaymentReference - VAT",
          TdAll.notEligibleIsMoreThanMaxPaymentReference,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "isMoreThanMaxPaymentReference",
          PageUrls.vatNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxPaymentReference`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "IsMoreThanMaxPaymentReference - SA",
          TdAll.notEligibleIsMoreThanMaxPaymentReference,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "isMoreThanMaxPaymentReference",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxPaymentReference`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "IsMoreThanMaxPaymentReference - SIMP",
          TdAll.notEligibleIsMoreThanMaxPaymentReference,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "isMoreThanMaxPaymentReference",
          PageUrls.simpNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxPaymentReference`(Origins.Simp.Pta),
          Origins.Simp.Pta
        ),
        (
          "ChargesBeforeMaxAccountingDate - VAT",
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleChargesBeforeMaxAccountingDate)),
          "chargesBeforeMaxAccountingDate",
          PageUrls.vatDebtBeforeAccountingDateUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - ChargesBeforeMaxAccountingDate`(Origins.Vat.Bta),
          Origins.Vat.Bta
        ),
        (
          "HasDisguisedRemuneration - SA",
          TdAll.notEligibleHasDisguisedRemuneration,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasDisguisedRemuneration",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasDisguisedRemuneration`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "HasCapacitor - SA",
          TdAll.notEligibleHasCapacitor,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "hasCapacitor",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasCapacitor`(Origins.Sa.Bta),
          Origins.Sa.Bta
        ),
        (
          "DmSpecialOfficeProcessingRequiredCESA - SA",
          TdAll.notEligibleDmSpecialOfficeProcessingRequiredCESA,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "dmSpecialOfficeProcessingRequiredCESA",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - dmSpecialOfficeProcessingRequiredCESA`(
            Origins.Sa.Bta
          ),
          Origins.Sa.Bta
        ),
        (
          "DmSpecialOfficeProcessingRequiredCDCS - SA",
          TdAll.notEligibleDmSpecialOfficeProcessingRequiredCDCS,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          "dmSpecialOfficeProcessingRequiredCDCS",
          PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequiredCDCS`(
            Origins.Sa.Bta
          ),
          Origins.Sa.Bta
        )
      )
    ) {
      (
        sf: String,
        eligibilityRules: EligibilityRules,
        assessmentCategoryInfo: Seq[AssessmentCategoryInfo],
        auditIneligibilityReason: String,
        expectedRedirect: String,
        updatedJourneyJson: String,
        origin: Origin
      ) =>
        s"Ineligible: [$sf] should redirect to $expectedRedirect" in {
          val eligibilityCheckResponseJson         = TtpJsonResponses.ttpEligibilityCallJson(
            taxRegime = origin.taxRegime,
            eligibilityPass = TdAll.notEligibleEligibilityPass,
            eligibilityRules = eligibilityRules,
            assessmentCategoryInfo = assessmentCategoryInfo,
            regimeDigitalCorrespondence = true,
            maybeCustomerType = Some(CustomerTypes.MTDITSA)
          )
          // for audit event
          val eligibilityCheckResponseJsonAsPounds =
            TtpJsonResponses.ttpEligibilityCallJson(
              taxRegime = origin.taxRegime,
              eligibilityPass = TdAll.notEligibleEligibilityPass,
              eligibilityRules = eligibilityRules,
              assessmentCategoryInfo = assessmentCategoryInfo,
              poundsInsteadOfPence = true,
              regimeDigitalCorrespondence = true,
              maybeCustomerType = Some(CustomerTypes.MTDITSA)
            )

          origin.taxRegime match {
            case Sa => stubCommonActions(authNino = Some("QQ123456A")) // NINO required for SA
            case _  => stubCommonActions()
          }

          EssttpBackend.DetermineTaxId.findJourney(origin)()
          Ttp.Eligibility.stubRetrieveEligibility(origin.taxRegime)(eligibilityCheckResponseJson)
          EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(TdAll.journeyId, updatedJourneyJson)

          val result = controller.determineEligibility(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedRedirect)
          Ttp.Eligibility.verifyTtpEligibilityRequests(origin.taxRegime)

          EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
            journeyId = TdAll.journeyId,
            expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
              TdAll.notEligibleEligibilityPass,
              eligibilityRules,
              assessmentCategoryInfo,
              origin.taxRegime,
              RegimeDigitalCorrespondence(value = true),
              maybeIndividalDetails = Some(
                IndividualDetails(
                  None,
                  None,
                  None,
                  None,
                  None,
                  Some(CustomerTypes.MTDITSA),
                  None
                )
              )
            )
          )(using testOperationCryptoFormat)

          val (expectedTaxType, expectedTaxDetailsJson) =
            origin.taxRegime match {
              case TaxRegime.Epaye =>
                (
                  "Epaye",
                  """{
                    |    "employerRef": "864FZ00049",
                    |    "accountsOfficeRef": "123PA44545546"
                    |  }""".stripMargin
                )
              case TaxRegime.Vat   =>
                ("Vat", """{ "vrn": "101747001" }""")

              case TaxRegime.Sa =>
                ("Sa", """{ "utr": "1234567895" }""")

              case TaxRegime.Simp =>
                ("Simp", """{ "nino": "QQ123456A" }""")
            }

          AuditConnectorStub.verifyEventAudited(
            "EligibilityCheck",
            Json
              .parse(
                s"""
                 |{
                 |  "eligibilityResult" : "ineligible",
                 |  "enrollmentReasons": "did not pass eligibility check",
                 |  "noEligibilityReasons": 1,
                 |  "eligibilityReasons" : [ "$auditIneligibilityReason" ],
                 |  "origin": "${origin.toString().split('.').last}",
                 |  "taxType": "$expectedTaxType",
                 |  "taxDetail": $expectedTaxDetailsJson,
                 |  "authProviderId": "authId-999",
                 |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                 |  "chargeTypeAssessment" :${(Json
                    .parse(eligibilityCheckResponseJsonAsPounds)
                    .as[JsObject] \ "chargeTypeAssessments" \ 0 \ "chargeTypeAssessment")
                    .getOrElse(JsArray())
                    .toString},
                 |  "futureChargeLiabilitiesExcluded": false
                 |}
                 |""".stripMargin
              )
              .as[JsObject]
          )

          if (origin.taxRegime == TaxRegime.Sa) {
            AuditConnectorStub.verifyEventAudited(
              auditType = "EligibilityCheck",
              auditEvent = Json
                .parse(
                  s"""
                     |{
                     |  "taxDetail": ${TdAll.taxDetailJsonString(TaxRegime.Sa)},
                     |  "saCustomerType": "MTD(ITSA)",
                     |  "chargeTypeAssessment" : [ {
                     |  "taxPeriodFrom" : "2020-08-13",
                     |  "taxPeriodTo" : "2020-08-14",
                     |  "debtTotalAmount" : 3000,
                     |  "chargeReference" : "A00000000001",
                     |  "charges" : [ {
                     |    "chargeType" : "InYearRTICharge-Tax",
                     |    "mainType" : "InYearRTICharge(FPS)",
                     |    "mainTrans" : "mainTrans",
                     |    "subTrans" : "subTrans",
                     |    "outstandingAmount" : 1000,
                     |    "interestStartDate" : "2017-03-07",
                     |    "dueDate" : "2017-03-07",
                     |    "accruedInterest" : 15.97,
                     |    "ineligibleChargeType" : false,
                     |    "chargeOverMaxDebtAge" : false,
                     |    "locks" : [ {
                     |      "lockType" : "Payment",
                     |      "lockReason" : "Risk/Fraud",
                     |      "disallowedChargeLockType" : false
                     |    } ],
                     |    "dueDateNotReached" : false
                     |  } ]
                     |} ],
                     |  "noEligibilityReasons" : 1,
                     |  "eligibilityReasons" : [ "$auditIneligibilityReason" ],
                     |  "regimeDigitalCorrespondence" : true,
                     |  "authProviderId" : "authId-999",
                     |  "eligibilityResult" : "ineligible",
                     |  "taxType" : "Sa",
                     |  "origin" : "Bta",
                     |  "futureChargeLiabilitiesExcluded" : false,
                     |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                     |  "enrollmentReasons" : "did not pass eligibility check"
                     |}
                     |""".stripMargin
                )
                .as[JsObject]
            )
          }
        }

    }

    "Ineligible: NoDueDatesReached with isLessThanMinDebtAllowance also true should redirect correctly" - {
      forAll(
        Table(
          (
            "Scenario flavour",
            "eligibility rules",
            "assessment eligibility rules",
            "expected redirect",
            "updated journey json",
            "origin"
          ),
          (
            "NoDueDatesReached - EPAYE",
            TdAll.eligibleEligibilityRules,
            Seq(
              AssessmentCategoryInfo(
                AssessmentCategory.Standard,
                TdAll.notEligibleNoDueDatesReached.copy(isLessThanMinDebtAllowance = true)
              )
            ),
            PageUrls.payeNoDueDatesReachedUrl,
            JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Epaye.Bta),
            Origins.Epaye.Bta
          ),
          (
            "NoDueDatesReached - VAT",
            TdAll.eligibleEligibilityRules,
            Seq(
              AssessmentCategoryInfo(
                AssessmentCategory.Standard,
                TdAll.notEligibleNoDueDatesReached.copy(isLessThanMinDebtAllowance = true)
              )
            ),
            PageUrls.vatNoDueDatesReachedUrl,
            JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Vat.Bta),
            Origins.Vat.Bta
          ),
          (
            "NoDueDatesReached - SA",
            TdAll.eligibleEligibilityRules,
            Seq(
              AssessmentCategoryInfo(
                AssessmentCategory.Standard,
                TdAll.notEligibleNoDueDatesReached.copy(isLessThanMinDebtAllowance = true)
              )
            ),
            PageUrls.saNotEligibleUrl,
            JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Sa.Bta),
            Origins.Sa.Bta
          )
        )
      ) {
        (
          sf: String,
          eligibilityRules: EligibilityRules,
          assessmentCategoryInfo: Seq[AssessmentCategoryInfo],
          expectedRedirect: String,
          updatedJourneyJson: String,
          origin: Origin
        ) =>
          s"[$sf] should redirect to noDueDatesReached page" in {
            val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
              origin.taxRegime,
              TdAll.notEligibleEligibilityPass,
              eligibilityRules,
              assessmentCategoryInfo,
              regimeDigitalCorrespondence = true
            )

            stubCommonActions()
            EssttpBackend.DetermineTaxId.findJourney(origin)()
            Ttp.Eligibility.stubRetrieveEligibility(origin.taxRegime)(eligibilityCheckResponseJson)
            EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(TdAll.journeyId, updatedJourneyJson)

            val result = controller.determineEligibility(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(expectedRedirect)
          }
      }
    }

    "no chargeTypeAssessments returned for SIMP should redirect to the no due dates ineligible page" in {
      val eligibilityResponseJson =
        TtpJsonResponses.ttpEligibilityCallJson(
          TaxRegime.Simp,
          TdAll.notEligibleEligibilityPass,
          TdAll.eligibleEligibilityRules,
          regimeDigitalCorrespondence = true,
          assessmentCategoryInfo = Seq.empty
        )

      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Simp.Mobile)()
      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Simp)(eligibilityResponseJson)
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ineligibility in ChargeTypesAssessments`(
          Origins.Simp.Mobile,
          Seq.empty
        )
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.simpNoDueDatesReachedUrl)
      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Simp)

      EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
        journeyId = TdAll.journeyId,
        expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
          TdAll.notEligibleEligibilityPass,
          TdAll.eligibleEligibilityRules,
          Seq.empty,
          TaxRegime.Simp,
          RegimeDigitalCorrespondence(true),
          maybeIndividalDetails = None
        )
      )(using testOperationCryptoFormat)

      AuditConnectorStub.verifyEventAudited(
        "EligibilityCheck",
        Json
          .parse(
            s"""
               |{
               |  "eligibilityResult" : "ineligible",
               |  "enrollmentReasons": "did not pass eligibility check",
               |  "noEligibilityReasons": 0,
               |  "eligibilityReasons" : [ ],
               |  "origin": "Mobile",
               |  "taxType": "Simp",
               |  "taxDetail": { "nino": "QQ123456A" },
               |  "authProviderId": "authId-999",
               |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
               |  "chargeTypeAssessment" : [],
               |  "futureChargeLiabilitiesExcluded": false
               |}
               |""".stripMargin
          )
          .as[JsObject]
      )
    }

    "allChargeTypeAssessmentsFailed should not count as an eligibility reason when checking for multiple failure reasons" in {
      val eligibilityResponseJson =
        TtpJsonResponses.ttpEligibilityCallJson(
          TaxRegime.Simp,
          TdAll.notEligibleEligibilityPass,
          TdAll.eligibleEligibilityRules.copy(allChargeTypeAssessmentsFailed = Some(true)),
          regimeDigitalCorrespondence = true,
          assessmentCategoryInfo = Seq(
            AssessmentCategoryInfo(
              AssessmentCategory.Standard,
              TdAll.assessmentEligibilityRules.copy(isMoreThanMaxDebtAllowance = true)
            )
          )
        )

      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Simp.Mobile)()
      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Simp)(eligibilityResponseJson)
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(
          Origins.Simp.Mobile,
          eligibilityRules = TdAll.eligibleEligibilityRules.copy(allChargeTypeAssessmentsFailed = Some(true))
        )
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      // if multiple reasons are detected then the generic ineligible page would have been shown
      redirectLocation(result) shouldBe Some(PageUrls.simpDebtTooLargeUrl)
      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Simp)

      AuditConnectorStub.verifyEventAudited(
        "EligibilityCheck",
        Json
          .parse(
            s"""
               |{
               |  "eligibilityResult" : "ineligible",
               |  "enrollmentReasons": "did not pass eligibility check",
               |  "noEligibilityReasons": 1,
               |  "eligibilityReasons" : [ "isMoreThanMaxDebtAllowance" ],
               |  "origin": "Mobile",
               |  "taxType": "Simp",
               |  "taxDetail": { "nino": "QQ123456A" },
               |  "authProviderId": "authId-999",
               |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
               |  "chargeTypeAssessment" : [],
               |  "futureChargeLiabilitiesExcluded": false
               |}
               |""".stripMargin
          )
          .as[JsObject]
      )
    }

    "throw an error is dmSpecialOfficeProcessingRequiredCDCS is received for a tax regime that is not SA" in {
      val taxRegimesAndOrigins = TaxRegime.values.map { taxRegime =>
        val origin = taxRegime match {
          case TaxRegime.Epaye => Origins.Epaye.Bta
          case TaxRegime.Vat   => Origins.Vat.Bta
          case TaxRegime.Sa    => Origins.Sa.Bta
          case TaxRegime.Simp  => Origins.Simp.Pta
        }
        taxRegime -> origin
      }

      taxRegimesAndOrigins.filter(p => p._1 !== TaxRegime.Sa).foreach { case (taxRegime, origin) =>
        withClue(s"For tax regime ${taxRegime.entryName} and origin ${origin.toString}: ") {
          val eligibilityResponseJson =
            TtpJsonResponses.ttpEligibilityCallJson(
              taxRegime,
              TdAll.notEligibleEligibilityPass,
              TdAll.notEligibleDmSpecialOfficeProcessingRequiredCDCS,
              regimeDigitalCorrespondence = true
            )

          stubCommonActions()
          EssttpBackend.DetermineTaxId.findJourney(origin)()
          Ttp.Eligibility.stubRetrieveEligibility(taxRegime)(eligibilityResponseJson)
          EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
            TdAll.journeyId,
            JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequiredCDCS`(origin)
          )

          val error = intercept[Exception](controller.determineEligibility(fakeRequest).futureValue)
          error.getMessage should include(
            s"dmSpecialOfficeProcessingRequiredCDCS ineligibility reason not relevant to ${taxRegime.entryName}"
          )
        }
      }

    }

    "Eligible for Epaye: should redirect to determine assessment category and send an audit event" - {
      allCombinationOfTwoBooleanOptions.foreach { combo =>
        val maybeChargeIsInterestBearingCharge = combo._1
        val maybeChargeUseChargeReference      = combo._2
        s"where 'isInterestBearingCharge' field is ${maybeChargeIsInterestBearingCharge.toString} " +
          s"and 'useChargeReference' is ${maybeChargeUseChargeReference.toString}" in {

            val eligibilityCheckResponseJson         = TtpJsonResponses.ttpEligibilityCallJson(
              TaxRegime.Epaye,
              regimeDigitalCorrespondence = true,
              maybeChargeIsInterestBearingCharge = maybeChargeIsInterestBearingCharge,
              maybeChargeUseChargeReference = maybeChargeUseChargeReference
            )
            // for audit event
            val eligibilityCheckResponseJsonAsPounds = TtpJsonResponses.ttpEligibilityCallJson(
              TaxRegime.Epaye,
              poundsInsteadOfPence = true,
              regimeDigitalCorrespondence = true,
              maybeChargeIsInterestBearingCharge = maybeChargeIsInterestBearingCharge,
              maybeChargeUseChargeReference = maybeChargeUseChargeReference
            )

            stubCommonActions()
            EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()
            Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Epaye)(eligibilityCheckResponseJson)
            EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
              TdAll.journeyId,
              JourneyJsonTemplates.`Eligibility Checked - Eligible`()
            )

            val result = controller.determineEligibility(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(
              routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
            )

            Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Epaye)

            EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
              journeyId = TdAll.journeyId,
              expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
                TdAll.eligibleEligibilityPass,
                TdAll.eligibleEligibilityRules,
                Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
                TaxRegime.Epaye,
                RegimeDigitalCorrespondence(value = true),
                maybeChargeIsInterestBearingCharge,
                maybeChargeUseChargeReference
              )
            )(using testOperationCryptoFormat)

            AuditConnectorStub.verifyEventAudited(
              "EligibilityCheck",
              Json
                .parse(
                  s"""
                 |{
                 |  "eligibilityResult" : "eligible",
                 |  "noEligibilityReasons": 0,
                 |  "origin": "Bta",
                 |  "taxType": "Epaye",
                 |  "taxDetail": {
                 |    "employerRef": "864FZ00049",
                 |    "accountsOfficeRef": "123PA44545546"
                 |  },
                 |  "authProviderId": "authId-999",
                 |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                 |  "regimeDigitalCorrespondence": true,
                 |  "futureChargeLiabilitiesExcluded": false,
                 |  "chargeTypeAssessment" : ${(Json
                      .parse(eligibilityCheckResponseJsonAsPounds)
                      .as[JsObject] \ "chargeTypeAssessments" \ 0 \ "chargeTypeAssessment").get.toString}
                 |}
                 |""".stripMargin
                )
                .as[JsObject]
            )
          }
      }
    }

    "Eligible for Vat: should redirect to determine assessment category and send an audit event" in {
      val eligibilityCheckResponseJson         = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Vat,
        regimeDigitalCorrespondence = true,
        maybeChargeBeforeMaxAccountingDate = Some(true)
      )
      // for audit event
      val eligibilityCheckResponseJsonAsPounds = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Vat,
        poundsInsteadOfPence = true,
        regimeDigitalCorrespondence = true,
        maybeChargeBeforeMaxAccountingDate = Some(true)
      )

      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)()
      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Vat)(eligibilityCheckResponseJson)
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Eligible`()
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
      )

      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Vat)

      EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
        journeyId = TdAll.journeyId,
        expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
          TdAll.eligibleEligibilityPass,
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          TaxRegime.Vat,
          RegimeDigitalCorrespondence(value = true),
          chargeChargeBeforeMaxAccountingDate = Some(true)
        )
      )(using testOperationCryptoFormat)

      AuditConnectorStub.verifyEventAudited(
        "EligibilityCheck",
        Json
          .parse(
            s"""
             |{
             |  "eligibilityResult" : "eligible",
             |  "noEligibilityReasons": 0,
             |  "origin": "Bta",
             |  "taxType": "Vat",
             |  "taxDetail": {
             |    "vrn":"101747001"
             |  },
             |  "authProviderId": "authId-999",
             |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
             |  "regimeDigitalCorrespondence": true,
             |  "futureChargeLiabilitiesExcluded": false,
             |  "chargeTypeAssessment" : ${(Json
                .parse(eligibilityCheckResponseJsonAsPounds)
                .as[JsObject] \ "chargeTypeAssessments" \ 0 \ "chargeTypeAssessment").get.toString}
             |}
             |""".stripMargin
          )
          .as[JsObject]
      )
    }

    "Eligible for Sa: should redirect to determine assessment category and send an audit event" in {
      val eligibilityCheckResponseJson         = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Sa,
        regimeDigitalCorrespondence = true,
        maybeChargeBeforeMaxAccountingDate = Some(true),
        eligibilityRules = TdAll.eligibleEligibilityRules.copy(noMtditsaEnrollment = Some(false))
      )
      // for audit event
      val eligibilityCheckResponseJsonAsPounds = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Sa,
        poundsInsteadOfPence = true,
        regimeDigitalCorrespondence = true,
        maybeChargeBeforeMaxAccountingDate = Some(true),
        eligibilityRules = TdAll.eligibleEligibilityRules.copy(noMtditsaEnrollment = Some(false))
      )

      stubCommonActions(authNino = Some("QQ123456A"), authAllEnrolments = Some(Set(TdAll.mtdEnrolment)))
      EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)()
      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Sa)(eligibilityCheckResponseJson)
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Eligible`()
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
      )

      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Sa)

      EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
        journeyId = TdAll.journeyId,
        expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
          TdAll.eligibleEligibilityPass,
          TdAll.eligibleEligibilityRules.copy(noMtditsaEnrollment = Some(false)),
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          TaxRegime.Sa,
          RegimeDigitalCorrespondence(value = true),
          chargeChargeBeforeMaxAccountingDate = Some(true)
        )
      )(using testOperationCryptoFormat)

      AuditConnectorStub.verifyEventAudited(
        "EligibilityCheck",
        Json
          .parse(
            s"""
             |{
             |  "eligibilityResult" : "eligible",
             |  "noEligibilityReasons": 0,
             |  "origin": "Bta",
             |  "taxType": "Sa",
             |  "taxDetail": {
             |    "utr": "1234567895"
             |  },
             |  "authProviderId": "authId-999",
             |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
             |  "regimeDigitalCorrespondence": true,
             |  "futureChargeLiabilitiesExcluded": false,
             |  "chargeTypeAssessment" : ${(Json
                .parse(eligibilityCheckResponseJsonAsPounds)
                .as[JsObject] \ "chargeTypeAssessments" \ 0 \ "chargeTypeAssessment").get.toString}
             |}
             |""".stripMargin
          )
          .as[JsObject]
      )
    }

    "Eligible for SIMP: should redirect to determine assessment category and send an audit event" in {
      val eligibilityCheckResponseJson         = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Simp,
        regimeDigitalCorrespondence = true,
        maybeChargeBeforeMaxAccountingDate = Some(true)
      )
      // for audit event
      val eligibilityCheckResponseJsonAsPounds = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Simp,
        poundsInsteadOfPence = true,
        regimeDigitalCorrespondence = true,
        maybeChargeBeforeMaxAccountingDate = Some(true)
      )

      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Simp.Pta)()
      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Simp)(eligibilityCheckResponseJson)
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Eligible`()
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
      )

      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Simp)

      EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
        journeyId = TdAll.journeyId,
        expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
          TdAll.eligibleEligibilityPass,
          TdAll.eligibleEligibilityRules,
          Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
          TaxRegime.Simp,
          RegimeDigitalCorrespondence(value = true),
          chargeChargeBeforeMaxAccountingDate = Some(true)
        )
      )(using testOperationCryptoFormat)

      AuditConnectorStub.verifyEventAudited(
        "EligibilityCheck",
        Json
          .parse(
            s"""
             |{
             |  "eligibilityResult" : "eligible",
             |  "noEligibilityReasons": 0,
             |  "origin": "Pta",
             |  "taxType": "Simp",
             |  "taxDetail": {
             |    "nino": "QQ123456A"
             |  },
             |  "authProviderId": "authId-999",
             |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
             |  "regimeDigitalCorrespondence": true,
             |  "futureChargeLiabilitiesExcluded": false,
             |  "chargeTypeAssessment" : ${(Json
                .parse(eligibilityCheckResponseJsonAsPounds)
                .as[JsObject] \ "chargeTypeAssessments" \ 0 \ "chargeTypeAssessment").get.toString}
             |}
             |""".stripMargin
          )
          .as[JsObject]
      )
    }

    "Eligibility already determined should route user to determine assessment category is and not update backend again" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
      )
      EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
    }

    "Redirect to landing page if journey is in started state" in {
      stubCommonActions()
      EssttpBackend.StartJourney.findJourney()

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.epayeLandingPageUrl)
      EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
    }

    List(
      Origins.Epaye.Bta -> TaxRegime.Epaye,
      Origins.Vat.Bta   -> TaxRegime.Vat,
      Origins.Sa.Bta    -> TaxRegime.Sa,
      Origins.Simp.Pta  -> TaxRegime.Simp
    ).foreach { case (origin, taxRegime) =>
      s"[${taxRegime.entryName}] throw an error when ttp eligibility call returns a legitimate error (not a 422)" in {
        stubCommonActions(authNino = Some("QQ123456A"), authAllEnrolments = Some(Set(TdAll.mtdEnrolment)))
        EssttpBackend.DetermineTaxId.findJourney(origin)()
        Ttp.Eligibility.stubServiceUnavailableRetrieveEligibility()

        val error = intercept[Exception](controller.determineEligibility(fakeRequest).futureValue)
        error.getMessage should include(
          "The future returned an exception of type: uk.gov.hmrc.http.UpstreamErrorResponse"
        )

        Ttp.Eligibility.verifyTtpEligibilityRequests(taxRegime)
        EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
        AuditConnectorStub.verifyNoAuditEvent()
      }

      if (taxRegime === TaxRegime.Epaye || taxRegime === TaxRegime.Vat) {
        s"[${taxRegime.entryName}] Redirect to generic ineligible call us page when ttp eligibility call returns a 422 response" in {
          stubCommonActions()
          EssttpBackend.DetermineTaxId.findJourney(origin)()
          Ttp.Eligibility.stub422RetrieveEligibility()

          val result = controller.determineEligibility(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER

          val expectedPageUrl: String = taxRegime match {
            case TaxRegime.Epaye => PageUrls.payeNotEligibleUrl
            case TaxRegime.Vat   => PageUrls.vatNotEligibleUrl
            case TaxRegime.Sa    => PageUrls.saNotEligibleUrl
            case TaxRegime.Simp  => PageUrls.simpNotEligibleUrl
          }
          redirectLocation(result) shouldBe Some(expectedPageUrl)

          Ttp.Eligibility.verifyTtpEligibilityRequests(taxRegime)
          EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
          AuditConnectorStub.verifyNoAuditEvent()
        }
      } else {
        s"[${taxRegime.entryName}] Return a technical error  when ttp eligibility call returns a 422 response" in {
          stubCommonActions(authNino = Some("QQ123456A"), authAllEnrolments = Some(Set(TdAll.mtdEnrolment)))
          EssttpBackend.DetermineTaxId.findJourney(origin)()
          Ttp.Eligibility.stub422RetrieveEligibility()

          val result = intercept[UpstreamErrorResponse](await(controller.determineEligibility(fakeRequest)))
          result.statusCode shouldBe UNPROCESSABLE_ENTITY

          Ttp.Eligibility.verifyTtpEligibilityRequests(taxRegime)
          EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
          AuditConnectorStub.verifyNoAuditEvent()
        }
      }

    }

    "VAT user with a debt below the minimum amount and debt too old should be redirected to generic ineligible page" in {
      val assessmentCategoryInfo       =
        Seq(
          AssessmentCategoryInfo(
            AssessmentCategory.Standard,
            TdAll.notEligibleIsLessThanMinDebtAllowance.copy(chargesOverMaxDebtAge = Some(true))
          )
        )
      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Vat,
        TdAll.notEligibleEligibilityPass,
        TdAll.eligibleEligibilityRules,
        assessmentCategoryInfo,
        regimeDigitalCorrespondence = true
      )

      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Vat)(eligibilityCheckResponseJson)
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)()
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons - debt too low and old`(
          Origins.Vat.Bta
        )
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.vatDebtTooSmallUrl)
    }
    "VAT user with multiple reasons from EligibilityRules redirect to " in {

      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Vat,
        TdAll.notEligibleEligibilityPass,
        TdAll.notEligibleMultipleReasons,
        Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
        regimeDigitalCorrespondence = true
      )

      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Vat)(eligibilityCheckResponseJson)
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)()
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons - Eligibility Reasons`(
          Origins.Vat.Bta
        )
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.vatNoDueDatesReachedUrl)
    }

    "VAT user with multiple reasons from AssessmentEligibilityRules redirect to " in {

      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Vat,
        TdAll.notEligibleEligibilityPass,
        TdAll.eligibleEligibilityRules,
        Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleMultipleReasonsAssessment)),
        regimeDigitalCorrespondence = true
      )

      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Vat)(eligibilityCheckResponseJson)
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)()
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons - Assessment Eligibility Reasons`(
          Origins.Vat.Bta
        )
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.vatNotEligibleUrl)
    }

    "VAT user with multiple reasons from EligibilityRules and AssessmentEligibilityRules redirect to " in {

      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Vat,
        TdAll.notEligibleEligibilityPass,
        TdAll.notEligibleDmSpecialOfficeProcessingRequired,
        Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.notEligibleDisallowedChargeLockTypes)),
        regimeDigitalCorrespondence = true
      )

      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Vat)(eligibilityCheckResponseJson)
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)()
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons - both lists`(
          Origins.Vat.Bta
        )
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.vatNotEligibleUrl)
    }

    "EPAYE user with a debt below the minimum amount and debt too old should be redirected to debt too low ineligible page" in {
      val assessmentCategoryInfo       =
        Seq(
          AssessmentCategoryInfo(
            AssessmentCategory.Standard,
            TdAll.notEligibleIsLessThanMinDebtAllowance.copy(chargesOverMaxDebtAge = Some(true))
          )
        )
      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Epaye,
        TdAll.notEligibleEligibilityPass,
        TdAll.eligibleEligibilityRules,
        assessmentCategoryInfo,
        regimeDigitalCorrespondence = true
      )

      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Epaye)(eligibilityCheckResponseJson)
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons - debt too low and old`()
      )

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.epayeDebtTooSmallUrl)
    }

    "SA user with IR-SA enrolment but no NINO found, should be directed to generic ineligible page" in {
      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Sa,
        TdAll.eligibleEligibilityPass,
        TdAll.eligibleEligibilityRules,
        regimeDigitalCorrespondence = true
      )

      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Sa)(eligibilityCheckResponseJson)
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)()
      EssttpBackend.EligibilityCheck
        .stubUpdateEligibilityResult(TdAll.journeyId, JourneyJsonTemplates.`Eligibility Checked - Eligible`())

      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.saNotEligibleUrl)
    }

    CustomerTypes.values
      .filter(_.value != CustomerTypes.MTDITSA.value)
      .map(Some(_))
      .appended(None)
      .foreach { customerType =>
        s"SA user (customer type = ${customerType.toString}) with IR-SA enrolment, NINO found but no HMRC-MTD-IT enrolment should be directed " +
          "to determine assessment category" in {
            val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
              TaxRegime.Sa,
              TdAll.eligibleEligibilityPass,
              TdAll.eligibleEligibilityRules,
              regimeDigitalCorrespondence = true,
              maybeCustomerType = customerType
            )

            Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Sa)(eligibilityCheckResponseJson)
            stubCommonActions(authNino = Some("QQ123456A"), authAllEnrolments = Some(Set(TdAll.saEnrolment)))
            EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)()
            EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
              TdAll.journeyId,
              JourneyJsonTemplates.`Eligibility Checked - Eligible`()
            )

            val result = controller.determineEligibility(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(
              routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
            )

            EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
              journeyId = TdAll.journeyId,
              expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
                TdAll.eligibleEligibilityPass,
                TdAll.eligibleEligibilityRules,
                Seq(AssessmentCategoryInfo(AssessmentCategory.Standard, TdAll.assessmentEligibilityRules)),
                TaxRegime.Sa,
                RegimeDigitalCorrespondence(value = true),
                maybeIndividalDetails =
                  customerType.map(c => IndividualDetails(None, None, None, None, None, Some(c), None))
              )
            )(using testOperationCryptoFormat)

          }
      }

    "SA user (customer type = MTD-ITSA) with IR-SA enrolment, NINO found but no HMRC-MTD-IT enrolment should be directed " +
      "to Sign up for Making Tax Digital page with content in English" in {
        val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
          TaxRegime.Sa,
          TdAll.eligibleEligibilityPass,
          TdAll.eligibleEligibilityRules,
          regimeDigitalCorrespondence = true,
          maybeCustomerType = Some(CustomerTypes.MTDITSA)
        )

        Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Sa)(eligibilityCheckResponseJson)
        stubCommonActions(authNino = Some("QQ123456A"), authAllEnrolments = Some(Set(TdAll.saEnrolment)))
        EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)()
        EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
          TdAll.journeyId,
          JourneyJsonTemplates.`Eligibility Checked - Eligible - No HMRC-MTD-IT enrolment`()
        )

        val result = controller.determineEligibility(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.signupMtdUrl)

        val followRedirectResult = route(app, fakeRequestWithPath(PageUrls.signupMtdUrl)).fold(
          fail("Redirect route could not be handled")
        )(identity)
        status(followRedirectResult) shouldBe Status.OK

        val page = Jsoup.parse(contentAsString(followRedirectResult))

        ContentAssertions.commonPageChecks(
          page,
          expectedH1 = "Sign up for Making Tax Digital for Income Tax to use this service",
          shouldBackLinkBePresent = false,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(TaxRegime.Sa)
        )
        val expectedLeadingP1 =
          "You must sign up for Making Tax Digital for Income Tax before you can set up a Self Assessment payment plan online."
        val expectedP2        =
          "If you’ve already signed up, sign in with the Government Gateway user ID that has your enrolment."

        page.select(".govuk-body").asScala.toList(0).text() shouldBe expectedLeadingP1
        page.select(".govuk-body").asScala.toList(1).text() shouldBe expectedP2

        ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Sa, Languages.English)
      }

    "SA user with IR-SA enrolment (customer type = MTD-ITSA), NINO found but no HMRC-MTD-IT enrolment should be directed to " +
      "Sign up for Making Tax Digital page with content in Welsh" in {
        val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
          TaxRegime.Sa,
          TdAll.eligibleEligibilityPass,
          TdAll.eligibleEligibilityRules,
          regimeDigitalCorrespondence = true,
          maybeCustomerType = Some(CustomerTypes.MTDITSA)
        )

        Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Sa)(eligibilityCheckResponseJson)
        stubCommonActions(authNino = Some("QQ123456A"), authAllEnrolments = Some(Set(TdAll.saEnrolment)))
        EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)()
        EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
          TdAll.journeyId,
          JourneyJsonTemplates.`Eligibility Checked - Eligible - No HMRC-MTD-IT enrolment`()
        )

        val followRedirectResult = route(app, fakeRequestWithPath(PageUrls.signupMtdUrl).withLangWelsh()).fold(
          fail("Redirect route could not be handled")
        )(identity)
        status(followRedirectResult) shouldBe Status.OK

        val page = Jsoup.parse(contentAsString(followRedirectResult))

        ContentAssertions.commonPageChecks(
          page,
          expectedH1 =
            "Cofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm er mwyn defnyddio’r gwasanaeth hwn",
          shouldBackLinkBePresent = false,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(TaxRegime.Sa),
          language = Languages.Welsh
        )
        val expectedLeadingP1 =
          "Mae’n rhaid i chi gofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm cyn i chi allu trefnu cynllun talu ar gyfer Hunanasesiad ar-lein."
        val expectedP2        =
          "Os ydych chi eisoes wedi cofrestru, mae’n rhaid i chi fewngofnodi gan ddefnyddio’r Dynodydd Defnyddiwr (ID) Porth y Llywodraeth sydd â’ch cofrestriad."

        page.select(".govuk-body").asScala.toList(0).text() shouldBe expectedLeadingP1
        page.select(".govuk-body").asScala.toList(1).text() shouldBe expectedP2

        ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Sa, Languages.Welsh)
      }

  }

  "Determine eligibility should prevent eligibility checks" - {

    "when SA is enabled and the journey has been marked to go to the legacy SA service" in {
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta, redirectToLegacySaService = Some(true))()

      val result = controller.determineEligibility(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9063/pay-what-you-owe-in-instalments/arrangement/determine-eligibility"
      )
    }

  }

}

class DetermineEligibilityControllerSimpDisabledSpec extends ItSpec, CombinationsHelper {

  override lazy val configOverrides: Map[String, Any] = Map(
    "features.simp" -> false
  )

  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]

  "Determine eligibility should prevent eligibility checks" - {

    "when SIMP is disabled and the journey is for SIMP" in {
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Simp.Pta)()

      val result = controller.determineEligibility(fakeRequest)
      status(result) shouldBe OK
      val doc    = Jsoup.parse(contentAsString(result))

      doc.select("h1").text shouldBe "Sorry, the service is unavailable"
    }

  }

}

class DetermineEligibilityControllerSaDisabledSpec extends ItSpec, CombinationsHelper {

  override lazy val configOverrides: Map[String, Any] = Map(
    "features.sa" -> false
  )

  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]

  "Determine eligibility should prevent eligibility checks" - {

    "when SA is disabled and the journey is for SA" in {
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.DetachedUrl)()

      val result = controller.determineEligibility(fakeRequest)
      status(result) shouldBe OK
      val doc    = Jsoup.parse(contentAsString(result))

      doc.select("h1").text shouldBe "Sorry, the service is unavailable"
    }

  }

}
