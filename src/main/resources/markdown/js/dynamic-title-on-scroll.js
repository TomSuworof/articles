(function() {
    var defaultTitle = document.title;
    var headings = Array.prototype.slice.call(document.querySelectorAll('h1'));
    if (!headings.length) return;

    var ticking = false;

    function updateTitle() {
        var scrollTop = window.scrollY || document.documentElement.scrollTop || 0;

        // At the very top of the page the blog title is shown. As soon as the
        // page is scrolled at all, the active article title takes over.
        if (scrollTop <= 0) {
            document.title = defaultTitle;
            ticking = false;
            return;
        }

        // Pick the last heading that has reached the upper part of the viewport.
        // Starting from the first heading is what keeps the top bar from
        // skipping article #1: its h1 is already above the old observer band.
        var line = window.innerHeight * 0.3;
        var active = null;

        for (var i = 0; i < headings.length; i++) {
            if (headings[i].getBoundingClientRect().top <= line) {
                active = headings[i];
            } else {
                break;
            }
        }

        document.title = (active && active.textContent.trim()) || defaultTitle;
        ticking = false;
    }

    function scheduleUpdate() {
        if (ticking) return;
        ticking = true;
        if (window.requestAnimationFrame) {
            window.requestAnimationFrame(updateTitle);
        } else {
            updateTitle();
        }
    }

    window.addEventListener('scroll', scheduleUpdate, { passive: true });
    window.addEventListener('resize', scheduleUpdate);
    updateTitle();
})();
