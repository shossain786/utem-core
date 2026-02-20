package com.utem.utem_core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utem.utem_core.dto.HierarchyNodeDTO;
import com.utem.utem_core.dto.TestRunHierarchyDTO;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.exception.TestRunNotFoundException;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that generates downloadable exports in JSON, CSV, and JUnit XML formats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExportService {

    private final TestRunRepository testRunRepository;
    private final TestNodeRepository testNodeRepository;
    private final HierarchyReconstructionService hierarchyReconstructionService;
    private final ObjectMapper objectMapper;

    // ============ JSON ============

    public String exportAsJson(String runId) {
        TestRunHierarchyDTO hierarchy = hierarchyReconstructionService.getFullHierarchy(runId);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(hierarchy);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize run {} to JSON", runId, e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    // ============ CSV ============

    public String exportAsCsv(String runId) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        List<TestNode> nodes = testNodeRepository.findByTestRunId(runId);

        StringBuilder sb = new StringBuilder();
        sb.append("id,node_type,name,status,parent_id,duration_ms,start_time,end_time,flaky,retry_count\n");

        for (TestNode node : nodes) {
            appendCsvRow(sb,
                    node.getId(),
                    node.getNodeType().name(),
                    escapeCsv(node.getName()),
                    node.getStatus().name(),
                    node.getParent() != null ? node.getParent().getId() : "",
                    node.getDuration() != null ? node.getDuration().toString() : "",
                    node.getStartTime() != null ? node.getStartTime().toString() : "",
                    node.getEndTime() != null ? node.getEndTime().toString() : "",
                    node.getFlaky() != null ? node.getFlaky().toString() : "false",
                    node.getRetryCount() != null ? node.getRetryCount().toString() : "0"
            );
        }

        return sb.toString();
    }

    // ============ JUnit XML ============

    public String exportAsJunitXml(String runId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new TestRunNotFoundException(runId));

        TestRunHierarchyDTO hierarchy = hierarchyReconstructionService.getFullHierarchy(runId);

        int totalTests = hierarchy.statistics().totalNodes();
        int failures = hierarchy.statistics().failedNodes();
        int skipped = hierarchy.statistics().skippedNodes();
        double totalTimeSec = hierarchy.statistics().totalDuration() / 1000.0;

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format(
                "<testsuites name=\"%s\" tests=\"%d\" failures=\"%d\" errors=\"0\" skipped=\"%d\" time=\"%.3f\">\n",
                escapeXml(run.getName()), totalTests, failures, skipped, totalTimeSec
        ));

        for (HierarchyNodeDTO rootNode : hierarchy.rootNodes()) {
            appendTestSuite(sb, rootNode, "  ");
        }

        sb.append("</testsuites>\n");
        return sb.toString();
    }

    // ============ Private Helpers ============

    private void appendTestSuite(StringBuilder sb, HierarchyNodeDTO suite, String indent) {
        int tests = countDescendantTestCases(suite);
        int failures = countDescendantByStatus(suite, "FAILED");
        int skipped = countDescendantByStatus(suite, "SKIPPED");
        double timeSec = suite.duration() != null ? suite.duration() / 1000.0 : 0.0;

        sb.append(String.format(
                "%s<testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" errors=\"0\" skipped=\"%d\" time=\"%.3f\">\n",
                indent, escapeXml(suite.name()), tests, failures, skipped, timeSec
        ));

        for (HierarchyNodeDTO child : suite.children()) {
            if (child.nodeType().name().equals("SUITE") || child.nodeType().name().equals("FEATURE")) {
                // Nested suite — flatten into parent testsuite by emitting test cases directly
                appendTestCasesFromSuite(sb, child, escapeXml(suite.name()), indent + "  ");
            } else {
                // Leaf test case
                appendTestCase(sb, child, escapeXml(suite.name()), indent + "  ");
            }
        }

        sb.append(indent).append("</testsuite>\n");
    }

    private void appendTestCasesFromSuite(StringBuilder sb, HierarchyNodeDTO suite, String classname, String indent) {
        for (HierarchyNodeDTO child : suite.children()) {
            if (child.nodeType().name().equals("SUITE") || child.nodeType().name().equals("FEATURE")) {
                appendTestCasesFromSuite(sb, child, classname, indent);
            } else {
                appendTestCase(sb, child, classname, indent);
            }
        }
        // If suite itself has no children, emit it as a test case
        if (suite.children().isEmpty()) {
            appendTestCase(sb, suite, classname, indent);
        }
    }

    private void appendTestCase(StringBuilder sb, HierarchyNodeDTO node, String classname, String indent) {
        double timeSec = node.duration() != null ? node.duration() / 1000.0 : 0.0;
        sb.append(String.format(
                "%s<testcase name=\"%s\" classname=\"%s\" time=\"%.3f\"",
                indent, escapeXml(node.name()), classname, timeSec
        ));

        String status = node.status().name();
        if ("FAILED".equals(status)) {
            sb.append(">\n");
            sb.append(indent).append("  <failure message=\"Test failed\" type=\"FAILURE\">");
            // Include step error messages if available
            String errorDetail = node.steps().stream()
                    .filter(s -> s.errorMessage() != null && !s.errorMessage().isBlank())
                    .map(s -> escapeXml(s.errorMessage()))
                    .findFirst()
                    .orElse("No error details");
            sb.append(errorDetail);
            sb.append("</failure>\n");
            sb.append(indent).append("</testcase>\n");
        } else if ("SKIPPED".equals(status)) {
            sb.append(">\n");
            sb.append(indent).append("  <skipped/>\n");
            sb.append(indent).append("</testcase>\n");
        } else {
            sb.append("/>\n");
        }
    }

    private int countDescendantTestCases(HierarchyNodeDTO node) {
        if (node.children().isEmpty()) return 1;
        return node.children().stream().mapToInt(this::countDescendantTestCases).sum();
    }

    private int countDescendantByStatus(HierarchyNodeDTO node, String status) {
        if (node.children().isEmpty()) {
            return node.status().name().equals(status) ? 1 : 0;
        }
        return node.children().stream().mapToInt(c -> countDescendantByStatus(c, status)).sum();
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static void appendCsvRow(StringBuilder sb, String... fields) {
        sb.append(String.join(",", fields)).append("\n");
    }
}
