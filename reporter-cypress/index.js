'use strict';

/**
 * UTEM Reporter for Cypress.
 *
 * Streams test events to UTEM Core as specs run. Uses only Node.js built-ins
 * (http/https, crypto) — zero external dependencies.
 *
 * Usage in cypress.config.js:
 *   const { defineConfig } = require('cypress');
 *   const { registerUtemPlugin } = require('utem-cypress-reporter');
 *
 *   module.exports = defineConfig({
 *     e2e: {
 *       setupNodeEvents(on, config) {
 *         registerUtemPlugin(on, config, {
 *           serverUrl: 'http://localhost:8080/utem',
 *           apiKey: process.env.UTEM_API_KEY,
 *         });
 *       }
 *     }
 *   });
 *
 * Configuration (in order of precedence):
 *   1. options.serverUrl  (in cypress.config.js)
 *   2. UTEM_SERVER_URL    (environment variable)
 *   3. http://localhost:8080/utem  (default)
 */

const http   = require('http');
const https  = require('https');
const { randomUUID } = require('crypto');

const MAX_RETRIES   = 3;
const RETRY_DELAYS  = [100, 500, 2000];
const REQUEST_TIMEOUT_MS = 10_000;

class UtemCypressPlugin {

  constructor(options = {}) {
    const rawUrl = options.serverUrl
      || process.env.UTEM_SERVER_URL
      || 'http://localhost:8080/utem';
    this._baseUrl = rawUrl.replace(/\/+$/, '');
    this._apiKey  = options.apiKey || process.env.UTEM_API_KEY || null;

    this._runId      = randomUUID();
    this._runEventId = randomUUID();

    /** spec.relative → suiteEventId */
    this._suiteIds = new Map();

    this._total   = 0;
    this._passed  = 0;
    this._failed  = 0;
    this._skipped = 0;
  }

  // ── Public API ─────────────────────────────────────────────────────────────

  register(on, config) {
    on('before:run',  (details) => this._onBeforeRun(details, config));
    on('before:spec', (spec)    => this._onBeforeSpec(spec));
    on('after:spec',  (spec, results) => this._onAfterSpec(spec, results));
    on('after:run',   (results) => this._onAfterRun(results));
  }

  // ── Lifecycle handlers ─────────────────────────────────────────────────────

  async _onBeforeRun(details, config) {
    const name = config?.projectName
      || details?.config?.projectName
      || process.env.UTEM_RUN_NAME
      || 'Cypress Test Run';

    await this._postBatch([
      this._buildEvent(this._runEventId, 'TEST_RUN_STARTED', null, { name }),
    ]);
  }

  async _onBeforeSpec(spec) {
    const suiteId = randomUUID();
    this._suiteIds.set(spec.relative, suiteId);
    await this._postBatch([
      this._buildEvent(suiteId, 'TEST_SUITE_STARTED', this._runEventId, {
        name: spec.relative,
      }),
    ]);
  }

