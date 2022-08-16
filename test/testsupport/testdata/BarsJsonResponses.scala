/*
 * Copyright 2022 HM Revenue & Customs
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

object BarsJsonResponses {

  object ValidateJson {
    val success: String =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeSupportsDirectDebit": "yes",
        |  "sortCodeSupportsDirectCredit": "no",
        |  "iban": "GB21BARC20710244344655",
        |  "sortCodeBankName": "BARCLAYS BANK UK PLC"
        |}""".stripMargin

    val accountNumberNotWellFormatted: String =
      """{
        |  "accountNumberIsWellFormatted": "no",
        |  "nonStandardAccountDetailsRequiredForBacs": "yes",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeSupportsDirectDebit": "no",
        |  "sortCodeSupportsDirectCredit": "yes",
        |  "sortCodeBankName": "Nottingham Building Society"
        |}""".stripMargin

    val sortCodeNotPresentOnEiscd: String =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "sortCodeIsPresentOnEISCD": "no"
        |}""".stripMargin

    val sortCodeDoesNotSupportsDirectDebit: String =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeSupportsDirectDebit": "no",
        |  "sortCodeSupportsDirectCredit": "no",
        |  "iban": "GB21BARC20670544311611",
        |  "sortCodeBankName": "BARCLAYS BANK UK PLC"
        |}""".stripMargin
  }

  object VerifyJson {
    val success: String =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "yes",
        |    "nameMatches": "yes",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "no",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val accountExistsError =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "error",
        |    "nameMatches": "indeterminate",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "no",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val accountDoesNotExist =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "no",
        |    "nameMatches": "yes",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "no",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val nameMatchesError =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "indeterminate",
        |    "nameMatches": "error",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "no",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val nameDoesNotMatch =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "indeterminate",
        |    "nameMatches": "no",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "no",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val undocumentedError =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeBankName": "BARCLAYS BANK PLC",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "accountExists": "indeterminate",
        |  "nameMatches": "indeterminate",
        |  "sortCodeSupportsDirectDebit": "yes",
        |  "sortCodeSupportsDirectCredit": "no"
        |}""".stripMargin

  }

}
