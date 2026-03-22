package com.utem.utem_core.service;

import com.utem.utem_core.dto.ProjectMemberDTO;
import com.utem.utem_core.entity.Project;
import com.utem.utem_core.entity.ProjectMember;
import com.utem.utem_core.entity.ProjectMemberId;
import com.utem.utem_core.entity.User;
import com.utem.utem_core.repository.ProjectMemberRepository;
import com.utem.utem_core.repository.ProjectRepository;
import com.utem.utem_core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Project getProject(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
    }

    @Transactional
    public Project createProject(String name, String description) {
        if (projectRepository.existsByName(name)) {
            throw new IllegalStateException("Project with name '" + name + "' already exists");
        }
        Project project = Project.builder()
                .name(name)
                .description(description)
                .apiKey(generateApiKey())
                .build();
        return projectRepository.save(project);
    }

    @Transactional
    public Project regenerateApiKey(String id) {
        Project project = getProject(id);
        project.setApiKey(generateApiKey());
        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(String id) {
        Project project = getProject(id);
        project.setActive(false);
        projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberDTO> getProjectMembers(String projectId) {
        return projectMemberRepository.findByIdProjectId(projectId).stream()
                .map(m -> {
                    User user = userRepository.findById(m.getId().getUserId()).orElse(null);
                    String username = user != null ? user.getUsername() : "unknown";
                    String email = user != null ? user.getEmail() : null;
                    return new ProjectMemberDTO(m.getId().getUserId(), username, email,
                            m.getRole(), m.getCreatedAt());
                })
                .toList();
    }

    @Transactional
    public ProjectMemberDTO addMember(String projectId, String userId, ProjectMember.MemberRole role) {
        getProject(projectId); // validates project exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ProjectMember member = ProjectMember.builder()
                .id(new ProjectMemberId(userId, projectId))
                .role(role)
                .build();
        projectMemberRepository.save(member);
        return new ProjectMemberDTO(userId, user.getUsername(), user.getEmail(), role, member.getCreatedAt());
    }

    @Transactional
    public void removeMember(String projectId, String userId) {
        projectMemberRepository.deleteByIdUserIdAndIdProjectId(userId, projectId);
    }

    private String generateApiKey() {
        return "utem_" + UUID.randomUUID().toString().replace("-", "");
    }
}
