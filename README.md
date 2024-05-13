# essttp-frontend

---

This repository contains the frontend microservice for eSSTTP (Enhanced Self Service Time To Pay). 
It is built using Scala (2.13) and the Play framework (2.8). We use linting tools such as WartRemover and Sclariform.
This microservice allows users to set up an arrangement and recurring direct debit to repay any debts they may have with HMRC.

Current tax regimes supported:
* PAYE
* VAT (wip)

---

## Contents:

* [Running the service locally](https://github.com/hmrc/essttp-frontend#running-locally)
* [Running tests](https://github.com/hmrc/essttp-frontend#running-tests)
* [Accessing the service](https://github.com/hmrc/essttp-frontend#accessing-the-service)
* [Bars stub data](https://github.com/hmrc/essttp-frontend#bars-stub-data)
* [Navigating through quickly with Tampermonkey](https://github.com/hmrc/essttp-frontend#navigating-through-quickly-with-tampermonkey)

---

### Running locally

Start up supporting services with `sm2 --start ESSTTP`

You can run the service locally using sbt: `sbt run`

To run with test endpoints enabled: `sbt runTestOnly`

If running locally, the service runs on port `9215`

---

### Running tests

You can run the unit/integration tests locally using sbt: `sbt test`

To run a specific spec, run `sbt 'testOnly *<SpecName>'`, e.g. `sbt 'testOnly *LandingPageControllerSpec'`

---

### Accessing the service

The standard entry point for the service is to call the appropriate start endpoint in [essttp-backend](https://github.com/hmrc/essttp-backend)
and redirect to the given `nextUrl` in the response. The `nextUrl` will point to the appropriate landing page in
this service.

However, you will need a valid government gateway account and various other data in ETMP to actually be a valid user.

In pre-production environments, there is a test only page that can be used to start journeys.

| Environment     | Url                                                                                      |
|-----------------|------------------------------------------------------------------------------------------|
| **Local**       | http://localhost:9215/set-up-a-payment-plan/test-only/tax-regime                         |
| **Development** | https://www.development.tax.service.gov.uk/set-up-a-payment-plan/test-only/tax-regime    |
| **QA**          | https://www.qa.tax.service.gov.uk/set-up-a-payment-plan/test-only/tax-regime             |
| **Staging**     | https://www.staging.tax.service.gov.uk/set-up-a-payment-plan/test-only/tax-regime        |

---

### BARs stub data
Use one of the following name/sortCode/accountNumber combinations on the 'Enter Bank Details' page
to get the desired behaviour (e.g. `Teddy Dickson and 207102 and 44344655` to successfully get past BARs validation)

| Sort Code | Account Number | Account Name         | BARs Response                     | Account Type |
|-----------|----------------|----------------------|-----------------------------------|--------------|
| 207102    | 44311655       | Teddy Dickson        | OK                                | Personal     |
| 207102    | 44311655       | Teddy Bear           | nameMatches - NO                  | Personal     |
| 207102    | 86563611       | Lambent Illumination | OK                                | Business     |
| 207102    | 86563611       | Lambert and Butler   | nameMatches - NO                  | Business     |
| 609593    | 44311611       | any                  | accountNumberIsWellFormatted - NO | any          |
| 206705    | 44311611       | any                  | sortCodeSupportsDirectDebit - NO  | any          |
| 309696    | 44311611       | any                  | sortCodeIsPresentOnEISCD - NO     | any          |

see here for more BARs stub data https://github.com/hmrc/bank-account-reputation-stub

---

### MainTrans codes for SA
In SA we look up the MainTrans code received in ChargeReference in the table below and display the corresponding charge
type in the 'You Bill' page. If the code does not have a corresponding charge description, the user is take to the 
generic sa ineligibility kick out page.

| Main Trans code | English description                            | Welsh description                                   |
|-----------------|------------------------------------------------|-----------------------------------------------------|
| 5060            | Revenue assessment                             | Asesiad refeniw                                     |
|                 | Balancing Charge Credit                        | Credyd cost mantoli                                 |
| 4910            | Balancing payment                              | Taliad mantoli                                      |
| 5050            | Revenue Determination                          | Penderfyniad refeniw                                |
| 4950            | Daily penalty                                  | Cosb ddyddiol                                       |
| 4990            | Partnership daily penalty                      | Cosb ddyddiol i bartneriaeth                        |
| 5210            | Enquiry Amendment                              | Diwygiad ymholiad                                   |
| 4920            | First payment on account                       | Taliad ar gyfrif cyntaf                             |
| 4930            | Second payment on account                      | Ail daliad ar gyfrif                                |
| 5190            | Enquiry Amendment                              | Diwygiad ymholiad                                   |
| 4960            | 6 month late filing penalty                    | Cosb am gyflwyno 6 mis yn hwyr                      |
| 4970            | 12 month late filing penalty                   | Cosb am gyflwyno 12 mis yn hwyr                     |
| 5010            | Partnership 6 months late filing penalty       | Cosb i bartneriaeth am gyflwyno 6 mis yn hwyr       |
| 5020            | Partnership 12 months late filing penalty      | Cosb i bartneriaeth am gyflwyno 12 mis yn hwyr      |
| 6010            | Late Payment interest                          | Llog ar daliadau hwyr                               |
| 5110            | 30 days late payment penalty                   | Cosb am dalu 30 diwrnod yn hwyr                     |
| 5120            | 6 months late payment penalty                  | Cosb am dalu 6 mis yn hwyr                          |
| 5130            | 12 months late payment penalty                 | Cosb am dalu 12 mis yn hwyr                         |
| 5080            | Penalty                                        | Cosb                                                |
| 5100            | Amount no longer included in Tax Code          | Swm sydd heb ei gynnwys yn y Cod Treth mwyach       |
| 5060            | Revenue assessment                             | Asesiad refeniw                                     |
| 5070            | Repayment supplement                           | Atodiad ad-daliad                                   |
| 5140            | First penalty for late tax return              | Cosb gyntaf ar gyfer Ffurflen Dreth hwyr            |
| 4940            | First penalty for late tax return              | Cosb gyntaf ar gyfer Ffurflen Dreth hwyr            |
| 5150            | Second penalty for late tax return             | Ail gosb ar gyfer Ffurflen Dreth hwy                |
|                 | Late filing penalty                            | Cosb am gyflwyno'n hwyr                             |
| 5160            | First penalty for late partnership tax return  | Cosb gyntaf ar gyfer Ffurflen Dreth Partneriae hwyr |
| 4980            | First penalty for late partnership tax return  | Cosb gyntaf ar gyfer Ffurflen Dreth Partneriae hwyr |
| 5170            | Second penalty for late partnership tax return | Ail gosb ar gyfer Ffurflen Dreth Partneriaeth hwr   |
|                 | Partnership late filing penalty                | Cosb i bartneriaeth am gyflwyno'n hwyr              |
| 5200            | Tax return amendment                           | Diwygiad i'r Ffurflen Dreth                         |
| 5071            | Repayment                                      | Ad-daliad                                           |
| 5180            | Enquiry amendment                              | Diwygiad ymholiad                                   |
| 5090            | Amount no longer included in tax code          | Swm sydd heb ei gynnwys yn y cod treth mwyach       |
| 5030            | First surcharge for late payment               | Gordal cyntaf ar gyfer taliad hwyr                  |
| 5040            | Second surcharge for late payment              | Ail ordal ar gyfer taliad hwyr                      |
| 5073            | Transfer to OAS                                | Trosglwyddo i OAS                                   |
| 4000            | HMRC adjustment                                | Addasiad gan CThEF                                  |
| 4001            | HMRC adjustment                                | Addasiad gan CThEF                                  |
| 4002            | HMRC adjustment                                | Addasiad gan CThEF                                  |
| 4003            | HMRC adjustment                                | Addasiad gan CThEF                                  |
| 4026            | ITSA Penalty Interest                          | Llog ar gosb ITSA                                   |


### Navigating through quickly with Tampermonkey
A script has been created to be used with [Tampermonkey](https://www.tampermonkey.net/) to enable fast navigation through 
the service to make testing easier. To make use of it, install the Tampermonkey browser extension on your browser and
then install [this script](https://raw.githubusercontent.com/hmrc/essttp-frontend/main/tampermonkey/quickJourney.js). After
installation, a green "Quick submit" button will be visible near the top-left of each page in the service. Clicking this 
button will autocomplete the inputs on the page (including the test-only start page) and automatically click the continue 
button on that page.


---

### Licence
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
