import type {
  Reporter,
  FullConfig,
  Suite,
  TestCase,
  TestResult,
  FullResult,
} from '@playwright/test/reporter';
import { createReadStream, statSync } from 'fs';
import { request as httpsRequest } from 'https';
import { request as httpRequest } from 'http';
import { randomUUID } from 'crypto';

// ── Config resolution ─────────────────────────────────────────────────

function resolveConfig() {
  const url = process.env['UTEM_SERVER_URL']?.trim();
  const serverUrl = (url ? url.replace(/\/$/, '') : 'http://localhost:8080/utem');
  const runName  = process.env['UTEM_RUN_NAME']?.trim()  || 'Playwright Test Run';
  const runLabel = process.env['UTEM_RUN_LABEL']?.trim() || null;
  const jobName  = process.env['UTEM_JOB_NAME']?.trim()  || null;
  return { serverUrl, runName, runLabel, jobName };
}

// ── HTTP helpers ──────────────────────────────────────────────────────

function doPost(url: string, body: Buffer, headers: Record<string, string | number>): Promise<void> {
  return new Promise((resolve) => {
    const parsed = new URL(url);
    const req = (parsed.protocol === 'https:' ? httpsRequest : httpRequest)(
      {
        hostname: parsed.hostname,
        port: parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
        path: parsed.pathname + parsed.search,
        method: 'POST',
        headers,
      },
      (res) => { res.resume(); resolve(); }
    );
    req.on('error', (e) => {
      console.error(`[UTEM] HTTP error: ${e.message}`);
      resolve();
    });
    req.write(body);
    req.end();
  });
}

async function postJson(url: string, json: string, retries = 3): Promise<void> {
  const body = Buffer.from(json, 'utf8');
  for (let i = 1; i <= retries; i++) {
    try {
      await doPost(url, body, {
        'Content-Type': 'application/json',
        'Content-Length': body.length,
      });
      return;
    } catch (e) {
      if (i < retries) await sleep(i * 1000);
    }
  }
}

