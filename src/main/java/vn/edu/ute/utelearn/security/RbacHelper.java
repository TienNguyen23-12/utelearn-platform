package vn.edu.ute.utelearn.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component("rbac")
public class RbacHelper {

    public boolean isLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    public List<String> getCurrentUserRoles() {
        return getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> !a.startsWith("ROLE_")) // Lấy mã role gốc: ADMIN, INSTRUCTOR, ...
            .collect(Collectors.toList());
    }

    public List<String> getCurrentUserPermissions() {
        return getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
    }

    public boolean hasRole(String role) {
        if (role == null || !isLoggedIn()) return false;
        String cleanRole = role.trim();
        return getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase(cleanRole) || 
                           a.getAuthority().equalsIgnoreCase("ROLE_" + cleanRole));
    }

    public boolean hasAnyRole(String... roles) {
        if (roles == null || !isLoggedIn()) return false;
        for (String r : roles) {
            if (hasRole(r)) return true;
        }
        return false;
    }

    public boolean hasPermission(String permission) {
        if (permission == null || !isLoggedIn()) return false;
        String cleanPerm = permission.trim();
        return getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase(cleanPerm));
    }

    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || !isLoggedIn()) return false;
        for (String p : permissions) {
            if (hasPermission(p)) return true;
        }
        return false;
    }

    /**
     * So sánh danh sách role được phép của một menu/tính năng (từ JSON) với role của người dùng hiện tại
     * @param allowedRoles Danh sách các vai trò được phép truy cập
     * @return true nếu người dùng có ít nhất một vai trò hợp lệ, hoặc nếu allowedRoles rỗng
     */
    public boolean isAllowed(Collection<String> allowedRoles) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true; // Công khai nếu không yêu cầu role
        }
        if (!isLoggedIn()) {
            return false;
        }
        return allowedRoles.stream().anyMatch(this::hasRole);
    }

    private Collection<? extends GrantedAuthority> getAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return List.of();
        }
        return auth.getAuthorities();
    }
}
