package com.utem.utem_core.service;

import com.utem.utem_core.dto.ChangePasswordRequest;
import com.utem.utem_core.dto.LoginRequest;
import com.utem.utem_core.dto.LoginResponse;
import com.utem.utem_core.entity.User;
import com.utem.utem_core.exception.UnauthorizedException;
import com.utem.utem_core.repository.ProjectMemberRepository;
import com.utem.utem_core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        List<String> projectIds = user.getRole() == User.Role.SUPER_ADMIN
                ? null
                : projectMemberRepository.findProjectIdsByUserId(user.getId());

        String token = jwtService.generateToken(user, projectIds);
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole(), projectIds);
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
