package com.expensemanager.approval.util;

import com.expensemanager.approval.service.ApprovalService.UserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static UserContext buildContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in context");
        }
        if (auth.getPrincipal() instanceof JwtUserPrincipal p) {
            return new UserContext(p.uuid(), p.email(), p.fullName(), p.role(), p.department());
        }
        throw new IllegalStateException("Unexpected principal type: "
                + auth.getPrincipal().getClass().getName());
    }

    public record JwtUserPrincipal(String uuid, String email, String fullName,
                                    String role, String department) {}
}
