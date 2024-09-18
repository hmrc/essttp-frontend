// ==UserScript==
// @name         ESSTTP quick journey
// @namespace    http://tampermonkey.net/
// @version      3.0
// @description
// @author       achung
// @match        http*://*/set-up-a-payment-plan*
// @match        http*://*/email-verification*
// @downloadURL  https://raw.githubusercontent.com/hmrc/essttp-frontend/main/tampermonkey/quickJourney.js
// @updateURL    https://raw.githubusercontent.com/hmrc/essttp-frontend/main/tampermonkey/quickJourney.js
// ==/UserScript==

(function() {
    'use strict'
    document.body.appendChild(setup())
})()


function setup() {
    let panel = document.createElement('div')
    panel.style.position = 'absolute'
    panel.style.top = '50px'
    panel.style.lineHeight = '200%'
    panel.appendChild(createQuickButton())

    return panel
}

function createQuickButton() {
    let button = document.createElement('button')
    button.id='quickSubmit'

    if (!!document.getElementById('global-header')) {
        button.classList.add('button-start', 'govuk-!-display-none-print')
    } else {
        button.classList.add('govuk-button','govuk-!-display-none-print')
    }

    button.innerHTML = 'Quick Submit'
    button.onclick = () => continueJourney()

    return button
}



function getPasscode(){
    let url;
    if (window.location.hostname === 'localhost') {
        url = 'http://localhost:9215/set-up-a-payment-plan/test-only/email-verification-passcodes'
    } else {
        url = '/set-up-a-payment-plan/test-only/email-verification-passcodes'
    }
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("GET", url, false); // false for synchronous request.
    xmlHttp.send();
    let passcodes = JSON.parse(xmlHttp.responseText).passcodes
    return passcodes[passcodes.length-1].passcode
}

const currentPageIs = (path) => {
    return window.location.pathname.match(RegExp(path))
}

const clickContinue = () => {
    let continueButton = document.getElementById('continue');
    if(continueButton)
        continueButton.click()
    else
        document.getElementsByClassName('govuk-button')[0].click()
}

/* ########################      ESSTTP PAGES      ######################## */

const testOnlyTaxRegimePage = () => {
    if (currentPageIs('/set-up-a-payment-plan/test-only/tax-regime')) {
        document.getElementById('taxRegime').checked = true

        clickContinue()
    }
}

const testOnlyStartPageEpaye = () => {
    if (currentPageIs('/set-up-a-payment-plan/test-only/start-journey-epaye')) {
        document.getElementById('signInAs').checked = true
        document.getElementById('enrolments').checked = true
        document.getElementById('payeDebtTotalAmount').value = '10000'
        document.getElementById('interestAmount').value = '0'
        document.getElementById('payeTaxReference').value = ''
        document.getElementById('regimeDigitalCorrespondence').checked = true
        document.getElementById('emailAddressPresent').checked = true
        document.getElementById('origin-3').checked = true

        clickContinue()
    }
}

const testOnlyStartPageVat = () => {
    if (currentPageIs('/set-up-a-payment-plan/test-only/start-journey-vat')) {
        document.getElementById('signInAs').checked = true
        document.getElementById('enrolments-2').checked = true
        document.getElementById('vatDebtTotalAmount').value = '10000'
        document.getElementById('interestAmount').value = '0'
        document.getElementById('vatTaxReference').value = ''
        document.getElementById('regimeDigitalCorrespondence').checked = true
        document.getElementById('emailAddressPresent').checked = true
        document.getElementById('origin-3').checked = true

        clickContinue()
    }
}

const testOnlyStartPageSa = () => {
    if (currentPageIs('/set-up-a-payment-plan/test-only/start-journey-sa')) {
        document.getElementById('signInAs').checked = true
        document.getElementById('enrolments-3').checked = true
        document.getElementById('saDebtTotalAmount').value = '10000'
        document.getElementById('interestAmount').value = '0'
        document.getElementById('saTaxReference').value = ''
        document.getElementById('regimeDigitalCorrespondence').checked = true
        document.getElementById('emailAddressPresent').checked = true
        document.getElementById('origin-4').checked = true

        clickContinue()
    }
}

const testOnlyStartPageSia = () => {
    if (currentPageIs('/set-up-a-payment-plan/test-only/start-journey-sia')) {
        document.getElementById('signInAs').checked = true
        document.getElementById('enrolments-3').checked = true
        document.getElementById('siaDebtTotalAmount').value = '10000'
        document.getElementById('interestAmount').value = '0'
        document.getElementById('nino').value = ''
        document.getElementById('regimeDigitalCorrespondence').checked = true
        document.getElementById('emailAddressPresent').checked = true
        document.getElementById('origin-4').checked = true

        clickContinue()
    }
}

