package com.utem.utem_core.service;

import com.utem.utem_core.config.JwtProperties;
import com.utem.utem_core.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    // 32 bytes decoded (256-bit key) — safe test secret
    private static final String SECRET = "dGhpcy1pcy1hLXNlY3VyZS1zZWNyZXQta2V5LWZvci11dGVtLWNvcmU=";

    private JwtService jwtService;

    private User superAdmin;
    private User member;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 24));

        superAdmin = User.builder()
                .id("user-1")
                .username("admin")
                .role(User.Role.SUPER_ADMIN)
                .active(true)
                .build();

        member = User.builder()
                .id("user-2")
                .username("alice")
                .role(User.Role.MEMBER)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("generateToken tests")
    class GenerateTokenTests {

        @Test
        @DisplayName("generates a non-blank token")
        void generatesToken() {
            String token = jwtService.generateToken(superAdmin, null);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("token claims contain userId as subject")
        void subjectIsUserId() {
            String token = jwtService.generateToken(superAdmin, null);
            Claims claims = jwtService.validateToken(token);
            assertThat(claims.getSubject()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("token claims contain username")
        void claimsContainUsername() {
            String token = jwtService.generateToken(superAdmin, null);
            Claims claims = jwtService.validateToken(token);
            assertThat(claims.get("username", String.class)).isEqualTo("admin");
        }

        @Test
        @DisplayName("token claims contain role")
        void claimsContainRole() {
            String token = jwtService.generateToken(member, List.of("proj-1"));
            Claims claims = jwtService.validateToken(token);
            assertThat(claims.get("role", String.class)).isEqualTo("MEMBER");
        }

        @Test
        @DisplayName("token claims contain projectIds list")
        void claimsContainProjectIds() {
            String token = jwtService.generateToken(member, List.of("proj-1", "proj-2"));
            Claims claims = jwtService.validateToken(token);
            assertThat(claims.get("projectIds", List.class)).containsExactly("proj-1", "proj-2");
        }

        @Test
        @DisplayName("projectIds is null for super admin")
        void nullProjectIdsForSuperAdmin() {
            String token = jwtService.generateToken(superAdmin, null);
            Claims claims = jwtService.validateToken(token);
            assertThat(claims.get("projectIds")).isNull();
        }
    }

    @Nested
    @DisplayName("validateToken tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("returns claims for a valid token")
        void returnsClaimsForValidToken() {
            String token = jwtService.generateToken(superAdmin, null);
            Claims claims = jwtService.validateToken(token);
            assertThat(claims).isNotNull();
        }

        @Test
        @DisplayName("throws for an expired token")
        void throwsForExpiredToken() {
            // expiryHours=0 produces a token that expires immediately
            JwtService expiredService = new JwtService(new JwtProperties(SECRET, 0));
            String token = expiredService.generateToken(superAdmin, null);
            assertThatThrownBy(() -> expiredService.validateToken(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("throws for a tampered token")
        void throwsForTamperedToken() {
            String token = jwtService.generateToken(superAdmin, null);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertThatThrownBy(() -> jwtService.validateToken(tampered))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);
        }
    }
}
