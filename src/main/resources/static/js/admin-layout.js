function toggleSidebar() {
    const sidebar = document.getElementById('appSidebar');
    const overlay = document.getElementById('sidebarOverlay');

    document.body.classList.toggle('sidebar-collapsed');

    if (sidebar) {
        sidebar.classList.toggle('show');
    }
    if (overlay) {
        overlay.classList.toggle('show');
    }
}

function showConfirmModal(event, element, message) {
    if (event) {
        event.preventDefault();
    }

    let confirmModalEl = document.getElementById('globalConfirmModal');
    if (!confirmModalEl) {
        const modalHtml = `
        <div class="modal fade" id="globalConfirmModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-sm">
                <div class="modal-content border-0 shadow rounded-4">
                    <div class="modal-body p-4 text-center">
                        <div class="text-warning mb-3" id="globalConfirmIconWrapper">
                            <i class="bi bi-exclamation-circle" style="font-size: 3rem;"></i>
                        </div>
                        <h5 class="fw-bold mb-2" id="globalConfirmTitle">Xác nhận</h5>
                        <p class="text-muted small mb-4" id="globalConfirmMessage"></p>
                        <div class="d-flex gap-2 justify-content-center">
                            <button type="button" class="btn btn-light border rounded-pill px-3" data-bs-dismiss="modal" id="globalCancelBtn">Đóng</button>
                            <button type="button" class="btn btn-danger rounded-pill px-4" id="globalConfirmBtn">Đồng ý</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>`;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        confirmModalEl = document.getElementById('globalConfirmModal');
    }

    document.getElementById('globalConfirmMessage').innerText = message;

    const lowerMsg = message ? message.toLowerCase() : '';
    const isDelete = lowerMsg.includes('xóa') || lowerMsg.includes('delete');
    const isCancel = lowerMsg.includes('hủy') || lowerMsg.includes('cancel');
    const confirmBtn = document.getElementById('globalConfirmBtn');
    const cancelBtn = document.getElementById('globalCancelBtn');
    const iconWrapper = document.getElementById('globalConfirmIconWrapper');
    const titleEl = document.getElementById('globalConfirmTitle');
    if (confirmBtn) {
        confirmBtn.className = (isDelete || isCancel) ? 'btn btn-danger rounded-pill px-4 fw-semibold' : 'btn btn-primary rounded-pill px-4 fw-semibold';
        confirmBtn.innerText = isDelete ? 'Xóa' : (isCancel ? 'Hủy đơn' : 'Đồng ý');
    }
    if (cancelBtn) {
        cancelBtn.innerText = isCancel ? 'Giữ lại' : 'Đóng';
    }
    if (iconWrapper) {
        iconWrapper.className = (isDelete || isCancel) ? 'text-danger mb-3' : 'text-warning mb-3';
    }
    if (titleEl) {
        titleEl.innerText = isCancel ? 'Xác nhận hủy đơn' : 'Xác nhận';
    }

    const newConfirmBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

    newConfirmBtn.addEventListener('click', function() {
        const modalInstance = bootstrap.Modal.getInstance(confirmModalEl);
        if (modalInstance) {
            modalInstance.hide();
        }

        if (typeof element === 'function') {
            element();
        } else if (element && element.tagName === 'FORM') {
            element.submit();
        } else if (element && element.tagName === 'A') {
            window.location.href = element.href;
        }
    });

    const modal = bootstrap.Modal.getOrCreateInstance(confirmModalEl);
    modal.show();

    return false;
}

function getToastIconHtml(type) {
    if (type === 'success') {
        return `<div style="background-color: #00b050; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg></div>`;
    }
    if (type === 'warning') {
        return `<div style="background-color: #f5a623; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg></div>`;
    }
    if (type === 'info') {
        return `<div style="background-color: #0ea5e9; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg></div>`;
    }
    return `<div style="background-color: #dc3545; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></div>`;
}

function wrapToastifyGlobal() {
    if (typeof window.Toastify !== 'function' || window.Toastify.__uteWhiteThemeWrapped) {
        return;
    }
    const rawToastify = window.Toastify;
    const wrapped = function(options) {
        const opts = Object.assign({}, options || {});
        const bg = (opts.style && opts.style.background) ? String(opts.style.background).toLowerCase() : '';
        if (bg === 'white' || bg === '#ffffff' || bg === '#fff') {
            return rawToastify(opts);
        }
        let type = opts.type || 'success';
        if (bg.includes('#dc3545') || bg.includes('#ef4444') || bg.includes('#dc2626') || bg.includes('239, 68, 68')) {
            type = 'error';
        } else if (bg.includes('#f59e0b') || bg.includes('#eab308') || bg.includes('#f5a623') || bg.includes('#d97706')) {
            type = 'warning';
        } else if (bg.includes('#0d6efd') || bg.includes('#2563eb') || bg.includes('#3b82f6') || bg.includes('#0ea5e9') || bg.includes('#64748b') || bg.includes('#4f46e5') || bg.includes('#6366f1')) {
            type = 'info';
        }
        const iconHtml = getToastIconHtml(type);
        let toastInstance = null;
        const normalizedOpts = Object.assign({}, opts, {
            text: `<div style="display: flex; align-items: center; gap: 10px;">
                    ${iconHtml}
                    <span style="color: #111; font-weight: 500; font-size: 14.5px; font-family: 'Plus Jakarta Sans', sans-serif;">${opts.text || ''}</span>
                   </div>`,
            escapeMarkup: false,
            duration: opts.duration || 3000,
            close: false,
            gravity: opts.gravity || "top",
            position: opts.position || "right",
            style: Object.assign({}, opts.style || {}, {
                background: "white",
                color: "#111",
                borderRadius: "30px",
                boxShadow: "0 6px 16px rgba(0,0,0,0.12)",
                padding: "10px 20px 10px 12px",
                border: "1px solid #f0f0f0",
                cursor: "pointer"
            }),
            onClick: opts.onClick || function() {
                if (!opts.destination && toastInstance && typeof toastInstance.hideToast === 'function') {
                    toastInstance.hideToast();
                }
            }
        });
        toastInstance = rawToastify(normalizedOpts);
        return toastInstance;
    };
    Object.assign(wrapped, rawToastify);
    wrapped.prototype = rawToastify.prototype;
    wrapped.__uteWhiteThemeWrapped = true;
    window.Toastify = wrapped;
}

