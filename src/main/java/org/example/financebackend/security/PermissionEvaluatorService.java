package org.example.financebackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Replaces the old ViewerStrategy / AnalystStrategy / AdminStrategy trio.
 * <p>
 * Instead of hard-coded boolean maps per role, this evaluator checks whether
 * the currently authenticated principal holds a specific permission authority
 * (prefixed with {@code PERM_}) that was loaded from the database at login
 * by {@link CustomUserDetailsService}.
 *
 * <p>Usage inside a service or component:
 * <pre>{@code
 *   if (!permissionEvaluator.hasPermission("records:write")) {
 *       throw AppException.forbidden("Insufficient permissions");
 *   }
 * }</pre>
 *
 * <p>Controller-level checks use {@code @PreAuthorize} directly:
 * <pre>{@code
 *   @PreAuthorize("hasAuthority('PERM_records:write')")
 * }</pre>
 */
@Component
public class PermissionEvaluatorService {

    /**
     * Returns {@code true} if the authenticated user has the given permission.
     *
     * @param permissionName bare permission name, e.g. {@code "records:write"}
     */
    public boolean hasPermission(String permissionName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String authority = "PERM_" + permissionName;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