const landingPage = () => {
    if (currentPageIs('/set-up-a-payment-plan/epaye-payment-plan') ||
        currentPageIs('/set-up-a-payment-plan/vat-payment-plan') ||
        currentPageIs('/set-up-a-payment-plan/sa-payment-plan')) ||
        currentPageIs('/set-up-a-payment-plan/sia-payment-plan')) {
        clickContinue()
    }
}

const yourBill = () => {
    if (currentPageIs('/set-up-a-payment-plan/your-bill')) {
        clickContinue()
    }
}

const whyCannotPayInFull = () => {
    if (currentPageIs('/set-up-a-payment-plan/why-are-you-unable-to-pay-in-full')) {
        document.getElementById('option-Bankrupt').checked = true
        document.getElementById('option-FloodFireTheft').checked = true
        document.getElementById('option-OverRepayment').checked = true
        clickContinue()
    }
}



const canYouMakeUpfrontPayment = () => {
    if (currentPageIs('/set-up-a-payment-plan/can-you-make-an-upfront-payment')) {
        document.getElementById('CanYouMakeAnUpFrontPayment').checked = true
        clickContinue()
    }
}

const howMuchCanYouPayUpfront = () => {
    if (currentPageIs('/set-up-a-payment-plan/how-much-can-you-pay-upfront')) {
        document.getElementById('UpfrontPaymentAmount').value = '23'
        clickContinue()
    }
}

const paymentSummary = () => {
    if (currentPageIs('/set-up-a-payment-plan/upfront-payment-summary')) {
        clickContinue()
    }
}

const howMuchCanYouAffordEachMonth = () => {
    if (currentPageIs('/set-up-a-payment-plan/how-much-can-you-pay-each-month')) {
        document.getElementById('MonthlyPaymentAmount').value = '2000'
        clickContinue()
    }
}

const whichDayOfTheMonth = () => {
    if (currentPageIs('/set-up-a-payment-plan/which-day-do-you-want-to-pay-each-month')) {
        document.getElementById('PaymentDay').checked = true
        clickContinue()
    }
}

const howManyMonthsDoYouWantToPayOver = () => {
    if (currentPageIs('/set-up-a-payment-plan/how-many-months-do-you-want-to-pay-over')) {
        document.getElementById('Instalments').checked = true
        clickContinue()
    }
}

const checkPaymentPlan = () => {
    if (currentPageIs('/set-up-a-payment-plan/check-your-payment-plan')) {
        clickContinue()
    }
}

const aboutYourBankAccount = () => {
    if (currentPageIs('/set-up-a-payment-plan/about-your-bank-account')) {
        document.getElementById('typeOfAccount').checked = true
        document.getElementById('isSoleSignatory').checked = true
        clickContinue()
    }
}

const setUpDirectDebit = () => {
    if (currentPageIs('/set-up-a-payment-plan/set-up-direct-debit')) {
        document.getElementById('name').value = 'Lambent Illumination'
        document.getElementById('sortCode').value = '207102'
        document.getElementById('accountNumber').value = '86563611'
        clickContinue()
    }
}

const checkDirectDebit = () => {
    if (currentPageIs('/set-up-a-payment-plan/check-your-direct-debit-details')) {
        clickContinue()
    }
}

const termsAndConditions = () => {
    if (currentPageIs('/set-up-a-payment-plan/terms-and-conditions')) {
        clickContinue()
    }
}

const whichEmail = () => {
    if (currentPageIs('/set-up-a-payment-plan/which-email-do-you-want-to-use')) {
        document.getElementById('selectAnEmailToUseRadio').checked = true
        clickContinue()
    }
}

const enterEmail = () => {
    if (currentPageIs('/set-up-a-payment-plan/enter-your-email-address')) {
        document.getElementById('newEmailInput').value = 'email@test.com'
        clickContinue()
    }
}

const enterPasscode = () => {
    if (currentPageIs('/email-verification/journey*')) {
        document.getElementById('passcode').value = getPasscode()
        clickContinue()
    }
}

const emailAddressConfirmed = () => {
    if (currentPageIs('/set-up-a-payment-plan/email-address-confirmed')) {
        clickContinue()
    }
}


/* ########################     MAIN FUNCTION     ########################## */
function continueJourney() {
    testOnlyTaxRegimePage()
    testOnlyStartPageEpaye()
    testOnlyStartPageVat()
    testOnlyStartPageSa()
    testOnlyStartPageSia()
    landingPage()
    yourBill()
    whyCannotPayInFull()
    canYouMakeUpfrontPayment()
    howMuchCanYouPayUpfront()
    paymentSummary()
    howMuchCanYouAffordEachMonth()
    whichDayOfTheMonth()
    howManyMonthsDoYouWantToPayOver()
    checkPaymentPlan()
    aboutYourBankAccount()
    setUpDirectDebit()
    checkDirectDebit()
    termsAndConditions()
    whichEmail()
    enterEmail()
    enterPasscode()
    emailAddressConfirmed()
}
