"""
UTEM Reporter - pytest plugin.

Streams test events to UTEM Core as tests run. Auto-discovered when the package
is installed; no extra configuration in conftest.py or pytest.ini needed.

Configuration:
    UTEM_SERVER_URL  environment variable   (e.g. http://localhost:8080/utem)
    --utem-url       pytest command-line option (overrides env var)

Selenium screenshot support:
    Call utem_reporter.register_driver(driver) in your fixture setUp,
    and utem_reporter.unregister_driver() in tearDown.
"""

from __future__ import annotations

import json
import os
import queue
import re
import sys
import threading
import urllib.request
import urllib.error
import uuid
from typing import Dict, Optional

import pytest

# ── Public Selenium integration helpers ─────────────────────────────────────

_driver_local = threading.local()


def register_driver(driver) -> None:
    """Register a Selenium WebDriver for the current thread (enables screenshot on failure)."""
    _driver_local.driver = driver


def unregister_driver() -> None:
    """Unregister the WebDriver for the current thread."""
    _driver_local.driver = None


# ── Plugin registration hook ─────────────────────────────────────────────────

def pytest_addoption(parser):
    group = parser.getgroup("utem", "UTEM test reporter")
    group.addoption(
        "--utem-url",
        default=None,
        help="UTEM Core server URL (default: UTEM_SERVER_URL env var or http://localhost:8080/utem)",
    )


def pytest_configure(config):
    server_url = config.getoption("--utem-url", default=None)
    plugin = UtemPlugin(server_url=server_url)
    config.pluginmanager.register(plugin, "utem_reporter")


# ── Core plugin ──────────────────────────────────────────────────────────────

