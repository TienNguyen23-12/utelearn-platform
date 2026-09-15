// UTELearn Admin Portal Layout Scripts
function toggleSidebar() {
    const sidebar = document.getElementById('appSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    if (sidebar) {
        sidebar.classList.toggle('show');
    }
    if (overlay) {
        overlay.classList.toggle('show');
    }
}
