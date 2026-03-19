package com.expensemanager.auth.service;

import com.expensemanager.auth.dto.request.LoginRequest;
import com.expensemanager.auth.dto.request.RegisterRequest;
import com.expensemanager.auth.dto.response.AuthResponse;
import com.expensemanager.auth.dto.response.UserResponse;
import com.expensemanager.auth.entity.Role;
import com.expensemanager.auth.entity.User;
import com.expensemanager.auth.exception.AuthException;
import com.expensemanager.auth.repository.RefreshTokenRepository;
import com.expensemanager.auth.repository.UserRepository;
import com.expensemanager.auth.security.JwtTokenProvider;
import com.expensemanager.auth.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .uuid("test-uuid-123")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.EMPLOYEE)
                .isActive(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Test@1234");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Test@1234");
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        UserResponse expected = new UserResponse();
        when(userMapper.toResponse(mockUser)).thenReturn(expected);

        UserResponse result = authService.register(registerRequest);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("Should login successfully and return tokens")
    void login_Success() {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtTokenProvider.generateAccessToken(mockUser)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(mockUser)).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(userMapper.toResponse(mockUser)).thenReturn(new UserResponse());

        AuthResponse result = authService.login(loginRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("Should throw exception on bad credentials")
    void login_BadCredentials_ThrowsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid email or password");
    }
}
