package com.expensemanager.report.util;

public class SecurityUtils {

    public static UserContext buildContext() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new IllegalStateException("Not authenticated");
        if (auth.getPrincipal() instanceof UserContext ctx) return ctx;
        throw new IllegalStateException("Unexpected principal type");
    }

    public record UserContext(String uuid, String email, String fullName, String role) {
        public boolean hasRole(String r) { return role != null && role.contains(r); }
    }
}
