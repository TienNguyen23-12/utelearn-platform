package vn.edu.ute.utelearn.util;

import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;

import java.util.Objects;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean hasRole(User user, String... roleCodes) {
        if (user == null || user.getRoles() == null || roleCodes == null || roleCodes.length == 0) {
            return false;
        }
        for (String target : roleCodes) {
            if (target == null || target.isBlank()) {
                continue;
            }
            boolean matched = user.getRoles().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(r -> {
                        String code = r.getCode();
                        String name = r.getName();
                        return (code != null && (code.equalsIgnoreCase(target) || code.equalsIgnoreCase("ROLE_" + target) || ("ROLE_" + code).equalsIgnoreCase(target)))
                                || (name != null && (name.equalsIgnoreCase(target) || name.equalsIgnoreCase("ROLE_" + target)));
                    });
            if (matched) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdmin(User user) {
        return hasRole(user, "ADMIN");
    }

    public static boolean isAdminOrFinance(User user) {
        return hasRole(user, "ADMIN", "FINANCE");
    }

    public static boolean isInstructor(User user) {
        return hasRole(user, "INSTRUCTOR", "TEACHER");
    }

    public static boolean isModerator(User user) {
        return hasRole(user, "MODERATOR");
    }
}
