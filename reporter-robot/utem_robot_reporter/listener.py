"""
UTEM Listener for Robot Framework.

Streams test events to UTEM Core as tests run. Uses only Python standard library
— zero external dependencies.

Usage:
    robot --listener utem_robot_reporter.UtemListener tests/

With explicit server URL and API key (positional args):
    robot --listener "utem_robot_reporter.UtemListener:http://myserver:8080/utem:my-api-key" tests/

Configuration (in order of precedence):
    1. Listener constructor args  (--listener UtemListener:url:key)
    2. Environment variables      UTEM_SERVER_URL, UTEM_API_KEY
    3. utem.config.json           { "serverUrl": "...", "apiKey": "..." }
    4. Defaults                   http://localhost:8080/utem

Environment variables:
    UTEM_SERVER_URL   UTEM Core server base URL
    UTEM_API_KEY      Project API key
    UTEM_RUN_NAME     Custom run name (overrides suite name)
    UTEM_RUN_LABEL    Tag (e.g. regression, smoke)
    UTEM_JOB_NAME     CI job name
    UTEM_DISABLED     Set to 'true' to disable

Selenium screenshot support:
    Call utem_robot_reporter.register_driver(driver) in a Suite Setup or
    resource keyword after opening the browser. The listener will automatically
    capture a screenshot on each failed test.
"""

from __future__ import annotations

import json
import os
import queue
import sys
import threading
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Optional

# ── Listener API version ─────────────────────────────────────────────────────

ROBOT_LISTENER_API_VERSION = 3

# ── Selenium driver registry (ThreadLocal) ───────────────────────────────────

_driver_local = threading.local()


def register_driver(driver) -> None:
    """Register a Selenium WebDriver for the current thread (enables failure screenshots)."""
    _driver_local.driver = driver


def unregister_driver() -> None:
    """Unregister the WebDriver for the current thread."""
    _driver_local.driver = None


# ── Config resolution ────────────────────────────────────────────────────────

