package com.utem.utem_core.repository;

import com.utem.utem_core.entity.ProjectMember;
import com.utem.utem_core.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    @Query("SELECT m.id.projectId FROM ProjectMember m WHERE m.id.userId = :userId")
    List<String> findProjectIdsByUserId(@Param("userId") String userId);

    List<ProjectMember> findByIdProjectId(String projectId);

    void deleteByIdUserIdAndIdProjectId(String userId, String projectId);
}
