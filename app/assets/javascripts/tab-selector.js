// used on test only start page to make the tabs play nice on form error submission

let tabIdToShow, tabIdToHide, panelIdToShow, panelIdToHide;
let regimeToShow;
let classForSelected = 'govuk-tabs__list-item--selected';
let classForHidden = 'govuk-tabs__panel--hidden';

if (document.querySelector(".govuk-error-summary")) {

    if (document.querySelector("#VatSubmitted")) regimeToShow = "Vat";
    else regimeToShow = "Epaye";

    switch (regimeToShow) {
        case "Epaye":
            tabIdToShow = "#tab_paye";
            tabIdToHide = "#tab_vat";
            panelIdToShow = "#paye";
            panelIdToHide = "#vat";
            break;
        case "Vat":
            tabIdToShow = "#tab_vat";
            tabIdToHide = "#tab_paye";
            panelIdToShow = "#vat";
            panelIdToHide = "#paye";
            break;
    }

    document.querySelector(tabIdToHide).parentElement.classList.remove(classForSelected);
    document.querySelector(panelIdToHide).classList.add(classForHidden);
    document.querySelector(panelIdToShow).classList.remove(classForHidden);
    document.querySelector(tabIdToShow).parentElement.classList.add(classForSelected);

}