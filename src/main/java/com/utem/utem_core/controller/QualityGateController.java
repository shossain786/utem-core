package com.utem.utem_core.controller;

import com.utem.utem_core.dto.QualityGateResultDTO;
import com.utem.utem_core.service.QualityGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/utem")
@RequiredArgsConstructor
@Slf4j
public class QualityGateController {

    private final QualityGateService qualityGateService;

    /**
     * Evaluate quality gates for a run.
     * Returns HTTP 200 if all gates pass, 422 if any gate fails.
     * Use with {@code curl --fail} in CI to automatically exit non-zero on failure.
     */
    @GetMapping("/quality-gate/{runId}")
    public ResponseEntity<QualityGateResultDTO> evaluate(
            @PathVariable String runId,
            @RequestParam(defaultValue = "100.0") double maxFailRate,
            @RequestParam(defaultValue = "100.0") double maxFlakinessScore,
            @RequestParam(defaultValue = "-1")    int maxNewFailures,
            @RequestParam(required = false)        String baselineRunId) {

        QualityGateResultDTO result = qualityGateService.evaluate(
                runId, maxFailRate, maxFlakinessScore, maxNewFailures, baselineRunId);

        HttpStatus status = result.passed() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Get a markdown-formatted CI summary for a run.
     * Suitable for posting as a PR comment body.
     */
    @GetMapping(value = "/runs/{runId}/ci-summary", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getCiSummary(@PathVariable String runId) {
        return ResponseEntity.ok(qualityGateService.generateCiSummary(runId));
    }
}
