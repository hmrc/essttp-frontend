# essttp-frontend

---

This repository contains the frontend microservice for eSSTTP (Enhanced Self Service Time To Pay). 
It is built using Scala (2.12) and the Play framework (2.8). We use linting tools such as WartRemover and Sclariform.
This microservice allows users to set up an arrangement and recurring direct debit to repay any debts they may have with HMRC.

Current tax regimes supported:
* PAYE
* VAT (wip)

N.B.: any start journey endpoints should end with `/start` in the url in order for the continueUrl if logging is required to be correct.

---

## Contents:

* [Running the service locally](https://github.com/hmrc/essttp-frontend#running-locally)
* [Running tests](https://github.com/hmrc/essttp-frontend#running-tests)
* [Accessing the service](https://github.com/hmrc/essttp-frontend#accessing-the-service)
* [Bars stub data](https://github.com/hmrc/essttp-frontend#bars-stub-data)

---

### Running locally

You can run the service locally using sbt: `sbt run`

To run with test endpoints enabled: `sbt runTestOnly`

If running locally, the service runs on port `9215`

---

### Running tests

You can run the unit/integration tests locally using sbt: `sbt test`

To run a specific spec, run `sbt 'testOnly *<SpecName>'`, e.g. `sbt 'testOnly *LandingPageControllerSpec'`

---

### Accessing the service

The standard entry point for the service is https://www.tax.service.gov.uk/set-up-a-payment-plan

However, you will need a valid government gateway account and various other data in ETMP to actually be a valid user.

In pre-production environments, there is a test only page that can be used to start journeys.

| Environment | Url                                                                                      |
|-------------|------------------------------------------------------------------------------------------|
| **Local**       | http://localhost:9215/set-up-a-payment-plan/test-only/start-journey                      |
| **Development** | https://www.development.tax.service.gov.uk/set-up-a-payment-plan/test-only/start-journey |
| **QA**          | https://www.qa.tax.service.gov.uk/set-up-a-payment-plan/test-only/start-journey          |
| **Staging**     | https://www.staging.tax.service.gov.uk/set-up-a-payment-plan/test-only/start-journey     |

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

### Licence
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
