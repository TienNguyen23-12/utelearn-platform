// UTELearn Admin Portal Layout Scripts
function toggleSidebar() {
    const sidebar = document.getElementById('appSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    // Toggle class on body for desktop behavior
    document.body.classList.toggle('sidebar-collapsed');
    
    // Toggle class for mobile behavior
    if (sidebar) {
        sidebar.classList.toggle('show');
    }
    if (overlay) {
        overlay.classList.toggle('show');
    }
}

// Global Confirm Modal
function showConfirmModal(event, element, message) {
    event.preventDefault();
    
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
                            <button type="button" class="btn btn-primary rounded-pill px-4" id="globalConfirmBtn">Đồng ý</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>`;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        confirmModalEl = document.getElementById('globalConfirmModal');
    }

    document.getElementById('globalConfirmMessage').innerText = message;
    
    const confirmBtn = document.getElementById('globalConfirmBtn');
    const newConfirmBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);
    
    newConfirmBtn.addEventListener('click', function() {
        const modalInstance = bootstrap.Modal.getInstance(confirmModalEl);
        modalInstance.hide();
        
        if (typeof element === 'function') {
            element();
        } else if (element && element.tagName === 'FORM') {
            element.submit();
        } else if (element && element.tagName === 'A') {
            window.location.href = element.href;
        }
    });

    const modal = new bootstrap.Modal(confirmModalEl);
    modal.show();
    
    return false;
}