def _load_file_config() -> dict:
    path = Path.cwd() / "utem.config.json"
    if path.exists():
        try:
            with open(path, encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return {}


def _resolve(env_key: str, file_key: str, file_cfg: dict, default: str = "") -> str:
    v = os.environ.get(env_key, "").strip()
    if v:
        return v
    v = str(file_cfg.get(file_key, "")).strip()
    if v:
        return v
    return default


# ── Elapsed time helper (RF 4 vs RF 5 compat) ────────────────────────────────

def _elapsed_ms(result) -> int:
    """Return test/suite elapsed time in milliseconds, compatible with RF 4 and 5."""
    # RF 5+: result.elapsed_time is a datetime.timedelta
    if hasattr(result, "elapsed_time"):
        et = result.elapsed_time
        try:
            return int(et.total_seconds() * 1000)
        except AttributeError:
            pass
    # RF 4: result.elapsedtime is already milliseconds (int)
    if hasattr(result, "elapsedtime"):
        return int(result.elapsedtime or 0)
    return 0


# ── Core listener ─────────────────────────────────────────────────────────────

class UtemListener:
    """Robot Framework listener v3 that reports events to UTEM Core."""

    ROBOT_LISTENER_API_VERSION = 3

    _BATCH_SIZE     = 50
    _DRAIN_INTERVAL = 0.2   # seconds
    _FLUSH_TIMEOUT  = 30.0  # seconds
    _MAX_RETRIES    = 3
    _RETRY_DELAYS   = [0.1, 0.5, 2.0]

    def __init__(self, server_url: str = "", api_key: str = ""):
        file_cfg = _load_file_config()

        disabled_env  = os.environ.get("UTEM_DISABLED", "").strip().lower() == "true"
        disabled_file = file_cfg.get("disabled", False) is True
        self._disabled = disabled_env or disabled_file

        self._base_url = (
            server_url.strip().rstrip("/")
            or _resolve("UTEM_SERVER_URL", "serverUrl", file_cfg,
                        "http://localhost:8080/utem")
        )
        self._api_key = (
            api_key.strip()
            or _resolve("UTEM_API_KEY", "apiKey", file_cfg, "")
        )
        self._run_name  = _resolve("UTEM_RUN_NAME",  "runName",  file_cfg, "")
        self._run_label = _resolve("UTEM_RUN_LABEL", "runLabel", file_cfg, "")
        self._job_name  = _resolve("UTEM_JOB_NAME",  "jobName",  file_cfg, "")

        self._run_id       = str(uuid.uuid4())
        self._run_event_id = str(uuid.uuid4())

        # Suite tracking: suite.longname → suiteEventId
        # Top-level suite is the "run" itself; nested suites are TEST_SUITE events
        self._suite_event_ids: Dict[str, str] = {}
        self._suite_fail_counts: Dict[str, int] = {}
        self._suite_lock = threading.Lock()

        # Test tracking: test.longname → caseEventId
        self._test_event_ids: Dict[str, str] = {}
        self._test_lock = threading.Lock()

        # Run totals
        self._total   = 0
        self._passed  = 0
        self._failed  = 0
        self._skipped = 0
        self._count_lock = threading.Lock()

        # Async queue
        self._queue: queue.Queue = queue.Queue(maxsize=10_000)
        self._running = True
        self._drain_thread = threading.Thread(
            target=self._drain_loop, name="utem-drain", daemon=True
        )
        self._drain_thread.start()

        self._root_suite_name: Optional[str] = None  # set on first start_suite

    # ── Robot Framework listener hooks ───────────────────────────────────────

    def start_suite(self, data, result) -> None:
        if self._disabled:
            return

        is_root = result.parent is None

        if is_root:
            # Root suite = the entire test run
            self._root_suite_name = result.longname
            run_name = self._run_name or result.name or "Robot Framework Test Run"
            payload: dict = {"name": run_name}
            if self._run_label:
                payload["label"] = self._run_label
            if self._job_name:
                payload["jobName"] = self._job_name
            self._enqueue(self._event(self._run_event_id, "TEST_RUN_STARTED", None, payload))
            # Also treat the root suite itself as a TEST_SUITE_STARTED
            suite_id = str(uuid.uuid4())
            with self._suite_lock:
                self._suite_event_ids[result.longname]  = suite_id
                self._suite_fail_counts[result.longname] = 0
            self._enqueue(self._event(suite_id, "TEST_SUITE_STARTED", self._run_event_id,
                                      {"name": result.name}))
        else:
            # Nested suite (sub-directory or resource file)
            parent_longname = _parent_longname(result.longname)
            with self._suite_lock:
                parent_id = self._suite_event_ids.get(parent_longname, self._run_event_id)
            suite_id = str(uuid.uuid4())
            with self._suite_lock:
                self._suite_event_ids[result.longname]  = suite_id
                self._suite_fail_counts[result.longname] = 0
            self._enqueue(self._event(suite_id, "TEST_SUITE_STARTED", parent_id,
                                      {"name": result.name}))

    def end_suite(self, data, result) -> None:
        if self._disabled:
            return

        is_root = result.parent is None

        with self._suite_lock:
            suite_id  = self._suite_event_ids.pop(result.longname, None)
            fail_count = self._suite_fail_counts.pop(result.longname, 0)

        if suite_id:
            node_status = "FAILED" if fail_count > 0 else "PASSED"
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_SUITE_FINISHED", suite_id,
                                      {"nodeStatus": node_status}))

        if is_root:
            with self._count_lock:
                total, passed, failed, skipped = (
                    self._total, self._passed, self._failed, self._skipped
                )
            run_status = "FAILED" if failed > 0 else "PASSED"
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_RUN_FINISHED", self._run_event_id, {
                "runStatus":    run_status,
                "totalTests":   total,
                "passedTests":  passed,
                "failedTests":  failed,
                "skippedTests": skipped,
            }))
            self._flush()

    def start_test(self, data, result) -> None:
        if self._disabled:
            return

        # Find parent suite event ID
        parent_longname = _parent_longname(result.longname)
        with self._suite_lock:
            parent_id = self._suite_event_ids.get(parent_longname, self._run_event_id)

        case_id = str(uuid.uuid4())
        with self._test_lock:
            self._test_event_ids[result.longname] = case_id

        self._enqueue(self._event(case_id, "TEST_CASE_STARTED", parent_id,
                                  {"name": result.name}))

    def end_test(self, data, result) -> None:
        if self._disabled:
            return

        with self._test_lock:
            case_id = self._test_event_ids.pop(result.longname, None)
        if case_id is None:
            return

        duration_ms = _elapsed_ms(result)
        status = result.status  # 'PASS', 'FAIL', 'SKIP', 'NOT RUN'

        # Update parent suite fail count
        parent_longname = _parent_longname(result.longname)
        if status == "FAIL":
            with self._suite_lock:
                if parent_longname in self._suite_fail_counts:
                    self._suite_fail_counts[parent_longname] += 1

        if status == "PASS":
            with self._count_lock:
                self._passed += 1
                self._total  += 1
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_PASSED", case_id,
                                      {"duration": duration_ms}))
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_CASE_FINISHED", case_id,
                                      {"nodeStatus": "PASSED", "duration": duration_ms}))

        elif status == "FAIL":
            with self._count_lock:
                self._failed += 1
                self._total  += 1
            payload: dict = {"duration": duration_ms}
            if result.message:
                payload["errorMessage"] = result.message
                payload["stackTrace"]   = result.message
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_FAILED", case_id, payload))
            self._capture_screenshot(case_id)
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_CASE_FINISHED", case_id,
                                      {"nodeStatus": "FAILED", "duration": duration_ms}))

        else:
            # SKIP or NOT RUN
            with self._count_lock:
                self._skipped += 1
                self._total   += 1
            reason = result.message or status
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_SKIPPED", case_id,
                                      {"errorMessage": reason}))
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_CASE_FINISHED", case_id,
                                      {"nodeStatus": "SKIPPED", "duration": duration_ms}))

    # ── Event building ────────────────────────────────────────────────────────

    def _event(self, event_id: str, event_type: str,
               parent_id: Optional[str], payload: dict) -> str:
        ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
        obj = {
            "eventId":   event_id,
            "runId":     self._run_id,
            "eventType": event_type,
            "parentId":  parent_id,
            "timestamp": ts,
            "payload":   json.dumps(payload, ensure_ascii=False),
        }
        return json.dumps(obj, ensure_ascii=False)

    # ── Screenshot ────────────────────────────────────────────────────────────

    def _capture_screenshot(self, parent_event_id: str) -> None:
        driver = getattr(_driver_local, "driver", None)
        if driver is None:
            return
        try:
            import os as _os
            import tempfile
            png_bytes = driver.get_screenshot_as_png()
            with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as f:
                f.write(png_bytes)
                tmp_path = f.name
            try:
                file_size = _os.path.getsize(tmp_path)
                attach_payload = json.dumps({
                    "name": "failure-screenshot.png",
                    "attachmentType": "SCREENSHOT",
                    "mimeType": "image/png",
                    "fileSize": file_size,
                    "isFailureScreenshot": True,
                })
                attach_id = str(uuid.uuid4())
                ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
                attach_event = json.dumps({
                    "eventId":   attach_id,
                    "runId":     self._run_id,
                    "eventType": "ATTACHMENT",
                    "parentId":  parent_event_id,
                    "timestamp": ts,
                    "payload":   attach_payload,
                })
                self._http_post(self._base_url + "/events", attach_event.encode())
                self._upload_file(attach_id, tmp_path, "failure-screenshot.png")
            finally:
                _os.unlink(tmp_path)
        except Exception as exc:
            print(f"[UTEM] Screenshot capture failed: {exc}", file=sys.stderr)

    def _upload_file(self, attachment_id: str, file_path: str, filename: str) -> None:
        try:
            boundary = "----UtemBoundary" + uuid.uuid4().hex
            with open(file_path, "rb") as f:
                file_bytes = f.read()
            prefix = (
                f"--{boundary}\r\n"
                f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
                f"Content-Type: application/octet-stream\r\n\r\n"
            ).encode()
            suffix = f"\r\n--{boundary}--\r\n".encode()
            body   = prefix + file_bytes + suffix
            url    = f"{self._base_url}/attachments/{attachment_id}/upload"
            headers = {"Content-Type": f"multipart/form-data; boundary={boundary}"}
            if self._api_key:
                headers["X-API-Key"] = self._api_key
            req = urllib.request.Request(url, data=body, headers=headers, method="POST")
            with urllib.request.urlopen(req, timeout=30):
                pass
        except Exception as exc:
            print(f"[UTEM] File upload failed: {exc}", file=sys.stderr)

    # ── Async queue ───────────────────────────────────────────────────────────

    def _enqueue(self, json_str: str) -> None:
        try:
            self._queue.put_nowait(json_str)
        except queue.Full:
            print("[UTEM] Event queue full — dropping event.", file=sys.stderr)

    def _drain_loop(self) -> None:
        while self._running:
            threading.Event().wait(self._DRAIN_INTERVAL)
            self._drain_batch()
        self._drain_remaining()

    def _drain_batch(self) -> None:
        batch = []
        try:
            while len(batch) < self._BATCH_SIZE:
                batch.append(self._queue.get_nowait())
        except queue.Empty:
            pass
        if not batch:
            return
        body = ("[" + ",".join(batch) + "]").encode()
        ok   = self._http_post(self._base_url + "/events/batch", body)
        if not ok:
            for item in batch:
                try:
                    self._queue.put_nowait(item)
                except queue.Full:
                    break

    def _drain_remaining(self) -> None:
        while not self._queue.empty():
            self._drain_batch()

    def _flush(self) -> None:
        self._running = False
        self._drain_thread.join(timeout=self._FLUSH_TIMEOUT)

    def _http_post(self, url: str, body: bytes) -> bool:
        headers: dict = {"Content-Type": "application/json"}
        if self._api_key:
            headers["X-API-Key"] = self._api_key
        for attempt in range(self._MAX_RETRIES + 1):
            try:
                req = urllib.request.Request(url, data=body, headers=headers, method="POST")
                with urllib.request.urlopen(req, timeout=10) as resp:
                    if resp.status == 409:
                        return True
                    return resp.status < 400
            except urllib.error.HTTPError as e:
                if e.code == 409:
                    return True
                print(f"[UTEM] HTTP error (attempt {attempt + 1}): {e.code}", file=sys.stderr)
            except Exception as exc:
                print(f"[UTEM] Send failed (attempt {attempt + 1}): {exc}", file=sys.stderr)
            if attempt < self._MAX_RETRIES:
                threading.Event().wait(self._RETRY_DELAYS[attempt])
        print("[UTEM] Gave up after max retries.", file=sys.stderr)
        return False


# ── Helpers ───────────────────────────────────────────────────────────────────

def _parent_longname(longname: str) -> str:
    """Return the parent's longname (everything before the last '.')."""
    idx = longname.rfind(".")
    return longname[:idx] if idx >= 0 else longname
