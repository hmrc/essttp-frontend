package testsupport.testdata

import actions.EnrolmentDef
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

object TdAll {

  private val `IR-PAYE-TaxOfficeNumber`: EnrolmentDef = EnrolmentDef(enrolmentKey = "IR-PAYE", identifierKey = "TaxOfficeNumber")
  private val `IR-PAYE-TaxOfficeReference`: EnrolmentDef = EnrolmentDef(enrolmentKey = "IR-PAYE", identifierKey = "TaxOfficeReference")

  val payeEnrolment: Enrolment = Enrolment(
    key = "IR-PAYE",
    identifiers = List(
      EnrolmentIdentifier(`IR-PAYE-TaxOfficeNumber`.identifierKey, "123"),
      EnrolmentIdentifier(`IR-PAYE-TaxOfficeReference`.identifierKey, "456")
    ),
    state = "Activated",
    delegatedAuthRule = None
  )

  val unactivePayeEnrolment: Enrolment = payeEnrolment.copy(state = "Not Activated")
}
