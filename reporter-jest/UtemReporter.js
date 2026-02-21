'use strict';

/**
 * UTEM Reporter for Jest.
 *
 * Streams test events to UTEM Core as tests run. Uses only Node.js built-ins
 * (http/https, crypto) — zero external dependencies.
 *
 * Usage in jest.config.js:
 *   reporters: [
 *     "default",
 *     ["utem-jest-reporter", { serverUrl: "http://localhost:8080/utem" }]
 *   ]
 *
 * Configuration (in order of precedence):
 *   1. options.serverUrl  (in jest.config.js)
 *   2. UTEM_SERVER_URL    (environment variable)
 *   3. http://localhost:8080/utem  (default)
 */

const http  = require('http');
const https = require('https');
const { randomUUID } = require('crypto');

const BATCH_SIZE    = 50;
const DRAIN_MS      = 200;
const FLUSH_TIMEOUT = 30_000;
const MAX_RETRIES   = 3;
const RETRY_DELAYS  = [100, 500, 2000];

class UtemReporter {
  constructor(globalConfig, options = {}) {
    const rawUrl = options.serverUrl
      || process.env.UTEM_SERVER_URL
      || 'http://localhost:8080/utem';
    this._baseUrl = rawUrl.replace(/\/+$/, '');

    this._runId       = randomUUID();
    this._runEventId  = randomUUID();

    /** testFilePath → suiteEventId */
    this._suiteIds = new Map();
    /** fullName → caseEventId */
    this._caseIds  = new Map();

    this._total   = 0;
    this._passed  = 0;
    this._failed  = 0;
    this._skipped = 0;

    /** @type {string[]} */
    this._eventQueue = [];
    this._drainTimer = null;
    this._flushPromise = null;
  }

  // ── Jest reporter lifecycle ────────────────────────────────────────────────

  onRunStart(results, options) {
    this._startDrain();
    const name = options?.rootDir
      ? require('path').basename(options.rootDir) + ' tests'
      : 'Jest Test Run';
    this._enqueue(this._buildEvent(this._runEventId, 'TEST_RUN_STARTED', null, { name }));
  }

  onTestFileStart(test) {
    const suiteId = randomUUID();
    this._suiteIds.set(test.path, suiteId);
    const suiteName = require('path').relative(process.cwd(), test.path) || test.path;
    this._enqueue(this._buildEvent(suiteId, 'TEST_SUITE_STARTED', this._runEventId,
      { name: suiteName }));
  }

  onTestCaseResult(test, testCaseResult) {
    const suiteId = this._suiteIds.get(test.path) || this._runEventId;

    // TEST_CASE_STARTED
    const caseId = randomUUID();
    const caseKey = test.path + '::' + testCaseResult.fullName;
    this._caseIds.set(caseKey, caseId);
    this._enqueue(this._buildEvent(caseId, 'TEST_CASE_STARTED', suiteId,
      { name: testCaseResult.fullName }));

    const durationMs = Math.round(testCaseResult.duration || 0);
    const status = testCaseResult.status; // 'passed' | 'failed' | 'skipped' | 'pending' | 'todo'

    if (status === 'passed') {
      this._passed++;
      this._total++;
      this._enqueue(this._buildEvent(randomUUID(), 'TEST_PASSED', caseId, { duration: durationMs }));
      this._enqueue(this._buildEvent(randomUUID(), 'TEST_CASE_FINISHED', caseId,
        { nodeStatus: 'PASSED', duration: durationMs }));

    } else if (status === 'failed') {
      this._failed++;
      this._total++;
      const failMsg = testCaseResult.failureMessages?.join('\n') || '';
      const lines   = failMsg.split('\n');
      const payload = { duration: durationMs };
      if (failMsg) {
        // Last non-empty line is typically the assertion
        const lastLine = lines.filter(l => l.trim()).pop() || failMsg;
        payload.errorMessage = lastLine;
        payload.stackTrace   = failMsg;
      }
      this._enqueue(this._buildEvent(randomUUID(), 'TEST_FAILED', caseId, payload));
      this._enqueue(this._buildEvent(randomUUID(), 'TEST_CASE_FINISHED', caseId,
        { nodeStatus: 'FAILED', duration: durationMs }));

    } else {
      // skipped / pending / todo
      this._skipped++;
      this._total++;
      this._enqueue(this._buildEvent(randomUUID(), 'TEST_SKIPPED', caseId,
        { errorMessage: status }));
      this._enqueue(this._buildEvent(randomUUID(), 'TEST_CASE_FINISHED', caseId,
        { nodeStatus: 'SKIPPED', duration: durationMs }));
    }
  }

