package com.utem.utem_core.controller;

import com.utem.utem_core.entity.Project;
import com.utem.utem_core.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/utem/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

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
        return projectService.createProject(request.name(), request.description());
    }

    @PostMapping("/{id}/regenerate-key")
    public Project regenerateApiKey(@PathVariable String id) {
        return projectService.regenerateApiKey(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
    }

    public record CreateProjectRequest(String name, String description) {}
}
