package com.utem.utem_core.repository;

import com.utem.utem_core.entity.TestRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TestRunRepositoryTest {

    @Autowired
    private TestRunRepository testRunRepository;

    private TestRun testRun;

    @BeforeEach
    void setUp() {
        testRunRepository.deleteAll();

        testRun = TestRun.builder()
                .name("Integration Test Suite")
                .startTime(Instant.now())
                .status(TestRun.RunStatus.RUNNING)
                .totalTests(10)
                .passedTests(0)
                .failedTests(0)
                .skippedTests(0)
                .metadata("{\"env\": \"test\"}")
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve test run by ID")
    void shouldSaveAndFindById() {
        TestRun saved = testRunRepository.save(testRun);

        Optional<TestRun> found = testRunRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Integration Test Suite");
        assertThat(found.get().getStatus()).isEqualTo(TestRun.RunStatus.RUNNING);
    }

    @Test
    @DisplayName("Should find test runs by status")
    void shouldFindByStatus() {
        testRunRepository.save(testRun);

        TestRun completedRun = TestRun.builder()
                .name("Completed Suite")
                .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
                .endTime(Instant.now())
                .status(TestRun.RunStatus.PASSED)
                .totalTests(5)
                .passedTests(5)
                .failedTests(0)
                .skippedTests(0)
                .build();
        testRunRepository.save(completedRun);

        List<TestRun> runningRuns = testRunRepository.findByStatus(TestRun.RunStatus.RUNNING);
        List<TestRun> passedRuns = testRunRepository.findByStatus(TestRun.RunStatus.PASSED);

        assertThat(runningRuns).hasSize(1);
        assertThat(runningRuns.get(0).getName()).isEqualTo("Integration Test Suite");
        assertThat(passedRuns).hasSize(1);
        assertThat(passedRuns.get(0).getName()).isEqualTo("Completed Suite");
    }

    @Test
    @DisplayName("Should find test runs by name containing string")
    void shouldFindByNameContaining() {
        testRunRepository.save(testRun);

        TestRun unitTestRun = TestRun.builder()
                .name("Unit Test Suite")
                .startTime(Instant.now())
                .status(TestRun.RunStatus.RUNNING)
                .build();
        testRunRepository.save(unitTestRun);

        List<TestRun> results = testRunRepository.findByNameContainingIgnoreCase("integration");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Integration Test Suite");
    }

    @Test
    @DisplayName("Should find test runs within time range")
    void shouldFindByStartTimeBetween() {
        Instant now = Instant.now();
        testRun.setStartTime(now);
        testRunRepository.save(testRun);

        TestRun oldRun = TestRun.builder()
                .name("Old Run")
                .startTime(now.minus(2, ChronoUnit.DAYS))
                .status(TestRun.RunStatus.PASSED)
                .build();
        testRunRepository.save(oldRun);

        List<TestRun> recentRuns = testRunRepository.findByStartTimeBetween(
                now.minus(1, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.HOURS)
        );

        assertThat(recentRuns).hasSize(1);
        assertThat(recentRuns.get(0).getName()).isEqualTo("Integration Test Suite");
    }

    @Test
    @DisplayName("Should find all test runs ordered by start time descending")
    void shouldFindAllOrderByStartTimeDesc() {
        Instant now = Instant.now();

        TestRun run1 = TestRun.builder()
                .name("First Run")
                .startTime(now.minus(2, ChronoUnit.HOURS))
                .status(TestRun.RunStatus.PASSED)
                .build();

        TestRun run2 = TestRun.builder()
                .name("Second Run")
                .startTime(now.minus(1, ChronoUnit.HOURS))
                .status(TestRun.RunStatus.PASSED)
                .build();

        TestRun run3 = TestRun.builder()
                .name("Third Run")
                .startTime(now)
                .status(TestRun.RunStatus.RUNNING)
                .build();

        testRunRepository.save(run1);
        testRunRepository.save(run2);
        testRunRepository.save(run3);

        List<TestRun> runs = testRunRepository.findAllByOrderByStartTimeDesc();

        assertThat(runs).hasSize(3);
        assertThat(runs.get(0).getName()).isEqualTo("Third Run");
        assertThat(runs.get(1).getName()).isEqualTo("Second Run");
        assertThat(runs.get(2).getName()).isEqualTo("First Run");
    }

    @Test
    @DisplayName("Should update test run status")
    void shouldUpdateStatus() {
        TestRun saved = testRunRepository.save(testRun);

        saved.setStatus(TestRun.RunStatus.PASSED);
        saved.setEndTime(Instant.now());
        saved.setPassedTests(10);
        testRunRepository.save(saved);

        Optional<TestRun> updated = testRunRepository.findById(saved.getId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(TestRun.RunStatus.PASSED);
        assertThat(updated.get().getEndTime()).isNotNull();
        assertThat(updated.get().getPassedTests()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should delete test run")
    void shouldDeleteTestRun() {
        TestRun saved = testRunRepository.save(testRun);
        String id = saved.getId();

        testRunRepository.deleteById(id);

        Optional<TestRun> deleted = testRunRepository.findById(id);
        assertThat(deleted).isEmpty();
    }
}
