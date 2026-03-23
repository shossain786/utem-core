package com.utem.utem_core.service;

import com.utem.utem_core.dto.UserDTO;
import com.utem.utem_core.entity.User;
import com.utem.utem_core.exception.ForbiddenException;
import com.utem.utem_core.repository.ProjectMemberRepository;
import com.utem.utem_core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserDTO.from(u, projectMemberRepository.findProjectIdsByUserId(u.getId())))
                .toList();
    }

    public UserDTO createUser(String username, String email, String password, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username already taken: " + username);
        }
        User user = User.builder()
                .username(username)
                .email(email != null && email.isBlank() ? null : email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .active(true)
                .build();
        User saved = userRepository.save(user);
        return UserDTO.from(saved, List.of());
    }

    public void deactivateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        user.setActive(false);
        userRepository.save(user);
    }

    public void reactivateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        user.setActive(true);
        userRepository.save(user);
    }

    public void resetPassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