async function uploadFile(url: string, filePath: string, fileName: string): Promise<void> {
  const boundary = '----UtemBoundary' + randomUUID().replace(/-/g, '');
  try {
    const stats   = statSync(filePath);
    const header  = Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: image/png\r\n\r\n`);
    const footer  = Buffer.from(`\r\n--${boundary}--\r\n`);
    const parsed  = new URL(url);

    return new Promise((resolve) => {
      const req = (parsed.protocol === 'https:' ? httpsRequest : httpRequest)(
        {
          hostname: parsed.hostname,
          port: parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
          path: parsed.pathname,
          method: 'POST',
          headers: {
            'Content-Type': `multipart/form-data; boundary=${boundary}`,
            'Content-Length': header.length + stats.size + footer.length,
          },
        },
        (res) => { res.resume(); resolve(); }
      );
      req.on('error', (e) => { console.error(`[UTEM] Upload error: ${e.message}`); resolve(); });
      req.write(header);
      const stream = createReadStream(filePath);
      stream.on('data', (chunk) => req.write(chunk));
      stream.on('end', () => { req.write(footer); req.end(); });
      stream.on('error', () => { req.end(); resolve(); });
    });
  } catch (e) {
    console.error(`[UTEM] Upload failed for ${fileName}: ${e}`);
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((r) => setTimeout(r, ms));
}

// ── JSON event builders ───────────────────────────────────────────────

function esc(s: string | null | undefined): string {
  if (!s) return '';
  return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
          .replace(/\n/g, '\\n').replace(/\r/g, '\\r').replace(/\t/g, '\\t');
}

function buildEvent(
  eventId: string, runId: string, eventType: string,
  parentId: string | null, payload: string
): string {
  const p = parentId ? `"${parentId}"` : 'null';
  return `{"eventId":"${eventId}","runId":"${runId}","eventType":"${eventType}","parentId":${p},"timestamp":"${new Date().toISOString()}","payload":"${esc(payload)}"}`;
}

function evRunStarted(id: string, runId: string, name: string, label: string | null, job: string | null): string {
  let pl = `{"name":"${esc(name)}"`;
  if (label) pl += `,"label":"${esc(label)}"`;
  if (job)   pl += `,"jobName":"${esc(job)}"`;
  pl += '}';
  return buildEvent(id, runId, 'TEST_RUN_STARTED', null, pl);
}

function evRunFinished(id: string, runId: string, parentId: string, total: number, passed: number, failed: number, skipped: number): string {
  const status = failed > 0 ? 'FAILED' : 'PASSED';
  const pl = `{"totalTests":${total},"passedTests":${passed},"failedTests":${failed},"skippedTests":${skipped},"runStatus":"${status}"}`;
  return buildEvent(id, runId, 'TEST_RUN_FINISHED', parentId, pl);
}

function evSuiteStarted(id: string, runId: string, name: string): string {
  return buildEvent(id, runId, 'TEST_SUITE_STARTED', null, `{"name":"${esc(name)}"}`);
}

function evSuiteFinished(id: string, runId: string, parentId: string, status: string): string {
  return buildEvent(id, runId, 'TEST_SUITE_FINISHED', parentId, `{"nodeStatus":"${status}"}`);
}

function evCaseStarted(id: string, runId: string, parentId: string | undefined, name: string): string {
  return buildEvent(id, runId, 'TEST_CASE_STARTED', parentId ?? null, `{"name":"${esc(name)}"}`);
}

function evCaseFinished(id: string, runId: string, parentId: string | undefined, status: string, duration: number): string {
  return buildEvent(id, runId, 'TEST_CASE_FINISHED', parentId ?? null, `{"nodeStatus":"${status}","duration":${duration}}`);
}

function evPassed(id: string, runId: string, parentId: string | undefined, duration: number): string {
  return buildEvent(id, runId, 'TEST_PASSED', parentId ?? null, `{"duration":${duration}}`);
}

function evFailed(id: string, runId: string, parentId: string | undefined, duration: number, msg: string | null, stack: string | null): string {
  let pl = `{"duration":${duration}`;
  if (msg)   pl += `,"errorMessage":"${esc(msg)}"`;
  if (stack) pl += `,"stackTrace":"${esc(stack.slice(0, 2000))}"`;
  pl += '}';
  return buildEvent(id, runId, 'TEST_FAILED', parentId ?? null, pl);
}

function evSkipped(id: string, runId: string, parentId: string | undefined): string {
  return buildEvent(id, runId, 'TEST_SKIPPED', parentId ?? null, '{}');
}

function evAttachment(id: string, runId: string, parentId: string | undefined, name: string, mimeType: string, fileSize: number, isFailure: boolean): string {
  const pl = `{"name":"${esc(name)}","attachmentType":"SCREENSHOT","mimeType":"${mimeType}","fileSize":${fileSize},"isFailureScreenshot":${isFailure}}`;
  return buildEvent(id, runId, 'ATTACHMENT', parentId ?? null, pl);
}

// ── Reporter ──────────────────────────────────────────────────────────

interface ScreenshotUpload { id: string; path: string; name: string; }

class UtemReporter implements Reporter {
  private readonly cfg = resolveConfig();
  private readonly runId      = randomUUID();
  private readonly runEventId = randomUUID();
  private readonly events: string[] = [];
  private readonly suiteIds    = new Map<Suite, string>();
  private readonly suiteStatus = new Map<Suite, 'PASSED' | 'FAILED'>();
  private readonly caseIds     = new Map<TestResult, string>();
  private readonly screenshots: ScreenshotUpload[] = [];

  private total = 0; private passed = 0; private failed = 0; private skipped = 0;

  onBegin(_config: FullConfig, _suite: Suite): void {
    const { runName, runLabel, jobName } = this.cfg;
    this.events.push(evRunStarted(this.runEventId, this.runId, runName, runLabel, jobName));
  }

  onTestBegin(test: TestCase, result: TestResult): void {
    const parent = test.parent;

    // Lazily register parent suite
    if (!this.suiteIds.has(parent)) {
      const suiteId = randomUUID();
      this.suiteIds.set(parent, suiteId);
      this.suiteStatus.set(parent, 'PASSED');
      const suiteName = parent.title || parent.location?.file || 'Suite';
      this.events.push(evSuiteStarted(suiteId, this.runId, suiteName));
    }

    const caseId = randomUUID();
    this.caseIds.set(result, caseId);
    this.total++;
    this.events.push(evCaseStarted(caseId, this.runId, this.suiteIds.get(parent), test.title));
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    const caseId   = this.caseIds.get(result);
    const duration = result.duration;
    const parent   = test.parent;

    if (result.status === 'passed') {
      this.passed++;
      this.events.push(evPassed(randomUUID(), this.runId, caseId, duration));
      this.events.push(evCaseFinished(randomUUID(), this.runId, caseId, 'PASSED', duration));
    } else if (result.status === 'skipped') {
      this.skipped++;
      this.events.push(evSkipped(randomUUID(), this.runId, caseId));
      this.events.push(evCaseFinished(randomUUID(), this.runId, caseId, 'SKIPPED', duration));
    } else {
      // failed | timedOut | interrupted
      this.failed++;
      this.suiteStatus.set(parent, 'FAILED');
      const msg   = result.error?.message ?? null;
      const stack = result.error?.stack   ?? null;
      this.events.push(evFailed(randomUUID(), this.runId, caseId, duration, msg, stack));
      this.events.push(evCaseFinished(randomUUID(), this.runId, caseId, 'FAILED', duration));
    }

    // Collect Playwright-captured screenshots (screenshot: 'on' / 'only-on-failure')
    for (const att of result.attachments) {
      if (att.path && (att.contentType === 'image/png' || att.contentType === 'image/jpeg')) {
        try {
          const stats      = statSync(att.path);
          const attachId   = randomUUID();
          const isFailure  = result.status !== 'passed';
          this.events.push(evAttachment(attachId, this.runId, caseId, att.name, att.contentType, stats.size, isFailure));
          this.screenshots.push({ id: attachId, path: att.path, name: att.name });
        } catch (_) { /* file gone, skip */ }
      }
    }
  }

  async onEnd(_result: FullResult): Promise<void> {
    const { serverUrl } = this.cfg;

    // Close all suites
    for (const [suite, suiteId] of this.suiteIds.entries()) {
      const status = this.suiteStatus.get(suite) ?? 'PASSED';
      this.events.push(evSuiteFinished(randomUUID(), this.runId, suiteId, status));
    }

    // Close run
    this.events.push(evRunFinished(randomUUID(), this.runId, this.runEventId,
      this.total, this.passed, this.failed, this.skipped));

    // Batch-send all events
    if (this.events.length > 0) {
      await postJson(`${serverUrl}/events/batch`, `[${this.events.join(',')}]`);
    }

    // Upload screenshots
    for (const ss of this.screenshots) {
      await uploadFile(`${serverUrl}/attachments/${ss.id}/upload`, ss.path, ss.name);
    }
  }

  printsToStdio(): boolean { return false; }
}

export default UtemReporter;
