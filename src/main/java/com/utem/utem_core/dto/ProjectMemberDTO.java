package com.utem.utem_core.dto;

import com.utem.utem_core.entity.ProjectMember;

import java.time.Instant;

public record ProjectMemberDTO(String userId, String username, String email,
                                ProjectMember.MemberRole role, Instant createdAt) {
}
