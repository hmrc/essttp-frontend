// form submit button is disabled once the form has been submitted. The form must have class
// 'prevent-multiple-submits' and the submit button to be disabled must have class 'disable-on-click'
if (document.querySelector('.prevent-multiple-submits')) {
    document.querySelector('.prevent-multiple-submits').addEventListener('submit', function(e){
        document.querySelector('.disable-on-click').setAttribute("disabled", true)
        return true
    });
}
