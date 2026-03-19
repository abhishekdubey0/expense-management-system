package com.expensemanager.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimitConfig {

    /**
     * Rate limit by client IP address.
     * Each unique IP gets its own token bucket: 20 req/sec, burst up to 50.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // Prefer X-Forwarded-For for clients behind a proxy/load balancer
            String xForwardedFor = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // Take first IP in chain (the original client)
                return Mono.just(xForwardedFor.split(",")[0].trim());
            }
            return Mono.just(
                    Objects.requireNonNull(
                            exchange.getRequest().getRemoteAddress()
                    ).getAddress().getHostAddress()
            );
        };
    }

    /**
     * A tighter rate limiter for auth endpoints (login, register)
     * to prevent brute-force attacks: 5 req/sec, burst 10.
     */
    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /**
     * Standard rate limiter for all other API calls: 20 req/sec, burst 50.
     */
    @Bean
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(20, 50, 1);
    }
}
