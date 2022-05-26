
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


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
