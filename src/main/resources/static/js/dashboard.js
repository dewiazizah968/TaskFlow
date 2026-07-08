/* ==========================================================================
   TaskFlow Dashboard - Behaviour
   window.__dashboardData is injected inline by dashboard.html before this
   script loads (see th:inline="javascript" block).
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
    animateCountUp();
    animateProgressBars();
    renderProgressChart();
    renderProjectsChart();
});

/* ------------------------------ Count-up numbers ------------------------------ */

function animateCountUp() {
    document.querySelectorAll('.stat-value[data-count-to]').forEach((el) => {
        const target = parseInt(el.getAttribute('data-count-to'), 10) || 0;
        const duration = 800;
        const start = performance.now();

        function step(now) {
            const progress = Math.min((now - start) / duration, 1);
            const value = Math.round(target * progress);
            el.textContent = value;
            if (progress < 1) requestAnimationFrame(step);
        }

        requestAnimationFrame(step);
    });
}

/* ------------------------------ Progress bars ------------------------------ */

function animateProgressBars() {
    document.querySelectorAll('.progress-fill[data-progress]').forEach((el) => {
        const value = parseFloat(el.getAttribute('data-progress')) || 0;
        requestAnimationFrame(() => {
            el.style.width = Math.min(value, 100) + '%';
        });
    });
}

/* ------------------------------ Charts ------------------------------ */

function renderProgressChart() {
    const canvas = document.getElementById('progressChart');
    if (!canvas) return;
    if (typeof Chart === 'undefined') {
        showChartFallback(canvas);
        return;
    }

    const data = window.__dashboardData || {};
    const completed = data.completedTasks || 0;
    const remaining = Math.max((data.totalTasks || 0) - completed, 0);

    const hasData = (data.totalTasks || 0) > 0;

    new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: ['Completed', 'Remaining'],
            datasets: [{
                data: hasData ? [completed, remaining] : [1],
                backgroundColor: hasData ? ['#6366f1', '#e6e4f7'] : ['#e6e4f7'],
                borderWidth: 0,
                hoverOffset: 6,
            }],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '68%',
            plugins: {
                legend: { display: false },
                tooltip: { enabled: hasData },
            },
        },
    });
}

function renderProjectsChart() {
    const canvas = document.getElementById('projectsChart');
    if (!canvas) return;
    if (typeof Chart === 'undefined') {
        showChartFallback(canvas);
        return;
    }

    const data = window.__dashboardData || {};

    new Chart(canvas, {
        type: 'bar',
        data: {
            labels: ['Completed', 'Running', 'Pending'],
            datasets: [{
                data: [
                    data.completedProjects || 0,
                    data.runningProjects || 0,
                    data.pendingProjects || 0,
                ],
                backgroundColor: ['#22c55e', '#f59e0b', '#a855f7'],
                borderRadius: 8,
                maxBarThickness: 46,
            }],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { precision: 0 },
                    grid: { color: '#f0eefc' },
                },
                x: {
                    grid: { display: false },
                },
            },
        },
    });
}

function showChartFallback(canvas) {
    const wrap = canvas.closest('.chart-wrap');
    if (!wrap) return;
    console.warn('Chart.js failed to load. Check the <script> CDN URL in dashboard.html, or your internet connection.');
    wrap.innerHTML = '<p style="font-size:12px;color:var(--text-muted);">Unable to load chart library.</p>';
}

/* ─── Notification popup ─── */

let notifLoaded = false;

function toggleNotifPanel() {
    const panel = document.getElementById('notifPanel');
    const isOpen = panel.classList.contains('open');
    if (isOpen) {
        closeNotifPanel();
    } else {
        panel.classList.add('open');
        if (!notifLoaded) {
            loadNotifications();
            notifLoaded = true;
        }
    }
}

function closeNotifPanel() {
    const panel = document.getElementById('notifPanel');
    if (panel) panel.classList.remove('open');
}

/* Close when clicking outside */
document.addEventListener('click', (e) => {
    const wrapper = document.querySelector('.notif-wrapper');
    if (wrapper && !wrapper.contains(e.target)) {
        closeNotifPanel();
    }
});

function loadNotifications() {
    const list = document.getElementById('notifList');
    list.innerHTML = '<div class="notif-loading">Loading…</div>';

    fetch('/api/notifications/me')
        .then(r => r.json())
        .then(data => renderNotifications(data))
        .catch(() => {
            list.innerHTML = '<div class="notif-empty">Could not load notifications.</div>';
        });
}

function renderNotifications(data) {
    const list = document.getElementById('notifList');
    const count = document.getElementById('notifCount');

    if (count) count.textContent = data.length > 0 ? data.length + ' unread' : '';

    if (!data || data.length === 0) {
        list.innerHTML = '<div class="notif-empty">🎉 No new notifications!</div>';
        return;
    }

    list.innerHTML = data.map(n => {
        const isInvite = n.type === 'PROJECT_INVITE';
        const isChat = n.type === 'PROJECT_CHAT';
        const subtitle = isInvite
            ? `📁 ${escHtml(n.projectName || '-')}`
            : isChat
                ? `💬 ${escHtml(n.projectName || '-')}`
                : `📋 ${escHtml(n.taskTitle || '-')}`;
        const btnTitle = isChat ? 'Mark as read & open chat' : (isInvite ? 'Mark as read' : 'Mark as read & go to task');

        return `
        <div class="notif-item" id="notif-${n.id}" style="max-height:200px;">
            <div class="notif-dot"></div>
            <div class="notif-body">
                <div class="notif-message">${escHtml(n.message)}</div>
                <div class="notif-task">${subtitle}</div>
            </div>
            <button class="notif-read-btn"
                    title="${btnTitle}"
                    onclick="markAsRead(${n.id}, ${n.projectId || 'null'})">✓</button>
        </div>`;
    }).join('');
}

function markAsRead(notifId, projectId) {
    fetch(`/api/notifications/${notifId}/read`, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            /* Fade out & remove the notification item */
            const item = document.getElementById(`notif-${notifId}`);
            if (item) {
                item.classList.add('removing');
                setTimeout(() => item.remove(), 320);
            }

            /* Update badge count */
            const badge = document.getElementById('notifBadge');
            const unread = data.unreadCount ?? 0;
            if (badge) {
                if (unread <= 0) {
                    badge.remove();
                } else {
                    badge.textContent = unread > 9 ? '9+' : unread;
                }
            }

            /* Update "X unread" counter in panel header */
            const count = document.getElementById('notifCount');
            if (count) count.textContent = unread > 0 ? unread + ' unread' : '';

            /* Show empty state if no items left */
            const list = document.getElementById('notifList');
            if (list && list.querySelectorAll('.notif-item:not(.removing)').length === 0) {
                setTimeout(() => {
                    list.innerHTML = '<div class="notif-empty">🎉 No new notifications!</div>';
                }, 350);
            }

            /* Navigate to project page */
            if (projectId) {
                setTimeout(() => {
                    window.location.href = `/projects/${projectId}`;
                }, 350);
            }
        })
        .catch(() => console.error('Failed to mark notification as read'));
}

function escHtml(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
