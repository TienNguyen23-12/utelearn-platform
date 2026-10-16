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
                        <div class="text-warning mb-3">
                            <i class="bi bi-exclamation-circle" style="font-size: 3rem;"></i>
                        </div>
                        <h5 class="fw-bold mb-3">Xác nhận</h5>
                        <p class="text-muted mb-4" id="globalConfirmMessage"></p>
                        <div class="d-flex gap-2 justify-content-center">
                            <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Hủy</button>
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

    const isDelete = message && (message.toLowerCase().includes('xóa') || message.toLowerCase().includes('delete'));
    const confirmBtn = document.getElementById('globalConfirmBtn');
    if (confirmBtn) {
        confirmBtn.className = isDelete ? 'btn btn-danger rounded-pill px-4' : 'btn btn-primary rounded-pill px-4';
        confirmBtn.innerText = isDelete ? 'Xóa' : 'Đồng ý';
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

function showToast(message, type) {
    if (typeof Toastify === 'undefined') {
        return;
    }

    let iconHtml = '';
    if (type === 'success') {
        iconHtml = `<div style="background-color: #00b050; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg></div>`;
    } else if (type === 'warning') {
        iconHtml = `<div style="background-color: #f5a623; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg></div>`;
    } else {
        iconHtml = `<div style="background-color: #dc3545; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></div>`;
    }

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
