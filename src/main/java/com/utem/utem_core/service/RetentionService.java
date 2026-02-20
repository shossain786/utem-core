package com.utem.utem_core.service;

import com.utem.utem_core.config.RetentionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled service that automatically deletes old test run data
 * based on the configured retention policy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionService {

    private final RunHistoryService runHistoryService;
    private final RetentionProperties retentionProperties;

    @Scheduled(cron = "${utem.retention.cron-expression:0 0 2 * * *}")
    public void cleanupOldRuns() {
        if (!retentionProperties.enabled()) {
            log.debug("Data retention is disabled — skipping cleanup");
            return;
        }

        Instant cutoff = Instant.now().minus(retentionProperties.retentionDays(), ChronoUnit.DAYS);
        log.info("Running data retention cleanup: deleting runs older than {} days (before {})",
                retentionProperties.retentionDays(), cutoff);

        try {
            int deleted = runHistoryService.deleteRunsBefore(cutoff);
            log.info("Data retention cleanup complete: deleted {} runs", deleted);
        } catch (Exception e) {
            log.error("Data retention cleanup failed: {}", e.getMessage(), e);
        }
    }
}
