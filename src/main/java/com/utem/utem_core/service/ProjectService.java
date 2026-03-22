package com.utem.utem_core.service;

import com.utem.utem_core.entity.Project;
import com.utem.utem_core.repository.ProjectRepository;
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

    private String generateApiKey() {
        return "utem_" + UUID.randomUUID().toString().replace("-", "");
    }
}
