package com.utem.utem_core.repository;

import com.utem.utem_core.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, String> {
    Optional<Project> findByApiKeyAndActiveTrue(String apiKey);
    List<Project> findAllByOrderByCreatedAtDesc();
    List<Project> findByIdInOrderByCreatedAtDesc(List<String> ids);
    boolean existsByName(String name);
}
