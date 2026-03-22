package com.utem.utem_core.controller;

import com.utem.utem_core.dto.UserDTO;
import com.utem.utem_core.entity.User;
import com.utem.utem_core.exception.ForbiddenException;
import com.utem.utem_core.security.AuthenticatedUser;
import com.utem.utem_core.security.UserContextHolder;
import com.utem.utem_core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/utem/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsers() {
        requireSuperAdmin();
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody Map<String, String> body) {
        requireSuperAdmin();
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String roleStr = body.getOrDefault("role", "MEMBER");
        User.Role role = User.Role.valueOf(roleStr);
        return ResponseEntity.ok(userService.createUser(username, email, password, role));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deactivateUser(@PathVariable String userId) {
        requireSuperAdmin();
        userService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable String userId) {
        requireSuperAdmin();
        userService.reactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable String userId,
                                               @RequestBody Map<String, String> body) {
        requireSuperAdmin();
        userService.resetPassword(userId, body.get("newPassword"));
        return ResponseEntity.noContent().build();
    }

    private void requireSuperAdmin() {
        if (!securityEnabled) return;
        AuthenticatedUser user = UserContextHolder.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new ForbiddenException("Super admin access required");
        }
    }
}
