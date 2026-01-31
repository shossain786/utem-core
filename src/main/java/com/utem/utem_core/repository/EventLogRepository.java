package com.utem.utem_core.repository;

import com.utem.utem_core.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, String> {

    List<EventLog> findByRunId(String runId);

    List<EventLog> findByRunIdOrderByTimestampAsc(String runId);

    List<EventLog> findByEventType(EventLog.EventType eventType);

    List<EventLog> findByRunIdAndEventType(String runId, EventLog.EventType eventType);

    List<EventLog> findByTimestampBetween(Instant start, Instant end);

    boolean existsByEventId(String eventId);
}