wrapToastifyGlobal();
document.addEventListener('DOMContentLoaded', wrapToastifyGlobal);

function showToast(message, type) {
    if (typeof Toastify === 'undefined') {
        return;
    }
    wrapToastifyGlobal();
    const iconHtml = getToastIconHtml(type || 'success');

    let toast = Toastify({
        text: `<div style="display: flex; align-items: center; gap: 10px;">
                ${iconHtml}
                <span style="color: #111; font-weight: 500; font-size: 14.5px; font-family: 'Plus Jakarta Sans', sans-serif;">${message}</span>
               </div>`,
        escapeMarkup: false,
        duration: 3000,
        close: false,
        gravity: "top",
        position: "right",
        style: {
            background: "white",
            borderRadius: "30px",
            boxShadow: "0 6px 16px rgba(0,0,0,0.12)",
            padding: "10px 20px 10px 12px",
            border: "1px solid #f0f0f0",
            cursor: "pointer"
        },
        onClick: function() {
            toast.hideToast();
        }
    });
    toast.showToast();
}

function showModerationFeedbackModal(itemType, itemId) {
    let feedbackModalEl = document.getElementById('globalFeedbackModal');
    if (!feedbackModalEl) {
        const modalHtml = `
        <div class="modal fade" id="globalFeedbackModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow rounded-4">
                    <div class="modal-header border-bottom-0 pb-0">
                        <h5 class="modal-title fw-bold text-danger d-flex align-items-center gap-2">
                            <i class="bi bi-exclamation-octagon-fill"></i> Lý do từ chối kiểm duyệt
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-4">
                        <div class="mb-3 d-flex justify-content-between align-items-center text-muted small">
                            <span><i class="bi bi-person-badge me-1"></i>Kiểm duyệt viên: <strong class="text-dark" id="feedbackModeratorName">---</strong></span>
                            <span id="feedbackCreatedAt">---</span>
                        </div>
                        <div class="alert alert-danger bg-danger-subtle border-danger-subtle text-danger-emphasis rounded-3 mb-3">
                            <div class="fw-semibold mb-1"><i class="bi bi-chat-left-dots-fill me-1"></i> Nhận xét từ kiểm duyệt viên:</div>
                            <div id="feedbackContent" style="white-space: pre-wrap;" class="mt-2 text-dark"></div>
                        </div>
                        <div class="text-muted small">
                            <i class="bi bi-info-circle me-1"></i> Bạn có thể cập nhật lại nội dung giáo trình/thông tin và nhấn nút <strong>"Gửi duyệt"</strong> để kiểm duyệt lại.
                        </div>
                    </div>
                    <div class="modal-footer border-top-0 pt-0">
                        <button type="button" class="btn btn-secondary rounded-pill px-4" data-bs-dismiss="modal">Đóng</button>
                    </div>
                </div>
            </div>
        </div>`;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        feedbackModalEl = document.getElementById('globalFeedbackModal');
    }

    fetch('/api/moderator/tasks/feedback?itemType=' + encodeURIComponent(itemType) + '&itemId=' + encodeURIComponent(itemId))
        .then(response => {
            if (!response.ok) {
                throw new Error('Không thể tải thông tin phản hồi');
            }
            return response.json();
        })
        .then(data => {
            document.getElementById('feedbackModeratorName').innerText = data.moderatorName || 'Kiểm duyệt viên';
            document.getElementById('feedbackCreatedAt').innerText = data.createdAt ? new Date(data.createdAt).toLocaleString('vi-VN') : '';
            document.getElementById('feedbackContent').innerText = data.feedback || 'Không có ghi chú chi tiết nào được ghi lại.';
            const modal = bootstrap.Modal.getOrCreateInstance(feedbackModalEl);
            modal.show();
        })
        .catch(err => {
            if (typeof showToast === 'function') {
                showToast(err.message || 'Lỗi khi tải phản hồi', 'error');
            } else {
                alert(err.message || 'Lỗi khi tải phản hồi');
            }
        });
}
