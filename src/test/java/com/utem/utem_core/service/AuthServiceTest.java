package com.utem.utem_core.service;

import com.utem.utem_core.dto.ChangePasswordRequest;
import com.utem.utem_core.dto.LoginRequest;
import com.utem.utem_core.dto.LoginResponse;
import com.utem.utem_core.entity.User;
import com.utem.utem_core.exception.UnauthorizedException;
import com.utem.utem_core.repository.ProjectMemberRepository;
import com.utem.utem_core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock ProjectMemberRepository projectMemberRepository;
    @Mock JwtService jwtService;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    private User activeUser;
    private static final String RAW_PASSWORD = "secret";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, projectMemberRepository, passwordEncoder, jwtService);

        activeUser = User.builder()
                .id("user-1")
                .username("alice")
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .role(User.Role.MEMBER)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("login tests")
    class LoginTests {

        @Test
        @DisplayName("returns LoginResponse on valid credentials")
        void returnsLoginResponseOnValidCredentials() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));
            when(projectMemberRepository.findProjectIdsByUserId("user-1")).thenReturn(List.of("proj-1"));
            when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

            LoginResponse response = authService.login(new LoginRequest("alice", RAW_PASSWORD));

            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.userId()).isEqualTo("user-1");
            assertThat(response.username()).isEqualTo("alice");
            assertThat(response.role()).isEqualTo(User.Role.MEMBER);
            assertThat(response.projectIds()).containsExactly("proj-1");
        }

        @Test
        @DisplayName("super admin login returns null projectIds")
        void superAdminLoginReturnsNullProjectIds() {
            User admin = User.builder()
                    .id("admin-1")
                    .username("admin")
                    .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                    .role(User.Role.SUPER_ADMIN)
                    .active(true)
                    .build();
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
            when(jwtService.generateToken(any(), any())).thenReturn("admin-token");

            LoginResponse response = authService.login(new LoginRequest("admin", RAW_PASSWORD));

            assertThat(response.projectIds()).isNull();
            verify(projectMemberRepository, never()).findProjectIdsByUserId(anyString());
        }

        @Test
        @DisplayName("throws UnauthorizedException for unknown user")
        void throwsForUnknownUser() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", RAW_PASSWORD)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("throws UnauthorizedException for wrong password")
        void throwsForWrongPassword() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("throws UnauthorizedException for inactive user")
        void throwsForInactiveUser() {
            User inactive = User.builder()
                    .id("user-2")
                    .username("alice")
                    .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                    .role(User.Role.MEMBER)
                    .active(false)
                    .build();
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> authService.login(new LoginRequest("alice", RAW_PASSWORD)))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("changePassword tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("updates passwordHash on correct current password")
        void updatesHashOnSuccess() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(activeUser));

            authService.changePassword("user-1", new ChangePasswordRequest(RAW_PASSWORD, "newpass"));

            verify(userRepository).save(activeUser);
            assertThat(passwordEncoder.matches("newpass", activeUser.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("throws UnauthorizedException for wrong current password")
        void throwsForWrongCurrentPassword() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.changePassword("user-1",
                    new ChangePasswordRequest("wrong", "newpass")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Current password");
        }

        @Test
        @DisplayName("throws UnauthorizedException for unknown userId")
        void throwsForUnknownUser() {
            when(userRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword("bad-id",
                    new ChangePasswordRequest(RAW_PASSWORD, "newpass")))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}
