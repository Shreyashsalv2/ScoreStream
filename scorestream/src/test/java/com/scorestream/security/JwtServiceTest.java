package com.scorestream.security;

import com.scorestream.domain.Role;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtMzI=";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2g=";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void aGeneratedTokenParsesBackToTheSameUserAndRole() {
        String token = jwtService.generateToken("admin", Role.ADMIN);

        assertThat(jwtService.parse(token)).hasValueSatisfying(user -> {
            assertThat(user.username()).isEqualTo("admin");
            assertThat(user.role()).isEqualTo(Role.ADMIN);
        });
    }

    @Test
    void aTokenEditedToClaimAdminIsRejected() {
        // An attacker decodes the payload, changes USER to ADMIN and keeps the old signature.
        String[] parts = jwtService.generateToken("riya", Role.USER).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.replace("\"USER\"", "\"ADMIN\"").getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThat(payload).contains("\"USER\"");
        assertThat(jwtService.parse(forged)).isEmpty();
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() {
        String foreign = new JwtService(OTHER_SECRET, 60).generateToken("admin", Role.ADMIN);

        assertThat(jwtService.parse(foreign)).isEmpty();
    }

    @Test
    void anExpiredTokenIsRejected() {
        String expired = new JwtService(SECRET, -1).generateToken("riya", Role.USER);

        assertThat(jwtService.parse(expired)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
        assertThat(jwtService.parse("")).isEmpty();
    }
}
