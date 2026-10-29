(function () {
    if (window.__uteNotificationCenterInit) {
        return;
    }
    window.__uteNotificationCenterInit = true;

    let stompClient = null;
    let currentTab = 'ALL';
    let cachedNotifications = [];

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

    function resolveToastStyle(type) {
        if (type === 'ASSIGNMENT_GRADED') {
            return {
                iconBg: '#f5a623',
                svg: '<polyline points="20 6 9 17 4 12"></polyline>'
            };
        }
        if (type === 'ASSIGNMENT_DEADLINE_REMINDER') {
            return {
                iconBg: '#dc3545',
                svg: '<line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line>'
            };
        }
        if (type === 'LESSON_UNLOCKED') {
            return {
                iconBg: '#0ea5e9',
                svg: '<polyline points="20 6 9 17 4 12"></polyline>'
            };
        }
        if (type === 'PAYMENT_SUCCESS' || type === 'MODERATION_APPROVED') {
            return {
                iconBg: '#00b050',
                svg: '<polyline points="20 6 9 17 4 12"></polyline>'
            };
        }
        if (type === 'MODERATION_SUBMITTED') {
            return {
                iconBg: '#0ea5e9',
                svg: '<line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line>'
            };
        }
        if (type === 'MODERATION_REJECTED') {
            return {
                iconBg: '#dc3545',
                svg: '<line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>'
            };
        }
        return {
            iconBg: '#00b050',
            svg: '<polyline points="20 6 9 17 4 12"></polyline>'
        };
    }

    function showToast(n) {
        if (typeof Toastify !== 'function') return;
        const styleInfo = resolveToastStyle(n.type);
        const actionUrl = n.actionUrl || '/notifications';
        const iconHtml = `<div style="background-color: ${styleInfo.iconBg}; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">${styleInfo.svg}</svg></div>`;

        Toastify({
            text: `<div style="display: flex; align-items: center; gap: 10px;">
                    ${iconHtml}
                    <span style="color: #111; font-weight: 500; font-size: 14.5px; font-family: 'Plus Jakarta Sans', sans-serif;">${escapeHtml(n.title)}: ${escapeHtml(n.content)}</span>
                   </div>`,
            escapeMarkup: false,
            duration: 5000,
            close: false,
            gravity: 'top',
            position: 'right',
            destination: actionUrl,
            newWindow: false,
            style: {
                background: 'white',
                borderRadius: '30px',
                boxShadow: '0 6px 16px rgba(0,0,0,0.12)',
                padding: '10px 20px 10px 12px',
                border: '1px solid #f0f0f0',
                cursor: 'pointer',
                maxWidth: '420px'
            }
        }).showToast();
    }

    function renderNotificationItem(n) {
        const initial = escapeHtml((n.senderName || 'U').charAt(0).toUpperCase());
        const rawAvatar = n.senderAvatar || '';
        const isValidUrl = rawAvatar.startsWith('https://') || rawAvatar.startsWith('http://') || rawAvatar.startsWith('/');
        const avatarHtml = isValidUrl
            ? `<img src="${escapeHtml(rawAvatar)}" alt="Avatar" class="w-100 h-100 object-fit-cover">`
            : `<span class="fw-bold small text-primary">${initial}</span>`;
        const targetUrl = escapeHtml(n.actionUrl || '/notifications');
        const iconClass = escapeHtml(n.iconClass || 'bi-bell-fill text-primary');
        const isRead = Boolean(n.isRead);
        const bgStyle = isRead ? 'background: #ffffff;' : 'background: #f0f7ff;';
        const unreadDot = isRead ? '' : `<span class="rounded-circle bg-primary flex-shrink-0" style="width: 8px; height: 8px;" title="Chưa đọc"></span>`;

        return `
            <a href="${targetUrl}" data-notification-id="${escapeHtml(n.id || '')}" class="nav-notif-link d-flex align-items-start gap-2.5 p-3 rounded-3 text-decoration-none border-bottom transition-all" style="${bgStyle}" onmouseover="this.style.background='#f8fafc'" onmouseout="this.style.background='${isRead ? '#ffffff' : '#f0f7ff'}'">
                <div class="position-relative flex-shrink-0">
                    <div class="rounded-circle overflow-hidden bg-light border d-flex align-items-center justify-content-center" style="width: 38px; height: 38px;">
                        ${avatarHtml}
                    </div>
                    <span class="position-absolute bottom-0 end-0 bg-white rounded-circle shadow-sm p-0.5 d-flex align-items-center justify-content-center" style="width: 18px; height: 18px; font-size: 11px;">
                        <i class="bi ${iconClass}"></i>
                    </span>
                </div>
                <div class="flex-grow-1 overflow-hidden min-w-0">
                    <div class="d-flex align-items-center justify-content-between mb-1">
                        <span class="fw-bold text-dark text-truncate small" style="max-width: 200px;">${escapeHtml(n.title)}</span>
                        <div class="d-flex align-items-center gap-1.5 ms-1">
                            ${unreadDot}
                            <small class="text-muted flex-shrink-0" style="font-size: 10.5px;">${escapeHtml(n.timeAgoFormatted || '')}</small>
                        </div>
                    </div>
                    <div class="text-secondary small text-truncate-2" style="font-size: 12px; line-height: 1.35; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">
                        ${escapeHtml(n.content)}
                    </div>
                </div>
            </a>
        `;
    }

    function updateBadge(count) {
        const badges = document.querySelectorAll('.nav-notification-badge');
        badges.forEach(b => {
            if (count > 0) {
                b.textContent = count > 99 ? '99+' : count;
                b.classList.remove('d-none');
            } else {
                b.textContent = '0';
                b.classList.add('d-none');
            }
        });
    }

    async function fetchUnreadCount() {
        try {
            const resp = await fetch('/api/notifications/unread-count');
            if (!resp.ok) return;
            const data = await resp.json();
            updateBadge(data.unreadCount || 0);
        } catch (e) {
        }
    }

    function filterAndRenderNotifications() {
        const lists = document.querySelectorAll('.nav-notification-list');
        lists.forEach(list => {
            let filtered = cachedNotifications;
            if (currentTab === 'ACADEMIC') {
                filtered = cachedNotifications.filter(n => n.category === 'ACADEMIC');
            } else if (currentTab === 'COHORT') {
                filtered = cachedNotifications.filter(n => n.category === 'COHORT');
            }

            if (!filtered || filtered.length === 0) {
                list.innerHTML = `
                    <div class="text-center text-muted py-4 small nav-notification-empty">
                        <i class="bi bi-bell-slash fs-4 d-block mb-1 text-secondary opacity-50"></i>
                        Không có thông báo nào trong mục này
                    </div>
                `;
                return;
            }
            list.innerHTML = filtered.map(renderNotificationItem).join('');
            attachItemClickListeners(list);
        });
    }

    function attachItemClickListeners(container) {
        const links = container.querySelectorAll('.nav-notif-link');
        links.forEach(link => {
            link.addEventListener('click', function (e) {
                const id = this.getAttribute('data-notification-id');
                if (id) {
                    fetch(`/api/notifications/${id}/read`, { method: 'PUT' });
                }
            });
        });
    }

    async function loadRecentNotifications() {
        try {
            const resp = await fetch('/api/notifications/recent');
            if (!resp.ok) return;
            const data = await resp.json();
            cachedNotifications = data || [];
            filterAndRenderNotifications();
        } catch (e) {
        }
    }

    async function markAllAsRead() {
        try {
            const resp = await fetch('/api/notifications/mark-all-read', { method: 'PUT' });
            if (resp.ok) {
                updateBadge(0);
                cachedNotifications.forEach(n => n.isRead = true);
                filterAndRenderNotifications();
            }
        } catch (e) {
        }
    }

    function handleIncomingNotification(n) {
        cachedNotifications.unshift(n);
        fetchUnreadCount();
        filterAndRenderNotifications();
        showToast(n);
    }

    function setupEventListeners() {
        document.querySelectorAll('.mark-all-read-btn').forEach(btn => {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                markAllAsRead();
            });
        });

        document.querySelectorAll('.notif-tab-btn').forEach(tabBtn => {
            tabBtn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                document.querySelectorAll('.notif-tab-btn').forEach(b => b.classList.remove('active', 'btn-primary', 'text-white'));
                this.classList.add('active', 'btn-primary', 'text-white');
                currentTab = this.getAttribute('data-tab') || 'ALL';
                filterAndRenderNotifications();
            });
        });

        document.querySelectorAll('.nav-notification-btn').forEach(btn => {
            btn.addEventListener('click', function () {
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
                stompClient.subscribe('/topic/notifications/' + userId, function (msg) {
                    try {
                        const notif = JSON.parse(msg.body);
                        handleIncomingNotification(notif);
                    } catch (err) {
                    }
                });

                stompClient.subscribe('/topic/users/' + userId + '/notifications', function (msg) {
                    try {
                        const raw = JSON.parse(msg.body);
                        const chatNotif = {
                            id: null,
                            title: 'Thảo luận lớp học: ' + (raw.cohortName || ''),
                            content: (raw.senderName || 'Thành viên') + ': ' + (raw.message || ''),
                            type: 'COHORT_CHAT',
                            category: 'COHORT',
                            actionUrl: raw.actionUrl || ('/cohorts/' + (raw.cohortId || '') + '/chat'),
                            senderName: raw.senderName,
                            senderAvatar: raw.senderAvatar,
                            iconClass: 'bi-chat-dots-fill text-primary',
                            timeAgoFormatted: 'Vừa xong',
                            isRead: false
                        };
                        handleIncomingNotification(chatNotif);
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

        setupEventListeners();
        fetchUnreadCount();
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
