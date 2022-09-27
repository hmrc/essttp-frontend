// handle back click
if (document.querySelector('.govuk-back-link')) {
    document.querySelector('.govuk-back-link').addEventListener('click', function(e){
        e.preventDefault();
        e.stopPropagation();
        window.history.back();
    });
}
