package com.utem.utem_core.repository;

import com.utem.utem_core.entity.TestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, String> {

    List<TestRun> findByStatus(TestRun.RunStatus status);

    List<TestRun> findByNameContainingIgnoreCase(String name);

    List<TestRun> findByStartTimeBetween(Instant start, Instant end);

    List<TestRun> findAllByOrderByStartTimeDesc();

    Page<TestRun> findAllByOrderByStartTimeDesc(Pageable pageable);

    Page<TestRun> findByStatusOrderByStartTimeDesc(TestRun.RunStatus status, Pageable pageable);

    Page<TestRun> findByNameContainingIgnoreCaseOrderByStartTimeDesc(String name, Pageable pageable);

    List<TestRun> findByStartTimeBefore(Instant cutoff);

    List<TestRun> findByStatusAndStartTimeBefore(TestRun.RunStatus status, Instant threshold);

    Optional<TestRun> findBySourceRunId(String sourceRunId);

    List<TestRun> findByStatusInOrderByStartTimeDesc(Collection<TestRun.RunStatus> statuses, Pageable pageable);

    // ── Archive queries ──────────────────────────────────────────────
    Page<TestRun> findByArchivedFalseOrderByStartTimeDesc(Pageable pageable);

    Page<TestRun> findByArchivedFalseAndStatusOrderByStartTimeDesc(TestRun.RunStatus status, Pageable pageable);

    Page<TestRun> findByArchivedFalseAndNameContainingIgnoreCaseOrderByStartTimeDesc(String name, Pageable pageable);

    Page<TestRun> findByArchivedTrueOrderByStartTimeDesc(Pageable pageable);

    // ── Label queries ─────────────────────────────────────────────────
    Page<TestRun> findByArchivedFalseAndLabelOrderByStartTimeDesc(String label, Pageable pageable);

    @Query("SELECT DISTINCT r.label FROM TestRun r WHERE r.label IS NOT NULL AND r.archived = false ORDER BY r.label")
    List<String> findDistinctActiveLabels();

    // ── Job queries ───────────────────────────────────────────────────
    Page<TestRun> findByJobNameAndArchivedFalseOrderByStartTimeDesc(String jobName, Pageable pageable);

    Optional<TestRun> findTopByJobNameAndArchivedFalseOrderByStartTimeDesc(String jobName);

    long countByJobNameAndArchivedFalse(String jobName);

    @Query("SELECT DISTINCT r.jobName FROM TestRun r WHERE r.jobName IS NOT NULL AND r.archived = false ORDER BY r.jobName")
    List<String> findDistinctActiveJobNames();

    long countByStatus(TestRun.RunStatus status);

    @Query("SELECT r FROM TestRun r WHERE (:status IS NULL OR r.status = :status) " +
           "AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (CAST(:fromTime AS timestamp) IS NULL OR r.startTime >= :fromTime) " +
           "AND (CAST(:toTime AS timestamp) IS NULL OR r.startTime <= :toTime) " +
           "ORDER BY r.startTime DESC")
    Page<TestRun> searchRuns(@Param("status") TestRun.RunStatus status,
                             @Param("name") String name,
                             @Param("fromTime") Instant fromTime,
                             @Param("toTime") Instant toTime,
                             Pageable pageable);
}
