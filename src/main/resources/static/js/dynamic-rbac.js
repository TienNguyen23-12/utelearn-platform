const UteRbac = {
    catalog: [],
    userPermissions: [],

    async init(options = {}) {
        const catalogUrl = options.catalogUrl || '/data/system-menus.json';
        this.userPermissions = options.userPermissions || window.currentUserPermissions || [];

        if (Array.isArray(options.catalog) && options.catalog.length > 0) {
            this.catalog = options.catalog;
        } else {
            this.catalog = await this.fetchCatalog(catalogUrl);
        }

        this.applyDomPermissions(this.userPermissions);

        if (typeof options.onReady === 'function') {
            options.onReady(this.evaluate(this.catalog, this.userPermissions));
        }

        return this;
    },

    async fetchCatalog(url) {
        try {
            const res = await fetch(url);
            if (!res.ok) {
                return [];
            }
            return await res.json();
        } catch (err) {
            return [];
        }
    },

    setUserPermissions(permissions) {
        this.userPermissions = Array.isArray(permissions) ? permissions : [];
        this.applyDomPermissions(this.userPermissions);
        return this.evaluate(this.catalog, this.userPermissions);
    },

    hasPermission(permissionCode, permissions) {
        const currentPerms = permissions || this.userPermissions;
        if (!permissionCode) {
            return true;
        }
        if (currentPerms.includes('ADMIN') || currentPerms.includes('ROLE_ADMIN')) {
            return true;
        }
        return currentPerms.includes(permissionCode);
    },

    canAccess(feature, permissions) {
        const currentPerms = permissions || this.userPermissions;
        if (!feature) {
            return false;
        }
        if (!feature.requiredPermissions || feature.requiredPermissions.length === 0) {
            return true;
        }
        if (currentPerms.includes('ADMIN') || currentPerms.includes('ROLE_ADMIN')) {
            return true;
        }

        const matchMode = feature.matchMode || 'ANY';
        if (matchMode === 'ALL') {
            return feature.requiredPermissions.every(p => currentPerms.includes(p));
        }
        return feature.requiredPermissions.some(p => currentPerms.includes(p));
    },

    evaluate(catalogList, permissions) {
        const list = catalogList || this.catalog;
        const perms = permissions || this.userPermissions;
        const granted = [];
        const denied = [];

        for (const item of list) {
            if (this.canAccess(item, perms)) {
                granted.push(item);
            } else {
                denied.push(item);
            }
        }

        return {
            granted,
            denied,
            total: list.length,
            grantedCount: granted.length,
            deniedCount: denied.length,
            userPermissions: perms
        };
    },

    renderMenu(containerId, grantedFeatures, activeId = '') {
        const container = typeof containerId === 'string' ? document.getElementById(containerId) : containerId;
        if (!container) {
            return;
        }

        container.innerHTML = '';
        const listWrapper = document.createElement('ul');
        listWrapper.className = 'nav flex-column mb-0 gap-1';

        for (const item of grantedFeatures) {
            const li = document.createElement('li');
            li.className = 'nav-item';

            const a = document.createElement('a');
            const isActive = activeId === item.id;
            a.className = `nav-link sidebar-item d-flex align-items-center justify-content-between ${isActive ? 'active' : ''}`;
            a.href = item.url || '#';

            const leftDiv = document.createElement('div');
            leftDiv.className = 'd-flex align-items-center gap-2';

            const icon = document.createElement('i');
            icon.className = `bi ${item.icon || 'bi-circle'} text-primary`;
            leftDiv.appendChild(icon);

            const span = document.createElement('span');
            span.textContent = item.title;
            leftDiv.appendChild(span);

            a.appendChild(leftDiv);

            if (item.badge) {
                const badge = document.createElement('span');
                badge.className = `badge rounded-pill ${item.badgeClass || 'bg-secondary'}`;
                badge.style.fontSize = '10px';
                badge.textContent = item.badge;
                a.appendChild(badge);
            }

            li.appendChild(a);
            listWrapper.appendChild(li);
        }

        container.appendChild(listWrapper);
    },

    applyDomPermissions(permissions) {
        const currentPerms = permissions || this.userPermissions;
        const elements = document.querySelectorAll('[data-permission]');

        elements.forEach(el => {
            const required = el.getAttribute('data-permission');
            const mode = el.getAttribute('data-permission-mode') || 'hide';
            const allowed = this.hasPermission(required, currentPerms);

            if (allowed) {
                if (mode === 'hide') {
                    el.classList.remove('d-none');
                } else {
                    el.removeAttribute('disabled');
                    el.classList.remove('disabled', 'pe-none');
                }
            } else {
                if (mode === 'hide') {
                    el.classList.add('d-none');
                } else {
                    el.setAttribute('disabled', 'true');
                    el.classList.add('disabled', 'pe-none');
                }
            }
        });
    }
};

window.UteRbac = UteRbac;
document.addEventListener('DOMContentLoaded', async () => {
    if (window.currentUserPermissions && Array.isArray(window.currentUserPermissions)) {
        UteRbac.setUserPermissions(window.currentUserPermissions);
        return;
    }
    if (document.querySelector('[data-permission]')) {
        try {
            const res = await fetch('/api/auth/me/permissions');
            if (res.ok) {
                const data = await res.json();
                if (data && Array.isArray(data.permissions)) {
                    window.currentUserPermissions = data.permissions;
                    UteRbac.setUserPermissions(data.permissions);
                }
            }
        } catch (e) {}
    }
});
