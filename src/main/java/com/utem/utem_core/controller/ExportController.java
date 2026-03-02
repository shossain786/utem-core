package com.utem.utem_core.controller;

import com.utem.utem_core.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/utem/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/{runId}/json")
    public ResponseEntity<String> exportJson(@PathVariable String runId) {
        log.info("Exporting run {} as JSON", runId);
        String json = exportService.exportAsJson(runId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"run-" + runId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("/{runId}/csv")
    public ResponseEntity<String> exportCsv(@PathVariable String runId) {
        log.info("Exporting run {} as CSV", runId);
        String csv = exportService.exportAsCsv(runId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"run-" + runId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/{runId}/junit-xml")
    public ResponseEntity<String> exportJunitXml(@PathVariable String runId) {
        log.info("Exporting run {} as JUnit XML", runId);
        String xml = exportService.exportAsJunitXml(runId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"run-" + runId + "-junit.xml\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @GetMapping("/{runId}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable String runId) {
        log.info("Exporting run {} as PDF", runId);
        byte[] pdf = exportService.exportAsPdf(runId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "run-" + runId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
