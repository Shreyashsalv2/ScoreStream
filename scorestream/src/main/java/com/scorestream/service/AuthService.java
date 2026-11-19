package com.scorestream.service;

import com.scorestream.domain.AppUser;
import com.scorestream.domain.Role;
import com.scorestream.dto.AuthResponse;
import com.scorestream.dto.LoginRequest;
import com.scorestream.dto.RegisterRequest;
import com.scorestream.repository.AppUserRepository;
import com.scorestream.security.JwtService;
import com.scorestream.web.ConflictException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** New accounts are always USER. Admins are created from configuration (see DataSeeder). */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ConflictException("Username '" + request.username() + "' is taken");
        }
        AppUser user = users.save(new AppUser(request.username(), passwordEncoder.encode(request.password()),
                Role.USER));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Same error whether the username or the password is wrong, so attackers can't probe usernames.
        AppUser user = users.findByUsername(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        return tokenFor(user);
    }

    private AuthResponse tokenFor(AppUser user) {
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, "Bearer", jwtService.getLifetimeMinutes(), user.getUsername(), user.getRole());
    }
}
