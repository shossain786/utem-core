package com.utem.utem_core.controller;

import com.utem.utem_core.dto.EventRequest;
import com.utem.utem_core.dto.EventResponse;
import com.utem.utem_core.entity.EventLog;
import com.utem.utem_core.repository.EventLogRepository;
import com.utem.utem_core.service.EventProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/utem/events")
@RequiredArgsConstructor
public class EventController {

    private final EventLogRepository eventLogRepository;
    private final EventProcessingService eventProcessingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EventResponse> ingestEvent(@Valid @RequestBody EventRequest request) {
        if (eventLogRepository.existsByEventId(request.eventId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        EventLog eventLog = EventLog.builder()
            .eventId(request.eventId())
            .runId(request.runId())
            .eventType(request.eventType())
            .parentId(request.parentId())
            .timestamp(request.timestamp())
            .payload(request.payload())
            .receivedAt(Instant.now())
            .build();

        EventLog saved = eventLogRepository.save(eventLog);
        eventProcessingService.processEvent(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(saved));
    }

    private static final int MAX_BATCH_SIZE = 500;

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<List<EventResponse>> ingestEvents(
            @Valid @RequestBody List<EventRequest> requests) {
        if (requests.size() > MAX_BATCH_SIZE) {
            return ResponseEntity.badRequest().build();
        }
        List<EventLog> eventLogs = requests.stream()
            .filter(request -> !eventLogRepository.existsByEventId(request.eventId()))
            .map(request -> EventLog.builder()
                .eventId(request.eventId())
                .runId(request.runId())
                .eventType(request.eventType())
                .parentId(request.parentId())
                .timestamp(request.timestamp())
                .payload(request.payload())
                .receivedAt(Instant.now())
                .build())
            .toList();

        List<EventLog> saved = eventLogRepository.saveAll(eventLogs);
        saved.forEach(eventProcessingService::processEvent);
        List<EventResponse> responses = saved.stream()
            .map(EventResponse::from)
            .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable String id) {
        return eventLogRepository.findById(id)
            .map(EventResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsByRunId(@RequestParam String runId) {
        List<EventResponse> events = eventLogRepository.findByRunIdOrderByTimestampAsc(runId)
            .stream()
            .map(EventResponse::from)
            .toList();

        return ResponseEntity.ok(events);
    }

    @GetMapping("/type/{eventType}")
    public ResponseEntity<List<EventResponse>> getEventsByType(
            @PathVariable EventLog.EventType eventType,
            @RequestParam(required = false) String runId) {
        List<EventLog> events;
        if (runId != null) {
            events = eventLogRepository.findByRunIdAndEventType(runId, eventType);
        } else {
            events = eventLogRepository.findByEventType(eventType);
        }

        List<EventResponse> responses = events.stream()
            .map(EventResponse::from)
            .toList();

        return ResponseEntity.ok(responses);
    }
}
