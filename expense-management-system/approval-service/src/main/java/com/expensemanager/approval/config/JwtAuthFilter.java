package com.expensemanager.approval.config;

import com.expensemanager.approval.util.SecurityUtils.JwtUserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                        .build().parseClaimsJws(token).getBody();

                String email  = claims.getSubject();
                String uuid   = claims.get("uuid",  String.class);
                String role   = claims.get("role",  String.class);
                String name   = claims.get("name",  String.class);

                JwtUserPrincipal principal = new JwtUserPrincipal(uuid, email, name, role, null);
                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                log.warn("Invalid JWT in approval-service: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return (StringUtils.hasText(h) && h.startsWith("Bearer ")) ? h.substring(7) : null;
    }
}
