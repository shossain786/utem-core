package com.utem.utem_core.service;

import com.utem.utem_core.config.RecoveryProperties;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled service that detects runs stuck in RUNNING state — e.g. after a CI pipeline crash
 * or a server restart — and marks them as ABORTED so the dashboard doesn't show them as active.
 *
 * <p>Runs whose {@code startTime} is older than {@link RecoveryProperties#staleThresholdMinutes()}
 * and whose status is still {@code RUNNING} are considered stale.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RunRecoveryService {

    private final TestRunRepository testRunRepository;
    private final RecoveryProperties recoveryProperties;

    @Scheduled(fixedDelayString = "${utem.recovery.check-interval-ms:300000}")
    public void detectStaleRuns() {
        if (!recoveryProperties.enabled()) {
            log.debug("Run recovery is disabled — skipping stale run check");
            return;
        }

        Instant staleThreshold = Instant.now()
                .minus(recoveryProperties.staleThresholdMinutes(), ChronoUnit.MINUTES);

        List<TestRun> staleRuns = testRunRepository
                .findByStatusAndStartTimeBefore(TestRun.RunStatus.RUNNING, staleThreshold);

        if (staleRuns.isEmpty()) return;

        log.info("Recovery: found {} stale RUNNING run(s), marking as ABORTED", staleRuns.size());

        for (TestRun run : staleRuns) {
            run.setStatus(TestRun.RunStatus.ABORTED);
            run.setEndTime(Instant.now());
            testRunRepository.save(run);
            log.info("Marked run {} ('{}') as ABORTED (started at {}, threshold was {} min)",
                    run.getId(), run.getName(), run.getStartTime(),
                    recoveryProperties.staleThresholdMinutes());
        }
    }
}
