package com.utem.utem_core.controller;

import com.utem.utem_core.dto.ProjectMemberDTO;
import com.utem.utem_core.entity.Project;
import com.utem.utem_core.entity.ProjectMember;
import com.utem.utem_core.exception.ForbiddenException;
import com.utem.utem_core.security.AuthenticatedUser;
import com.utem.utem_core.security.UserContextHolder;
import com.utem.utem_core.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/utem/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public Project getProject(@PathVariable String id) {
        return projectService.getProject(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project createProject(@RequestBody CreateProjectRequest request) {
        requireSuperAdmin();
        return projectService.createProject(request.name(), request.description());
    }

    @PostMapping("/{id}/regenerate-key")
    public Project regenerateApiKey(@PathVariable String id) {
        requireSuperAdmin();
        return projectService.regenerateApiKey(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable String id) {
        requireSuperAdmin();
        projectService.deleteProject(id);
    }

    // ── Member management ──────────────────────────────────────────────────

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMemberDTO>> getMembers(@PathVariable String id) {
        requireSuperAdmin();
        return ResponseEntity.ok(projectService.getProjectMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectMemberDTO> addMember(@PathVariable String id,
                                                       @RequestBody Map<String, String> body) {
        requireSuperAdmin();
        String userId = body.get("userId");
        ProjectMember.MemberRole role = ProjectMember.MemberRole.valueOf(
                body.getOrDefault("role", "VIEWER"));
        return ResponseEntity.ok(projectService.addMember(id, userId, role));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable String id, @PathVariable String userId) {
        requireSuperAdmin();
        projectService.removeMember(id, userId);
    }

    private void requireSuperAdmin() {
        if (!securityEnabled) return;
        AuthenticatedUser user = UserContextHolder.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new ForbiddenException("Super admin access required");
        }
    }

    public record CreateProjectRequest(String name, String description) {}
}
