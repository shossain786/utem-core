package com.utem.reporter.junit5;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Async event queue that batches and sends events to UTEM Core in a background thread.
 * Decouples test execution from network I/O so test performance is unaffected.
 */
public final class EventQueue {

    private static final int CAPACITY = 10_000;
    private static final int BATCH_SIZE = 50;
    private static final long DRAIN_INTERVAL_MS = 200;
    private static final long FLUSH_TIMEOUT_MS = 30_000;

    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(CAPACITY);
    private final UtemHttpClient httpClient;
    private final Thread drainThread;
    private volatile boolean running = true;

    public EventQueue(UtemHttpClient httpClient) {
        this.httpClient = httpClient;
        this.drainThread = new Thread(this::drainLoop, "utem-event-drain");
        this.drainThread.setDaemon(true);
        this.drainThread.start();
    }

    /**
     * Enqueue a JSON event for async sending. Non-blocking.
     * If the queue is full, the event is logged and dropped rather than blocking test execution.
     */
    public void enqueue(String json) {
        if (!queue.offer(json)) {
            System.err.println("[UTEM] Event queue full — dropping event. Consider increasing capacity or checking server connectivity.");
        }
    }

    /**
     * Flush all remaining events synchronously. Call at end of test run.
     * Blocks until queue is empty or timeout (30s) is reached.
     */
    public void flush() {
        running = false;
        drainRemaining();

        // Wait for drain thread to finish
        try {
            drainThread.join(FLUSH_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Internal ────────────────────────────────────────────────────

    private void drainLoop() {
        while (running) {
            try {
                Thread.sleep(DRAIN_INTERVAL_MS);
                drainBatch();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Final drain after running=false
        drainRemaining();
    }

    private void drainBatch() {
        List<String> batch = new ArrayList<>(BATCH_SIZE);
        queue.drainTo(batch, BATCH_SIZE);
        if (batch.isEmpty()) return;

        boolean sent = httpClient.sendBatch(batch);
        if (!sent) {
            // Re-queue failed events (best effort, avoid infinite loop with capacity check)
            for (String json : batch) {
                if (!queue.offer(json)) break;
            }
        }
    }

    private void drainRemaining() {
        while (!queue.isEmpty()) {
            drainBatch();
        }
    }
}
