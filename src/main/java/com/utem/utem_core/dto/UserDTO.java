package com.utem.utem_core.dto;

import com.utem.utem_core.entity.User;

import java.time.Instant;
import java.util.List;

public record UserDTO(String id, String username, String email, User.Role role, boolean active,
                      Instant createdAt, List<String> projectIds) {

    public static UserDTO from(User user, List<String> projectIds) {
        return new UserDTO(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole(), user.isActive(), user.getCreatedAt(), projectIds);
    }
}