  async _onAfterSpec(spec, results) {
    const suiteId = this._suiteIds.get(spec.relative) || this._runEventId;
    const events  = [];

    if (results && results.tests) {
      let suiteFailed = false;

      for (const test of results.tests) {
        const caseId    = randomUUID();
        const testName  = Array.isArray(test.title) ? test.title.join(' > ') : String(test.title);
        const attempt   = test.attempts?.[test.attempts.length - 1] || {};
        const durationMs = Math.round(attempt.wallClockDuration || attempt.duration || 0);
        const state     = test.state; // 'passed' | 'failed' | 'pending'

        this._total++;
        events.push(this._buildEvent(caseId, 'TEST_CASE_STARTED', suiteId, { name: testName }));

        if (state === 'passed') {
          this._passed++;
          events.push(this._buildEvent(randomUUID(), 'TEST_PASSED', caseId, { duration: durationMs }));
          events.push(this._buildEvent(randomUUID(), 'TEST_CASE_FINISHED', caseId,
            { nodeStatus: 'PASSED', duration: durationMs }));

        } else if (state === 'failed') {
          this._failed++;
          suiteFailed = true;
          const errorMessage = test.displayError || attempt.error?.message || '';
          const stackTrace   = attempt.error?.stack  || errorMessage;
          events.push(this._buildEvent(randomUUID(), 'TEST_FAILED', caseId, {
            duration: durationMs,
            errorMessage,
            stackTrace,
          }));
          events.push(this._buildEvent(randomUUID(), 'TEST_CASE_FINISHED', caseId,
            { nodeStatus: 'FAILED', duration: durationMs }));

        } else {
          // pending / skipped
          this._skipped++;
          events.push(this._buildEvent(randomUUID(), 'TEST_SKIPPED', caseId,
            { errorMessage: state }));
          events.push(this._buildEvent(randomUUID(), 'TEST_CASE_FINISHED', caseId,
            { nodeStatus: 'SKIPPED', duration: durationMs }));
        }
      }

      events.push(this._buildEvent(randomUUID(), 'TEST_SUITE_FINISHED', suiteId,
        { nodeStatus: suiteFailed ? 'FAILED' : 'PASSED' }));
    } else {
      events.push(this._buildEvent(randomUUID(), 'TEST_SUITE_FINISHED', suiteId,
        { nodeStatus: 'PASSED' }));
    }

    await this._postBatch(events);
    this._suiteIds.delete(spec.relative);
  }

  async _onAfterRun() {
    await this._postBatch([
      this._buildEvent(randomUUID(), 'TEST_RUN_FINISHED', this._runEventId, {
        runStatus:    this._failed > 0 ? 'FAILED' : 'PASSED',
        totalTests:   this._total,
        passedTests:  this._passed,
        failedTests:  this._failed,
        skippedTests: this._skipped,
      }),
    ]);
  }

  // ── Event builder ──────────────────────────────────────────────────────────

  _buildEvent(eventId, eventType, parentId, payload) {
    return {
      eventId,
      runId:     this._runId,
      eventType,
      parentId:  parentId || null,
      timestamp: new Date().toISOString(),
      payload:   JSON.stringify(payload),
    };
  }

  // ── HTTP ───────────────────────────────────────────────────────────────────

  _postBatch(events) {
    const body = JSON.stringify(events);
    return this._httpPost(this._baseUrl + '/events/batch', body);
  }

  _httpPost(url, body) {
    return new Promise((resolve) => {
      const attempt = (retryIndex) => {
        const parsed  = new URL(url);
        const lib     = parsed.protocol === 'https:' ? https : http;
        const bodyBuf = Buffer.from(body, 'utf8');
        const headers = {
          'Content-Type':   'application/json',
          'Content-Length': bodyBuf.length,
        };
        if (this._apiKey) headers['X-API-Key'] = this._apiKey;

        const req = lib.request({
          hostname: parsed.hostname,
          port:     parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
          path:     parsed.pathname + parsed.search,
          method:   'POST',
          headers,
          timeout:  REQUEST_TIMEOUT_MS,
        }, (res) => {
          res.resume();
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

        req.on('timeout', () => req.destroy());
        req.write(bodyBuf);
        req.end();
      };
      attempt(0);
    });
  }
}

/**
 * Register the UTEM plugin with Cypress.
 *
 * @param {Function} on     - Cypress `on` from setupNodeEvents
 * @param {Object}   config - Cypress config object
 * @param {Object}   options
 * @param {string}   [options.serverUrl] - UTEM server base URL
 * @param {string}   [options.apiKey]    - UTEM project API key
 */
function registerUtemPlugin(on, config, options = {}) {
  const plugin = new UtemCypressPlugin(options);
  plugin.register(on, config);
}

module.exports = { registerUtemPlugin, UtemCypressPlugin };
