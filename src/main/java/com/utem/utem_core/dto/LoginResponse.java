package com.utem.utem_core.dto;

import com.utem.utem_core.entity.User;

import java.util.List;

public record LoginResponse(String token, String userId, String username, User.Role role, List<String> projectIds) {
}