  onTestFileResult(test, testResult) {
    const suiteId = this._suiteIds.get(test.path);
    if (!suiteId) return;
    const hasFailed = testResult.testResults.some(r => r.status === 'failed');
    this._enqueue(this._buildEvent(randomUUID(), 'TEST_SUITE_FINISHED', suiteId,
      { nodeStatus: hasFailed ? 'FAILED' : 'PASSED' }));
    this._suiteIds.delete(test.path);
  }

  async onRunComplete(contexts, results) {
    const status = this._failed > 0 ? 'FAILED' : 'PASSED';
    this._enqueue(this._buildEvent(randomUUID(), 'TEST_RUN_FINISHED', this._runEventId, {
      runStatus:    status,
      totalTests:   this._total,
      passedTests:  this._passed,
      failedTests:  this._failed,
      skippedTests: this._skipped,
    }));
    await this._flush();
  }

  // ── Event building ─────────────────────────────────────────────────────────

  _buildEvent(eventId, eventType, parentId, payload) {
    return JSON.stringify({
      eventId,
      runId:     this._runId,
      eventType,
      parentId:  parentId || null,
      timestamp: new Date().toISOString(),
      payload:   JSON.stringify(payload),
    });
  }

  // ── Async queue ────────────────────────────────────────────────────────────

  _enqueue(jsonStr) {
    if (this._eventQueue.length >= 10_000) {
      process.stderr.write('[UTEM] Event queue full — dropping event.\n');
      return;
    }
    this._eventQueue.push(jsonStr);
  }

  _startDrain() {
    this._drainTimer = setInterval(() => this._drainBatch(), DRAIN_MS);
    // Allow the process to exit even if this timer is active
    if (this._drainTimer.unref) this._drainTimer.unref();
  }

  _drainBatch() {
    if (this._eventQueue.length === 0) return;
    const batch = this._eventQueue.splice(0, BATCH_SIZE);
    const body  = '[' + batch.join(',') + ']';
    this._httpPost(this._baseUrl + '/events/batch', body).catch(() => {
      // Re-queue on failure (best effort)
      this._eventQueue.unshift(...batch);
    });
  }

  async _flush() {
    if (this._drainTimer) {
      clearInterval(this._drainTimer);
      this._drainTimer = null;
    }
    const deadline = Date.now() + FLUSH_TIMEOUT;
    while (this._eventQueue.length > 0 && Date.now() < deadline) {
      await this._drainBatchAsync();
    }
  }

  async _drainBatchAsync() {
    if (this._eventQueue.length === 0) return;
    const batch = this._eventQueue.splice(0, BATCH_SIZE);
    const body  = '[' + batch.join(',') + ']';
    const ok = await this._httpPost(this._baseUrl + '/events/batch', body);
    if (!ok) {
      this._eventQueue.unshift(...batch);
    }
  }

  // ── HTTP ───────────────────────────────────────────────────────────────────

  /** POST body (string) to url. Returns true on success (2xx or 409). */
  _httpPost(url, body) {
    return new Promise((resolve) => {
      const attempt = (retryIndex) => {
        const parsed = new URL(url);
        const lib    = parsed.protocol === 'https:' ? https : http;
        const bodyBuf = Buffer.from(body, 'utf8');
        const options = {
          hostname: parsed.hostname,
          port:     parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
          path:     parsed.pathname + parsed.search,
          method:   'POST',
          headers:  {
            'Content-Type':   'application/json',
            'Content-Length': bodyBuf.length,
          },
          timeout: 10_000,
        };
        const req = lib.request(options, (res) => {
          res.resume(); // consume response body
          if (res.statusCode === 409 || res.statusCode < 400) {
            resolve(true);
          } else if (retryIndex < MAX_RETRIES) {
            setTimeout(() => attempt(retryIndex + 1), RETRY_DELAYS[retryIndex]);
          } else {
            process.stderr.write(`[UTEM] Gave up after ${MAX_RETRIES + 1} attempts (HTTP ${res.statusCode}).\n`);
            resolve(false);
          }
        });
        req.on('error', (err) => {
          process.stderr.write(`[UTEM] Send failed (attempt ${retryIndex + 1}): ${err.message}\n`);
          if (retryIndex < MAX_RETRIES) {
            setTimeout(() => attempt(retryIndex + 1), RETRY_DELAYS[retryIndex]);
          } else {
            resolve(false);
          }
        });
        req.on('timeout', () => {
          req.destroy();
        });
        req.write(bodyBuf);
        req.end();
      };
      attempt(0);
    });
  }
}

module.exports = UtemReporter;
