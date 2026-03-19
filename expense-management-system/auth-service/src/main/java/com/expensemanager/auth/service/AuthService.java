package com.expensemanager.auth.service;

import com.expensemanager.auth.dto.request.LoginRequest;
import com.expensemanager.auth.dto.request.RefreshTokenRequest;
import com.expensemanager.auth.dto.request.RegisterRequest;
import com.expensemanager.auth.dto.response.AuthResponse;
import com.expensemanager.auth.dto.response.UserResponse;
import com.expensemanager.auth.entity.RefreshToken;
import com.expensemanager.auth.entity.Role;
import com.expensemanager.auth.entity.User;
import com.expensemanager.auth.exception.AuthException;
import com.expensemanager.auth.repository.RefreshTokenRepository;
import com.expensemanager.auth.repository.UserRepository;
import com.expensemanager.auth.security.JwtTokenProvider;
import com.expensemanager.auth.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;

    // ─── Register ─────────────────────────────────────────────

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered: " + request.getEmail());
        }

        if (request.getEmployeeId() != null &&
            userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new AuthException("Employee ID already exists: " + request.getEmployeeId());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .department(request.getDepartment())
                .employeeId(request.getEmployeeId())
                .role(Role.EMPLOYEE)  // Default role; admin can promote
                .isActive(true)
                .isVerified(false)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());
        return userMapper.toResponse(saved);
    }

    // ─── Login ────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(),
                            request.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();

            // Revoke existing refresh tokens (single session policy)
            refreshTokenRepository.revokeAllUserTokens(user);

            String accessToken  = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // Persist refresh token
            saveRefreshToken(user, refreshToken);

            log.info("User logged in: {}", user.getEmail());
            return AuthResponse.of(
                    accessToken,
                    refreshToken,
                    jwtTokenProvider.getAccessTokenExpiration() / 1000,
                    userMapper.toResponse(user)
            );

        } catch (BadCredentialsException e) {
            throw new AuthException("Invalid email or password");
        } catch (DisabledException e) {
            throw new AuthException("Account is disabled. Contact support.");
        }
    }

    // ─── Refresh Token ────────────────────────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(requestToken)) {
            throw new AuthException("Invalid or expired refresh token");
        }

        String tokenType = jwtTokenProvider.extractTokenType(requestToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new AuthException("Not a refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new AuthException("Refresh token not found"));

        if (!storedToken.isValid()) {
            throw new AuthException("Refresh token is expired or revoked");
        }

        User user = storedToken.getUser();

        // Rotate: revoke old, issue new
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken  = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        saveRefreshToken(user, newRefreshToken);

        log.info("Token refreshed for user: {}", user.getEmail());
        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000,
                userMapper.toResponse(user)
        );
    }

    // ─── Logout ───────────────────────────────────────────────

    public void logout(String accessToken, String refreshTokenStr) {
        // Blacklist access token in Redis until expiry
        try {
            long expiry = jwtTokenProvider.extractExpiration(accessToken).getTime()
                          - System.currentTimeMillis();
            if (expiry > 0) {
                tokenBlacklistService.blacklistToken(accessToken, expiry);
            }
        } catch (Exception e) {
            log.warn("Could not blacklist access token: {}", e.getMessage());
        }

        // Revoke refresh token in DB
        if (refreshTokenStr != null) {
            refreshTokenRepository.findByToken(refreshTokenStr)
                    .ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
        }
        log.info("User logged out successfully");
    }

    // ─── Private helpers ──────────────────────────────────────

    private void saveRefreshToken(User user, String tokenValue) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtTokenProvider.getRefreshTokenExpiration() / 1000))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}
