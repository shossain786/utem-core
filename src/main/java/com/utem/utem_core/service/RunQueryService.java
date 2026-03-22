package com.utem.utem_core.service;

import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Project-aware query wrapper for TestRun list operations.
 *
 * <pre>
 *   allowedProjectIds == null  → SUPER_ADMIN, no project filter applied
 *   allowedProjectIds.isEmpty()→ no access, returns empty page immediately
 *   allowedProjectIds non-empty→ filter to those projects only
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class RunQueryService {

    private final TestRunRepository repo;

    public Page<TestRun> getActiveRuns(List<String> allowedProjectIds, Pageable pageable) {
        if (allowedProjectIds != null && allowedProjectIds.isEmpty()) return Page.empty(pageable);
        return allowedProjectIds == null
                ? repo.findByArchivedFalseOrderByStartTimeDesc(pageable)
                : repo.findByArchivedFalseAndProjectIdInOrderByStartTimeDesc(allowedProjectIds, pageable);
    }

    public Page<TestRun> getActiveRunsByStatus(TestRun.RunStatus status, List<String> allowedProjectIds, Pageable pageable) {
        if (allowedProjectIds != null && allowedProjectIds.isEmpty()) return Page.empty(pageable);
        return allowedProjectIds == null
                ? repo.findByArchivedFalseAndStatusOrderByStartTimeDesc(status, pageable)
                : repo.findByArchivedFalseAndStatusAndProjectIdInOrderByStartTimeDesc(status, allowedProjectIds, pageable);
    }

    public Page<TestRun> searchActiveRunsByName(String name, List<String> allowedProjectIds, Pageable pageable) {
        if (allowedProjectIds != null && allowedProjectIds.isEmpty()) return Page.empty(pageable);
        return allowedProjectIds == null
                ? repo.findByArchivedFalseAndNameContainingIgnoreCaseOrderByStartTimeDesc(name, pageable)
                : repo.findByArchivedFalseAndNameContainingIgnoreCaseAndProjectIdInOrderByStartTimeDesc(name, allowedProjectIds, pageable);
    }

    public Page<TestRun> getActiveRunsByLabel(String label, List<String> allowedProjectIds, Pageable pageable) {
        if (allowedProjectIds != null && allowedProjectIds.isEmpty()) return Page.empty(pageable);
        return allowedProjectIds == null
                ? repo.findByArchivedFalseAndLabelOrderByStartTimeDesc(label, pageable)
                : repo.findByArchivedFalseAndLabelAndProjectIdInOrderByStartTimeDesc(label, allowedProjectIds, pageable);
    }
}
