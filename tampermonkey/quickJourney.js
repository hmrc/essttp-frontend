// ==UserScript==
// @name         ESSTTP quick journey
// @namespace    http://tampermonkey.net/
// @version      15.19
// @description
// @author       achung
// @match        http*://*/set-up-a-payment-plan*
// @include      http*://*/email-verification*
// @updateURL    https://raw.githubusercontent.com/hmrc/essttp-frontend/main/tampermonkey/quickJourney.js
// ==/UserScript==

/*eslint no-undef: "error"*/

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

const testOnlyStartPage = () => {
    if (currentPageIs('/set-up-a-payment-plan/test-only/start-journey')) {
        document.getElementsByName('taxRegime').value = 'Epaye'
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

const landingPage = () => {
    if (currentPageIs('/set-up-a-payment-plan/epaye-payment-plan')) {
        clickContinue()
    }
}

const yourBill = () => {
    if (currentPageIs('/set-up-a-payment-plan/your-bill')) {
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
    testOnlyStartPage()
    landingPage()
    yourBill()
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
    enterPasscode()
    emailAddressConfirmed()
}
