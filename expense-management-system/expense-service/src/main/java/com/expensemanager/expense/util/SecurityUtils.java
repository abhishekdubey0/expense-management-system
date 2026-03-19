package com.expensemanager.expense.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class SecurityUtils {

    public static UserContext getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        // The principal here is the JwtUserDetails populated by the JWT filter
        if (auth.getPrincipal() instanceof JwtUserDetails jwtUser) {
            return UserContext.builder()
                    .uuid(jwtUser.getUuid())
                    .email(jwtUser.getUsername())
                    .fullName(jwtUser.getFullName())
                    .department(jwtUser.getDepartment())
                    .authorities(auth.getAuthorities().stream()
                            .map(a -> a.getAuthority()).toList())
                    .build();
        }
        throw new IllegalStateException("Cannot extract user context from authentication");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserContext {
        private String uuid;
        private String email;
        private String fullName;
        private String department;
        private java.util.List<String> authorities;

        public boolean hasRole(String role) {
            return authorities != null && authorities.contains(role);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JwtUserDetails implements UserDetails {
        private String uuid;
        private String email;
        private String fullName;
        private String department;
        private Collection<? extends org.springframework.security.core.GrantedAuthority> authorities;

        @Override public String getUsername()              { return email; }
        @Override public String getPassword()              { return null; }
        @Override public boolean isAccountNonExpired()     { return true; }
        @Override public boolean isAccountNonLocked()      { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled()               { return true; }
    }
}
