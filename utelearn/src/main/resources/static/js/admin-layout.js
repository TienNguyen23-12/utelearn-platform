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
