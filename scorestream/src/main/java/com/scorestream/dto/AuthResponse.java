package com.scorestream.dto;

import com.scorestream.domain.Role;

public record AuthResponse(String token, String tokenType, long expiresInMinutes, String username, Role role) {
}
