package com.utem.utem_core.controller;

import com.utem.utem_core.dto.ChangePasswordRequest;
import com.utem.utem_core.dto.LoginRequest;
import com.utem.utem_core.dto.LoginResponse;
import com.utem.utem_core.dto.UserDTO;
import com.utem.utem_core.exception.UnauthorizedException;
import com.utem.utem_core.repository.ProjectMemberRepository;
import com.utem.utem_core.security.AuthenticatedUser;
import com.utem.utem_core.security.UserContextHolder;
import com.utem.utem_core.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/utem/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProjectMemberRepository projectMemberRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me() {
        AuthenticatedUser user = UserContextHolder.get();
        if (user == null) throw new UnauthorizedException("Not authenticated");
        return ResponseEntity.ok(new UserDTO(user.userId(), user.username(), null,
                user.role(), true, null, user.projectIds()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedUser user = UserContextHolder.get();
        if (user == null) throw new UnauthorizedException("Not authenticated");
        authService.changePassword(user.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
