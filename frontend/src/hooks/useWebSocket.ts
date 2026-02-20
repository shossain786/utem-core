import { useCallback, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useWebSocketContext } from '@/contexts/WebSocketContext';
import websocketService from '@/services/websocket';
import type { ConnectionStatus } from '@/services/websocket';
import type { WebSocketEventMessage, RunSummaryMessage, EventType } from '@/api/types';

const TERMINAL_EVENTS: Set<EventType> = new Set([
  'TEST_RUN_FINISHED',
  'TEST_SUITE_FINISHED',
  'TEST_CASE_FINISHED',
  'TEST_PASSED',
  'TEST_FAILED',
  'TEST_SKIPPED',
]);

/**
 * Returns the current WebSocket connection status and reconnect count.
 */
export function useWebSocket(): { status: ConnectionStatus; reconnectCount: number } {
  const { status, reconnectCount } = useWebSocketContext();
  return { status, reconnectCount };
}

/**
 * Subscribes to real-time events for a specific run.
 * Manages subscription lifecycle and reconnects automatically.
 * Invalidates React Query caches on terminal events.
 */
export function useRunEvents(runId: string | null | undefined) {
  const [events, setEvents] = useState<WebSocketEventMessage[]>([]);
  const [lastEvent, setLastEvent] = useState<WebSocketEventMessage | null>(null);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!runId) return;

    const destination = `/topic/runs/${runId}/events`;

    const unsubscribe = websocketService.subscribeSafe<WebSocketEventMessage>(
      destination,
      (message) => {
        setLastEvent(message);
        setEvents((prev) => [...prev, message]);

        if (TERMINAL_EVENTS.has(message.eventType)) {
          queryClient.invalidateQueries({ queryKey: ['runs'] });
        }
        if (message.eventType === 'TEST_RUN_FINISHED') {
          queryClient.invalidateQueries({ queryKey: ['runs', 'summary'] });
        }
      },
    );

    return () => {
      unsubscribe();
      setEvents([]);
      setLastEvent(null);
    };
  }, [runId, queryClient]);

  const clearEvents = useCallback(() => {
    setEvents([]);
    setLastEvent(null);
  }, []);

  return { events, lastEvent, clearEvents };
}

/**
 * Subscribes to run summary updates for a specific run.
 * Returns the latest aggregate state (total/passed/failed/skipped).
 */
export function useRunSummary(runId: string | null | undefined) {
  const [summary, setSummary] = useState<RunSummaryMessage | null>(null);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!runId) return;

    const destination = `/topic/runs/${runId}/summary`;

    const unsubscribe = websocketService.subscribeSafe<RunSummaryMessage>(
      destination,
      (message) => {
        setSummary(message);
        queryClient.invalidateQueries({ queryKey: ['runs'] });
      },
    );

    return () => {
      unsubscribe();
      setSummary(null);
    };
  }, [runId, queryClient]);

  return { summary };
}
