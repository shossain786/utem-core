package com.utem.utem_core.service;

import com.utem.utem_core.dto.UserDTO;
import com.utem.utem_core.entity.User;
import com.utem.utem_core.repository.ProjectMemberRepository;
import com.utem.utem_core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("UserService")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock ProjectMemberRepository projectMemberRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, projectMemberRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("getAllUsers tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("returns DTO list for all users")
        void returnsDtoList() {
            User u = User.builder().id("u1").username("alice").role(User.Role.MEMBER).active(true).build();
            when(userRepository.findAll()).thenReturn(List.of(u));
            when(projectMemberRepository.findProjectIdsByUserId("u1")).thenReturn(List.of("p1"));

            List<UserDTO> result = userService.getAllUsers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).username()).isEqualTo("alice");
            assertThat(result.get(0).projectIds()).containsExactly("p1");
        }
    }

    @Nested
    @DisplayName("createUser tests")
    class CreateUserTests {

        @Test
        @DisplayName("saves user with encoded password")
        void savesWithEncodedPassword() {
            when(userRepository.existsByUsername("bob")).thenReturn(false);
            User saved = User.builder().id("u2").username("bob").role(User.Role.MEMBER).active(true).build();
            when(userRepository.save(any())).thenReturn(saved);

            UserDTO dto = userService.createUser("bob", "bob@test.com", "pass123", User.Role.MEMBER);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(passwordEncoder.matches("pass123", captor.getValue().getPasswordHash())).isTrue();
            assertThat(dto.username()).isEqualTo("bob");
        }

        @Test
        @DisplayName("throws IllegalStateException when username already taken")
        void throwsForDuplicateUsername() {
            when(userRepository.existsByUsername("alice")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser("alice", null, "pw", User.Role.MEMBER))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already taken");
        }
    }

    @Nested
    @DisplayName("deactivateUser tests")
    class DeactivateUserTests {

        @Test
        @DisplayName("sets active=false and saves")
        void setsActiveFalse() {
            User u = User.builder().id("u1").username("alice").active(true).role(User.Role.MEMBER).build();
            when(userRepository.findById("u1")).thenReturn(Optional.of(u));

            userService.deactivateUser("u1");

            assertThat(u.isActive()).isFalse();
            verify(userRepository).save(u);
        }

        @Test
        @DisplayName("throws for unknown userId")
        void throwsForUnknownUser() {
            when(userRepository.findById("bad")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deactivateUser("bad"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("reactivateUser tests")
    class ReactivateUserTests {

        @Test
        @DisplayName("sets active=true and saves")
        void setsActiveTrue() {
            User u = User.builder().id("u1").username("alice").active(false).role(User.Role.MEMBER).build();
            when(userRepository.findById("u1")).thenReturn(Optional.of(u));

            userService.reactivateUser("u1");

            assertThat(u.isActive()).isTrue();
            verify(userRepository).save(u);
        }
    }

    @Nested
    @DisplayName("resetPassword tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("updates passwordHash with new encoded password")
        void updatesPasswordHash() {
            User u = User.builder().id("u1").username("alice").passwordHash("old").role(User.Role.MEMBER).active(true).build();
            when(userRepository.findById("u1")).thenReturn(Optional.of(u));

            userService.resetPassword("u1", "newpass");

            assertThat(passwordEncoder.matches("newpass", u.getPasswordHash())).isTrue();
            verify(userRepository).save(u);
        }
    }
}
