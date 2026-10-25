(function () {
    if (window.__uteChatNotificationInit) {
        return;
    }
    window.__uteChatNotificationInit = true;

    let stompClient = null;
    let unreadCount = 0;

    function escapeHtml(text) {
        if (!text) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function ensureScript(src, isReadyCheck, callback) {
        if (isReadyCheck()) {
            callback();
            return;
        }
        const script = document.createElement('script');
        script.src = src;
        script.onload = callback;
        document.head.appendChild(script);
    }

    function ensureCss(href) {
        const existing = document.querySelector(`link[href="${href}"]`);
        if (!existing) {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = href;
            document.head.appendChild(link);
        }
    }

    function ensureDependencies(callback) {
        ensureCss('https://cdn.jsdelivr.net/npm/toastify-js/src/toastify.min.css');
        ensureScript('https://cdn.jsdelivr.net/npm/toastify-js', () => typeof window.Toastify === 'function', () => {
            ensureScript('https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js', () => typeof window.SockJS === 'function', () => {
                ensureScript('https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js', () => typeof window.Stomp === 'function', callback);
            });
        });
    }

    function getRoleLabel(role) {
        if (!role) return 'Học viên';
        const r = role.toUpperCase();
        if (r === 'INSTRUCTOR' || r === 'TEACHER') return 'Giảng viên';
        if (r === 'TA') return 'Trợ giảng';
        if (r === 'ADMIN') return 'Quản trị';
        return 'Học viên';
    }

    function getBadgeClass(role) {
        const r = (role || '').toUpperCase();
        if (r === 'INSTRUCTOR' || r === 'TEACHER') return 'bg-success-subtle text-success border border-success-subtle';
        if (r === 'TA') return 'bg-info-subtle text-info border border-info-subtle';
        if (r === 'ADMIN') return 'bg-danger-subtle text-danger border border-danger-subtle';
        return 'bg-secondary-subtle text-secondary border';
    }

    function resolveActionUrl(n) {
        const isInstructorElement = document.querySelector('[data-is-instructor="true"]');
        const isInstructorPath = window.location.pathname.startsWith('/instructor') || window.location.pathname.startsWith('/admin');
        if ((isInstructorElement || isInstructorPath) && n.cohortId) {
            return '/instructor/cohorts/' + n.cohortId + '/chat';
        }
        return n.actionUrl || ('/modules/' + n.cohortId + '/learn');
    }

    function renderNotificationItem(n) {
        const initial = escapeHtml((n.senderName || 'U').charAt(0).toUpperCase());
        const avatarHtml = n.senderAvatar
            ? `<img src="${n.senderAvatar}" alt="Avatar" class="w-100 h-100 object-fit-cover">`
            : `<span class="fw-bold small text-primary">${initial}</span>`;
        const targetUrl = resolveActionUrl(n);

        return `
            <a href="${targetUrl}" class="d-flex align-items-start gap-2.5 p-3 rounded-3 text-decoration-none border-bottom transition-all" style="background: #ffffff;" onmouseover="this.style.background='#f8fafc'" onmouseout="this.style.background='#ffffff'">
                <div class="rounded-circle overflow-hidden flex-shrink-0 bg-light border d-flex align-items-center justify-content-center" style="width: 38px; height: 38px;">
                    ${avatarHtml}
                </div>
                <div class="flex-grow-1 overflow-hidden min-w-0">
                    <div class="d-flex align-items-center justify-content-between mb-1">
                        <span class="fw-bold text-dark text-truncate small" style="max-width: 210px;" title="${escapeHtml(n.cohortName)}">${escapeHtml(n.cohortName)}</span>
                        <small class="text-muted flex-shrink-0 ms-1" style="font-size: 10.5px;">${escapeHtml(n.createdAtFormatted)}</small>
                    </div>
                    <div class="d-flex align-items-center gap-1.5 mb-1">
                        <span class="badge ${getBadgeClass(n.senderRole)} rounded-pill" style="font-size: 9.5px; padding: 2px 7px;">${getRoleLabel(n.senderRole)}</span>
                        <span class="fw-semibold text-dark text-truncate" style="font-size: 12px;">${escapeHtml(n.senderName)}</span>
                    </div>
                    <div class="text-secondary text-truncate small" style="font-size: 12px;">
                        ${escapeHtml(n.message)}
                    </div>
                </div>
            </a>
        `;
    }

    function showNotificationToast(n) {
        if (typeof Toastify !== 'function') return;

        const roleText = getRoleLabel(n.senderRole);
        const targetUrl = resolveActionUrl(n);
        Toastify({
            text: `💬 [${n.cohortName}] ${roleText} ${n.senderName}: ${n.message}`,
            duration: 6000,
            close: true,
            gravity: 'top',
            position: 'right',
            destination: targetUrl,
            newWindow: false,
            style: {
                background: (n.senderRole === 'INSTRUCTOR' || n.senderRole === 'TEACHER')
                    ? 'linear-gradient(135deg, #059669 0%, #10b981 100%)'
                    : 'linear-gradient(135deg, #2563eb 0%, #3b82f6 100%)',
                borderRadius: '14px',
                boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.25)',
                fontSize: '13px',
                fontWeight: '500',
                padding: '12px 18px',
                cursor: 'pointer'
            }
        }).showToast();
    }

    function handleIncomingNotification(n) {
        unreadCount++;
        const badges = document.querySelectorAll('.nav-notification-badge');
        badges.forEach(b => {
            b.textContent = unreadCount > 99 ? '99+' : unreadCount;
            b.classList.remove('d-none');
        });

        const lists = document.querySelectorAll('.nav-notification-list');
        lists.forEach(list => {
            const emptyEl = list.querySelector('.nav-notification-empty');
            if (emptyEl) {
                emptyEl.remove();
            }
            const itemHtml = renderNotificationItem(n);
            list.insertAdjacentHTML('afterbegin', itemHtml);
        });

        const targetUrl = resolveActionUrl(n);
        if (window.location.pathname !== targetUrl) {
            showNotificationToast(n);
        }
    }

    async function loadRecentNotifications() {
        try {
            const resp = await fetch('/api/cohorts/notifications/recent');
            if (!resp.ok) return;
            const data = await resp.json();
            const lists = document.querySelectorAll('.nav-notification-list');
            lists.forEach(list => {
                if (!data || data.length === 0) {
                    list.innerHTML = `
                        <div class="text-center text-muted py-4 small nav-notification-empty">
                            <i class="bi bi-bell-slash fs-4 d-block mb-1 text-secondary opacity-50"></i>
                            Chưa có thông báo thảo luận mới
                        </div>
                    `;
                    return;
                }
                list.innerHTML = data.map(renderNotificationItem).join('');
            });
        } catch (e) {
        }
    }

    function initNotificationDropdowns() {
        const buttons = document.querySelectorAll('.nav-notification-btn');
        buttons.forEach(btn => {
            btn.addEventListener('click', function () {
                unreadCount = 0;
                const badges = document.querySelectorAll('.nav-notification-badge');
                badges.forEach(b => {
                    b.textContent = '0';
                    b.classList.add('d-none');
                });
                loadRecentNotifications();
            });
        });
    }

    function connectWebSocket(userId) {
        try {
            const socket = new SockJS('/ws');
            stompClient = Stomp.over(socket);
            stompClient.debug = null;
            stompClient.connect({}, function () {
                stompClient.subscribe('/topic/users/' + userId + '/notifications', function (msg) {
                    try {
                        const notif = JSON.parse(msg.body);
                        handleIncomingNotification(notif);
                    } catch (err) {
                    }
                });
            }, function () {
                setTimeout(() => connectWebSocket(userId), 7000);
            });
        } catch (err) {
            setTimeout(() => connectWebSocket(userId), 7000);
        }
    }

    function start() {
        const wrapper = document.querySelector('[data-chat-user-id]');
        if (!wrapper) return;

        const userId = wrapper.getAttribute('data-chat-user-id');
        if (!userId) return;

        initNotificationDropdowns();
        ensureDependencies(() => {
            connectWebSocket(userId);
            loadRecentNotifications();
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
