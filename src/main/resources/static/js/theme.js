/**
 * Day / Night mode toggle.
 */
(function () {
    "use strict";

    function initThemeSwitch() {
        var btn = document.getElementById("themeSwitch");
        if (!btn) return;

        var root = document.documentElement;

        function applyState(theme) {
            root.setAttribute("data-theme", theme);
            btn.setAttribute("data-active", theme);
            btn.setAttribute("aria-checked", theme === "night" ? "true" : "false");
        }

        // Sync the button's visual state with whatever the server rendered
        applyState(root.getAttribute("data-theme") === "night" ? "night" : "day");

        btn.addEventListener("click", function () {
            if (btn.disabled) return;

            var current = root.getAttribute("data-theme") === "night" ? "night" : "day";
            var next = current === "night" ? "day" : "night";

            // Optimistic UI update
            applyState(next);
            btn.disabled = true;

            fetch("/theme/toggle", { method: "POST" })
                .then(function (res) {
                    if (!res.ok) throw new Error("Gagal menyimpan preferensi tema");
                    return res.json();
                })
                .then(function (data) {
                    applyState(data.theme === "night" ? "night" : "day");
                })
                .catch(function () {
                    // Revert on failure so the UI doesn't lie about what's saved
                    applyState(current);
                })
                .finally(function () {
                    btn.disabled = false;
                });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initThemeSwitch);
    } else {
        initThemeSwitch();
    }
})();
