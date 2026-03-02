package com.utem.utem_core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    // ============ PDF ============

    public byte[] exportAsPdf(String runId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new TestRunNotFoundException(runId));

        TestRunHierarchyDTO hierarchy = hierarchyReconstructionService.getFullHierarchy(runId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── Title ──
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            Paragraph title = new Paragraph(run.getName(), titleFont);
            title.setSpacingAfter(4);
            doc.add(title);

            Paragraph subtitle = new Paragraph("Test Run Report", FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY));
            subtitle.setSpacingAfter(16);
            doc.add(subtitle);

            // ── Summary table ──
            boolean passed = run.getStatus() == TestRun.RunStatus.PASSED;
            Color statusColor = passed ? new Color(0, 168, 107) : new Color(220, 38, 38);

            int total   = run.getTotalTests()   != null ? run.getTotalTests()   : 0;
            int passedN = run.getPassedTests()  != null ? run.getPassedTests()  : 0;
            int failedN = run.getFailedTests()  != null ? run.getFailedTests()  : 0;
            int skipped = run.getSkippedTests() != null ? run.getSkippedTests() : 0;
            double passRate = total > 0 ? (passedN * 100.0 / total) : 0.0;

            String durationStr = "--";
            if (run.getStartTime() != null && run.getEndTime() != null) {
                long secs = java.time.Duration.between(run.getStartTime(), run.getEndTime()).toSeconds();
                durationStr = secs >= 60 ? (secs / 60) + "m " + (secs % 60) + "s" : secs + "s";
            }

            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(60);
            summary.setHorizontalAlignment(Element.ALIGN_LEFT);
            summary.setSpacingAfter(16);
            summary.setWidths(new float[]{2f, 3f});

            addSummaryRow(summary, "Status", run.getStatus().name(), labelFont, valueFont, statusColor);
            addSummaryRow(summary, "Total Tests", String.valueOf(total), labelFont, valueFont, null);
            addSummaryRow(summary, "Passed", String.valueOf(passedN), labelFont, valueFont, new Color(0, 168, 107));
            addSummaryRow(summary, "Failed", String.valueOf(failedN), labelFont, valueFont, failedN > 0 ? new Color(220, 38, 38) : null);
            if (skipped > 0) addSummaryRow(summary, "Skipped", String.valueOf(skipped), labelFont, valueFont, Color.GRAY);
            addSummaryRow(summary, "Pass Rate", String.format("%.1f%%", passRate), labelFont, valueFont, null);
            addSummaryRow(summary, "Duration", durationStr, labelFont, valueFont, null);
            if (run.getJobName() != null) addSummaryRow(summary, "Job", run.getJobName(), labelFont, valueFont, null);
            if (run.getLabel() != null)   addSummaryRow(summary, "Label", run.getLabel(), labelFont, valueFont, null);
            if (run.getStartTime() != null) {
                String started = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                        .withZone(ZoneId.of("UTC"))
                        .format(run.getStartTime());
                addSummaryRow(summary, "Started", started, labelFont, valueFont, null);
            }
            doc.add(summary);

            // ── Test results table ──
            Paragraph sectionTitle = new Paragraph("Test Results", sectionFont);
            sectionTitle.setSpacingAfter(8);
            doc.add(sectionTitle);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1.5f, 1f, 1.5f});
            table.setSpacingAfter(16);

            addTableHeader(table, labelFont, "Name", "Status", "Type", "Duration");

            for (HierarchyNodeDTO node : hierarchy.rootNodes()) {
                addNodeRows(table, node, 0, smallFont);
            }
            doc.add(table);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            log.error("PDF generation failed for run {}", runId, e);
            throw new RuntimeException("PDF export failed", e);
        } catch (java.io.IOException e) {
            log.error("PDF I/O error for run {}", runId, e);
            throw new RuntimeException("PDF export failed", e);
        }
    }

    private void addSummaryRow(PdfPTable table, String label, String value,
                               Font labelFont, Font valueFont, Color valueColor) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(4);
        table.addCell(labelCell);

        Font vf = valueColor != null ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, valueColor) : valueFont;
        PdfPCell valueCell = new PdfPCell(new Phrase(value, vf));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(4);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, Font font, String... headers) {
        Color headerBg = new Color(243, 244, 246);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addNodeRows(PdfPTable table, HierarchyNodeDTO node, int depth, Font font) {
        String indent = "  ".repeat(depth);
        String statusStr = node.status().name();
        Color statusColor = switch (statusStr) {
            case "PASSED"  -> new Color(0, 168, 107);
            case "FAILED"  -> new Color(220, 38, 38);
            case "SKIPPED" -> Color.GRAY;
            default        -> Color.BLACK;
        };
        String durStr = node.duration() != null ? node.duration() + " ms" : "--";

        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, statusColor);

        PdfPCell nameCell = new PdfPCell(new Phrase(indent + node.name(), nameFont));
        nameCell.setPadding(4);
        table.addCell(nameCell);

        PdfPCell statusCell = new PdfPCell(new Phrase(statusStr, statusFont));
        statusCell.setPadding(4);
        table.addCell(statusCell);

        PdfPCell typeCell = new PdfPCell(new Phrase(node.nodeType().name(), font));
        typeCell.setPadding(4);
        table.addCell(typeCell);

        PdfPCell durCell = new PdfPCell(new Phrase(durStr, font));
        durCell.setPadding(4);
        table.addCell(durCell);

        for (HierarchyNodeDTO child : node.children()) {
            addNodeRows(table, child, depth + 1, font);
        }
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
