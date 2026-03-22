package com.utem.utem_core.security;

import com.utem.utem_core.entity.User;

import java.util.List;

public record AuthenticatedUser(String userId, String username, User.Role role, List<String> projectIds) {

    public boolean isSuperAdmin() {
        return role == User.Role.SUPER_ADMIN;
    }

    public boolean canAccessProject(String projectId) {
        return isSuperAdmin() || (projectIds != null && projectIds.contains(projectId));
    }
}
