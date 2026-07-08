document.addEventListener('DOMContentLoaded', function () {

    /* ---------------------------------------------------------
       Generate a scattered starfield
       --------------------------------------------------------- */
    var starsContainer = document.querySelector('.stars');
    if (starsContainer) {
        var starCount = 70;
        for (var i = 0; i < starCount; i++) {
            var star = document.createElement('span');
            star.className = 'star';
            var size = Math.random() * 2.4 + 1;
            star.style.width = size + 'px';
            star.style.height = size + 'px';
            star.style.top = Math.random() * 100 + '%';
            star.style.left = Math.random() * 100 + '%';
            star.style.animationDelay = (Math.random() * 3.5) + 's';
            star.style.animationDuration = (2.5 + Math.random() * 3) + 's';
            starsContainer.appendChild(star);
        }
    }

    /* ---------------------------------------------------------
       Place waypoint dots along the rocket's trajectory path
       and light them up in sync as the rocket passes by.
       --------------------------------------------------------- */
    var path = document.querySelector('.trajectory .flight-path');
    var svg = document.querySelector('.trajectory');
    var rocket = document.querySelector('.rocket');

    if (path && svg && rocket) {
        var waypointCount = 6;
        var totalLength = path.getTotalLength();
        var waypoints = [];

        for (var w = 0; w < waypointCount; w++) {
            var frac = w / (waypointCount - 1);
            var point = path.getPointAtLength(frac * totalLength);
            var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.setAttribute('class', 'waypoint');
            circle.setAttribute('cx', point.x);
            circle.setAttribute('cy', point.y);
            circle.setAttribute('r', 4);
            svg.appendChild(circle);
            waypoints.push({ el: circle, frac: frac });
        }

        var durationMs = 13000; /* keep in sync with .rocket animation duration */
        var startTime = performance.now();

        function syncWaypoints(now) {
            var elapsed = (now - startTime) % durationMs;
            var progress = elapsed / durationMs;

            waypoints.forEach(function (wp) {
                var dist = Math.abs(progress - wp.frac);
                var wrapped = Math.min(dist, 1 - dist);
                var isActive = wrapped < 0.035;
                wp.el.classList.toggle('active', isActive);
                wp.el.setAttribute('r', isActive ? 6 : 4);
            });

            requestAnimationFrame(syncWaypoints);
        }

        if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            requestAnimationFrame(syncWaypoints);
        }
    }

    /* ---------------------------------------------------------
       Navbar background on scroll
       --------------------------------------------------------- */
    var navbar = document.querySelector('.navbar');
    if (navbar) {
        var onScroll = function () {
            navbar.classList.toggle('scrolled', window.scrollY > 20);
        };
        window.addEventListener('scroll', onScroll);
        onScroll();
    }

    /* ---------------------------------------------------------
       Mobile nav toggle
       --------------------------------------------------------- */
    var toggle = document.querySelector('.nav-toggle');
    var links = document.querySelector('.nav-links');
    if (toggle && links) {
        toggle.addEventListener('click', function () {
            links.classList.toggle('open');
        });
    }
});