class UtemPlugin:
    """pytest plugin that reports test events to UTEM Core."""

    _BATCH_SIZE = 50
    _DRAIN_INTERVAL = 0.2   # seconds
    _FLUSH_TIMEOUT  = 30.0  # seconds
    _MAX_RETRIES    = 3
    _RETRY_DELAYS   = [0.1, 0.5, 2.0]

    def __init__(self, server_url: Optional[str] = None):
        # URL resolution: argument → env var → default
        url = (server_url
               or os.environ.get("UTEM_SERVER_URL")
               or "http://localhost:8080/utem")
        self._base_url = url.rstrip("/")

        self._run_id: str = str(uuid.uuid4())
        self._run_event_id: str = str(uuid.uuid4())

        # Suite tracking: file path → eventId
        self._file_to_event_id: Dict[str, str] = {}
        self._file_lock = threading.Lock()

        # Test case tracking: nodeid → eventId
        self._item_to_event_id: Dict[str, str] = {}
        self._item_lock = threading.Lock()

        # Run counters
        self._total   = 0
        self._passed  = 0
        self._failed  = 0
        self._skipped = 0
        self._count_lock = threading.Lock()

        # Async event queue
        self._queue: queue.Queue = queue.Queue(maxsize=10_000)
        self._running = True
        self._drain_thread = threading.Thread(
            target=self._drain_loop, name="utem-drain", daemon=True
        )
        self._drain_thread.start()

    # ── pytest hooks ─────────────────────────────────────────────────────────

    def pytest_sessionstart(self, session):
        name = getattr(session, "name", None) or "Pytest Test Run"
        self._enqueue(self._event(
            self._run_event_id, "TEST_RUN_STARTED", None,
            {"name": name},
        ))

    def pytest_runtest_logstart(self, nodeid: str, location):
        """Called at the start of each test. location = (fspath, lineno, domain)."""
        filename = str(location[0]) if location else nodeid

        # Ensure a suite event exists for this file
        with self._file_lock:
            if filename not in self._file_to_event_id:
                suite_id = str(uuid.uuid4())
                self._file_to_event_id[filename] = suite_id
                self._enqueue(self._event(
                    suite_id, "TEST_SUITE_STARTED", self._run_event_id,
                    {"name": filename},
                ))
            parent_id = self._file_to_event_id[filename]

        # Case started
        case_id = str(uuid.uuid4())
        with self._item_lock:
            self._item_to_event_id[nodeid] = case_id

        test_name = str(location[2]) if location and len(location) > 2 else nodeid
        self._enqueue(self._event(
            case_id, "TEST_CASE_STARTED", parent_id,
            {"name": test_name},
        ))

    def pytest_runtest_logreport(self, report):
        """Called after setup, call, and teardown phases."""
        # Handle 'call' phase (actual test body), or 'setup' if it was skipped
        if report.when not in ("call",) and not (report.when == "setup" and report.skipped):
            return

        nodeid = report.nodeid
        with self._item_lock:
            case_id = self._item_to_event_id.pop(nodeid, None)
        if case_id is None:
            return

        duration_ms = int((report.duration or 0) * 1000)

        if report.passed:
            with self._count_lock:
                self._passed += 1
                self._total += 1
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_PASSED", case_id,
                                      {"duration": duration_ms}))
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_CASE_FINISHED", case_id,
                                      {"nodeStatus": "PASSED", "duration": duration_ms}))

        elif report.failed:
            with self._count_lock:
                self._failed += 1
                self._total += 1
            payload: dict = {"duration": duration_ms}
            if report.longrepr:
                full = str(report.longrepr).strip()
                lines = full.splitlines()
                # Last line is usually the assertion message
                payload["errorMessage"] = lines[-1] if lines else full
                payload["stackTrace"] = full
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_FAILED", case_id, payload))
            self._capture_screenshot(case_id)
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_CASE_FINISHED", case_id,
                                      {"nodeStatus": "FAILED", "duration": duration_ms}))

        elif report.skipped:
            with self._count_lock:
                self._skipped += 1
                self._total += 1
            reason = ""
            if isinstance(report.longrepr, tuple) and len(report.longrepr) >= 3:
                reason = str(report.longrepr[2])
            elif report.longrepr:
                reason = str(report.longrepr)
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_SKIPPED", case_id,
                                      {"errorMessage": reason}))
            self._enqueue(self._event(str(uuid.uuid4()), "TEST_CASE_FINISHED", case_id,
                                      {"nodeStatus": "SKIPPED", "duration": duration_ms}))

    def pytest_sessionfinish(self, session, exitstatus):
        # Close all open suites
        with self._file_lock:
            for filename, suite_id in self._file_to_event_id.items():
                self._enqueue(self._event(str(uuid.uuid4()), "TEST_SUITE_FINISHED", suite_id,
                                          {"nodeStatus": "PASSED"}))
            self._file_to_event_id.clear()

        with self._count_lock:
            total, passed, failed, skipped = self._total, self._passed, self._failed, self._skipped
        status = "FAILED" if failed > 0 else "PASSED"
        self._enqueue(self._event(str(uuid.uuid4()), "TEST_RUN_FINISHED", self._run_event_id, {
            "runStatus": status,
            "totalTests": total,
            "passedTests": passed,
            "failedTests": failed,
            "skippedTests": skipped,
        }))
        self._flush()

    # ── Event building ────────────────────────────────────────────────────────

    def _event(self, event_id: str, event_type: str, parent_id: Optional[str], payload: dict) -> str:
        from datetime import datetime, timezone
        ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
        obj = {
            "eventId": event_id,
            "runId": self._run_id,
            "eventType": event_type,
            "parentId": parent_id,
            "timestamp": ts,
            "payload": json.dumps(payload, ensure_ascii=False),
        }
        return json.dumps(obj, ensure_ascii=False)

    # ── Screenshot ────────────────────────────────────────────────────────────

    def _capture_screenshot(self, parent_event_id: str) -> None:
        driver = getattr(_driver_local, "driver", None)
        if driver is None:
            return
        try:
            import tempfile
            import os as _os
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
                from datetime import datetime, timezone
                ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
                attach_event = json.dumps({
                    "eventId": attach_id,
                    "runId": self._run_id,
                    "eventType": "ATTACHMENT",
                    "parentId": parent_event_id,
                    "timestamp": ts,
                    "payload": attach_payload,
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
            body = prefix + file_bytes + suffix
            url = f"{self._base_url}/attachments/{attachment_id}/upload"
            req = urllib.request.Request(
                url,
                data=body,
                headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
                method="POST",
            )
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
        ok = self._http_post(self._base_url + "/events/batch", body)
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
        for attempt in range(self._MAX_RETRIES + 1):
            try:
                req = urllib.request.Request(
                    url,
                    data=body,
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                with urllib.request.urlopen(req, timeout=10) as resp:
                    if resp.status == 409:
                        return True  # duplicate — idempotent
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
