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

Go to `/set-up-a-payment-plan/test-only/email-verification-passcodes` when using email verification to get the passcode

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
In SA we look up the MainTrans code received in ChargeReference and display the corresponding charge
type in the 'You Bill' page. If the code does not have a corresponding charge description, the user is taken to the 
generic sa ineligibility kick out page. See the entire [table here](https://confluence.tools.tax.service.gov.uk/display/SSTTP/Your+Bill+translations)


### Navigating through quickly with Tampermonkey
A script has been created to be used with [Tampermonkey](https://www.tampermonkey.net/) to enable fast navigation through 
the service to make testing easier. To make use of it, install the Tampermonkey browser extension on your browser and
then install [this script](https://raw.githubusercontent.com/hmrc/essttp-frontend/main/tampermonkey/quickJourney.js). After
installation, a green "Quick submit" button will be visible near the top-left of each page in the service. Clicking this 
button will autocomplete the inputs on the page (including the test-only start page) and automatically click the continue 
button on that page.

### Internal auth token for TTP
TTP are protecting their API endpoints with internal auth. We present an `Authorization` header to our requests to them 
with the header value read from our config value with key `internal-auth.token`. By default, this is set to `valid-auth-token`.
To make this work locally, make sure `INTERNAL_AUTH` is running - it should be already running through this service's service manager
profile. Then run the curl command:
```
curl -i -X POST -H 'Content-Type: application/json'  -d '{
  "token": "valid-auth-token",         
  "principal": "essttp-frontend",
  "permissions": [{
    "resourceType": "time-to-pay-eligibility",
    "resourceLocation": "*",
    "actions": ["*"]
  }]
}' 'http://localhost:8470/test-only/token'
```


---

### Licence
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
