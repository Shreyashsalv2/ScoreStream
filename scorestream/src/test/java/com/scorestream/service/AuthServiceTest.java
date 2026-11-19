package com.scorestream.service;

import com.scorestream.domain.AppUser;
import com.scorestream.domain.Role;
import com.scorestream.dto.AuthResponse;
import com.scorestream.dto.LoginRequest;
import com.scorestream.dto.RegisterRequest;
import com.scorestream.repository.AppUserRepository;
import com.scorestream.security.JwtService;
import com.scorestream.web.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtMzI=";

    @Mock
    private AppUserRepository users;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost: fast tests
    private final JwtService jwtService = new JwtService(SECRET, 60);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, encoder, jwtService);
    }

    @Test
    void register_createsAUserWithAHashedPasswordAndReturnsAToken() {
        when(users.existsByUsername("riya")).thenReturn(false);
        when(users.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest("riya", "password123"));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password123");
        assertThat(encoder.matches("password123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(jwtService.parse(response.token())).hasValueSatisfying(u -> {
            assertThat(u.username()).isEqualTo("riya");
            assertThat(u.role()).isEqualTo(Role.USER);
        });
    }

    @Test
    void register_rejectsATakenUsername() {
        when(users.existsByUsername("riya")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("riya", "password123")))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void login_returnsATokenForTheRightPassword() {
        when(users.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", encoder.encode("secret-pass"), Role.ADMIN)));

        AuthResponse response = authService.login(new LoginRequest("admin", "secret-pass"));

        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_rejectsTheWrongPassword() {
        when(users.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", encoder.encode("secret-pass"), Role.ADMIN)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_rejectsAnUnknownUserWithTheSameError() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
