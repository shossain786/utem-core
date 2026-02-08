package com.utem.utem_core.repository;

import com.utem.utem_core.entity.TestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

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

    long countByStatus(TestRun.RunStatus status);
}
