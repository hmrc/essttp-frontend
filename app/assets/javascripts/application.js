(function (document, window, navigator) {
    // polyfill for when forEach is not supported e.g. IE11
    if ('NodeList' in window && !NodeList.prototype.forEach) {
        NodeList.prototype.forEach = function (callback, scope) {
            for (let i = 0; i < this.length; i++) {
                callback.call(scope || window, this[i], i, this)
            }
        }
    }

    // copy buttons
    (function (window, document, navigator) {
        const activeClassName = 'copied-to-clipboard'
        function copy (event) {
            event.preventDefault()
            const el = event.currentTarget
            if (navigator.clipboard) {
                navigator.clipboard.writeText(el.dataset.clip).then(function () {
                    resetCopyButtons()
                    el.classList.add(activeClassName)
                }, function (e) {
                    console.error(e)
                })
            } else if (window.clipboardData) {
                window.clipboardData.setData('text/plain', el.dataset.clip)
                resetCopyButtons()
                el.classList.add(activeClassName)
            }
        }

        function resetCopyButtons () {
            document.querySelectorAll('button.' + activeClassName)
                .forEach(function (el) {
                    el.classList.remove(activeClassName)
                })
        }
        if (!navigator.userAgent.match(/(MSIE|Trident)/)) {
            document.querySelectorAll('button.copy-to-clipboard')
                .forEach(function (el) {
                    el.classList.remove('not-supported')
                    el.addEventListener('click', copy)
                })
        }
    })(window, document, navigator)
    // end copy buttons

    if (window.showUrBanner && window.hideBannerUrl) {
        const urBanner = document.querySelector('.hmrc-user-research-banner')
        if (urBanner) {
            urBanner.classList.add('hmrc-user-research-banner--show')
            const closeLink = urBanner.querySelector('.hmrc-user-research-banner__close');
            closeLink.addEventListener('click', noThanksClick)
            function noThanksClick(event) {
                event.preventDefault()
                fetch('/tax-check-for-licence/hide-ur-banner')
                    .then(function(r) {
                        urBanner.classList.remove('hmrc-user-research-banner--show')
                    })
                    .catch(function(error) {
                        console.error('Error:', error);
                    })
            }
        }
    }


    document.querySelectorAll('a[href="#print-dialogue"]')
        .forEach(function(link) {
            link.addEventListener('click', function(event) {
                event.preventDefault();
                window.print();
            })
        })
})(document, window, navigator);