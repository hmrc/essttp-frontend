
# essttp-frontend

Frontend repo for essttp journeys

# emulate start journey from gov-uk
This will make a get request with referer value of "github.com".
That referer was configured on test environments to represent a journey which is started from a gov-uk.
Obviously we can't hook those urls on the gov.uk sites and for that reason we used that test domain to test 
those cases.

http://localhost:9215/set-up-a-payment-plan/govuk/epaye/start

https://www.development.tax.service.gov.uk/set-up-a-payment-plan/govuk/epaye/start

https://www.qa.tax.service.gov.uk/set-up-a-payment-plan/govuk/epaye/start

https://www.staging.tax.service.gov.uk/set-up-a-payment-plan/govuk/epaye/start


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

### Licence
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
