package com.utem.utem_core.service;

import com.utem.utem_core.dto.DiagnosisDTO;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.exception.TestNodeNotFoundException;
import com.utem.utem_core.repository.TestStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based failure analysis — no external AI API required.
 * Matches exception class names and error message patterns against a curated
 * knowledge base to produce plain-English diagnoses and suggested fixes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FailureDiagnosisService {

    private final TestStepRepository testStepRepository;

    // Extracts the fully-qualified class name from the first line of a stack trace
    private static final Pattern EXCEPTION_CLASS =
            Pattern.compile("^([A-Za-z][\\w$.]*(?:\\.[A-Za-z][\\w$.]*)*)(?::\\s*.*)?$",
                    Pattern.MULTILINE);

    // Assertion failure patterns (JUnit 4/5, TestNG, AssertJ, Hamcrest variants)
    private static final Pattern ASSERT_EXPECTED_BUT_WAS = Pattern.compile(
            "expected:?\\s*[<\\[\"']?(.{1,150}?)[>\\]\"']?\\s+but\\s+(?:was|found|got):?\\s*[<\\[\"']?(.{1,150}?)[>\\]\"']?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern ASSERT_TO_BE = Pattern.compile(
            "(?:to equal|to be)\\s+[<\\[\"']?(.{1,80}?)[>\\]\"']?",
            Pattern.CASE_INSENSITIVE);

    // ============================================================

    public DiagnosisDTO diagnoseStep(String stepId) {
        TestStep step = testStepRepository.findById(stepId)
                .orElseThrow(() -> new TestNodeNotFoundException("Step not found: " + stepId));

        String errorMessage = step.getErrorMessage();
        String stackTrace   = step.getStackTrace();
        Long   durationMs   = step.getDuration();

        Boolean flaky = null;
        if (step.getTestNode() != null) {
            flaky = step.getTestNode().getFlaky();
        }

        return diagnose(errorMessage, stackTrace, durationMs, flaky);
    }

    /** Visible for testing. */
    DiagnosisDTO diagnose(String errorMessage, String stackTrace, Long durationMs, Boolean flaky) {

        String exceptionClass = extractExceptionClass(stackTrace, errorMessage);

        Result result = matchByExceptionClass(exceptionClass, errorMessage);
        if (result == null) result = matchByMessagePattern(errorMessage);
        if (result == null) result = new Result(
                "Unknown Error",
                "An unexpected error occurred.",
                "Review the full stack trace for more details.",
                "LOW");

        // Contextual notes appended to suggestion
        List<String> notes = new ArrayList<>();
        if (Boolean.TRUE.equals(flaky)) {
            notes.add("This test has a history of intermittent failures.");
        }
        if (durationMs != null && durationMs > 30_000) {
            notes.add("Step took " + (durationMs / 1000) + "s — unusually slow.");
        }

        String suggestion = notes.isEmpty()
                ? result.suggestion()
                : result.suggestion() + " Note: " + String.join(" ", notes);

        return new DiagnosisDTO(result.category(), result.explanation(), suggestion, result.confidence());
    }

    // ============================================================
    // Exception class extraction
    // ============================================================

    private String extractExceptionClass(String stackTrace, String errorMessage) {
        String source = (stackTrace != null && !stackTrace.isBlank()) ? stackTrace : errorMessage;
        if (source == null) return null;

        String firstLine = source.lines().findFirst().orElse("").trim();
        Matcher m = EXCEPTION_CLASS.matcher(firstLine);
        if (m.find()) {
            String fqn = m.group(1);
            int dot = fqn.lastIndexOf('.');
            return dot >= 0 ? fqn.substring(dot + 1) : fqn;
        }
        return null;
    }

    // ============================================================
    // Rule table — exception class name → diagnosis
    // ============================================================

    private Result matchByExceptionClass(String cls, String errorMessage) {
        if (cls == null) return null;

        return switch (cls) {

            // --- Null / Reference ---
            case "NullPointerException" -> new Result(
                    "Null Reference",
                    "A null object was accessed when a value was expected.",
                    "Ensure the object is initialized before use. Check for missing dependency injection, unset fields, or a method returning null unexpectedly.",
                    "HIGH");

            // --- Assertions ---
            case "AssertionError", "AssertionFailedError", "ComparisonFailure",
                 "AssertionFailure", "MultipleFailuresError" -> {
                String detail = parseAssertionDetail(errorMessage);
                yield new Result(
                        "Assertion Failure",
                        detail != null ? detail : "A test assertion did not hold.",
                        "Verify the expected value matches the actual output. Check recent code changes that may have altered the returned value.",
                        "HIGH");
            }

            // --- Selenium / WebDriver ---
            case "TimeoutException" -> new Result(
                    "Timeout",
                    "The operation exceeded the allowed time limit.",
                    "The page or element may be loading slowly. Consider increasing the explicit wait or check for network / performance regressions.",
                    "HIGH");

            case "NoSuchElementException" -> new Result(
                    "Element Not Found",
                    "The expected element was not present in the DOM.",
                    "The selector may be outdated, the page may not have fully loaded, or the element is inside an iframe / shadow DOM.",
                    "HIGH");

            case "StaleElementReferenceException" -> new Result(
                    "Stale Element",
                    "The element was found but became invalid after the DOM was refreshed.",
                    "Re-locate the element after any action that triggers a DOM update (navigation, AJAX calls, dynamic content).",
                    "HIGH");

            case "ElementClickInterceptedException" -> new Result(
                    "Click Intercepted",
                    "Another element is covering the target element and received the click.",
                    "Scroll the element into view, wait for overlaying modals or banners to close, or use a JavaScript click as a fallback.",
                    "HIGH");

            case "ElementNotInteractableException" -> new Result(
                    "Element Not Interactable",
                    "The element exists in the DOM but is hidden, disabled, or off-screen.",
                    "Wait for the element to become visible and enabled before interacting with it.",
                    "HIGH");

            case "MoveTargetOutOfBoundsException" -> new Result(
                    "Move Target Out of Bounds",
                    "An action was attempted on an element that is outside the visible viewport.",
                    "Scroll the element into view before performing the action.",
                    "HIGH");

            case "InvalidSelectorException" -> new Result(
                    "Invalid Selector",
                    "The CSS or XPath selector used to locate the element is not valid.",
                    "Validate the selector syntax. Use the browser DevTools to test the selector manually.",
                    "HIGH");

            case "WebDriverException" -> new Result(
                    "WebDriver Error",
                    "The WebDriver encountered an unexpected error.",
                    "Check that the browser and driver versions are compatible. The browser may have crashed or the session may have expired.",
                    "MEDIUM");

            // --- Java standard ---
            case "IllegalArgumentException" -> new Result(
                    "Invalid Argument",
                    "A method received an argument with an invalid value.",
                    "Check input parameters for null, empty, or out-of-range values.",
                    "HIGH");

            case "IllegalStateException" -> new Result(
                    "Illegal State",
                    "An object was in an unexpected state when the operation was attempted.",
                    "Ensure the object lifecycle is correct — it may need to be initialized or reset before this operation.",
                    "MEDIUM");

            case "IndexOutOfBoundsException", "ArrayIndexOutOfBoundsException",
                 "StringIndexOutOfBoundsException" -> new Result(
                    "Index Out of Bounds",
                    "An array, list, or string was accessed at an index that does not exist.",
                    "Check that the collection is not empty before accessing it and that loop bounds are correct.",
                    "HIGH");

            case "ClassCastException" -> new Result(
                    "Type Cast Error",
                    "An object could not be cast to the expected type.",
                    "Verify the actual runtime type of the object matches what is expected. Check generics and inheritance hierarchies.",
                    "HIGH");

            case "NumberFormatException" -> new Result(
                    "Number Parse Error",
                    "A string could not be parsed as a number.",
                    "Ensure the input string contains a valid numeric value with no unexpected whitespace or non-numeric characters.",
                    "HIGH");

            case "ArithmeticException" -> new Result(
                    "Arithmetic Error",
                    "An illegal arithmetic operation occurred (e.g. division by zero).",
                    "Check for zero denominators or other invalid numeric operations before performing arithmetic.",
                    "HIGH");

            case "UnsupportedOperationException" -> new Result(
                    "Unsupported Operation",
                    "An operation was called that is not supported by this object.",
                    "Check if you are using an immutable collection or an unimplemented method stub.",
                    "MEDIUM");

            case "ConcurrentModificationException" -> new Result(
                    "Concurrent Modification",
                    "A collection was modified while it was being iterated.",
                    "Avoid modifying collections during iteration. Use an Iterator's remove() method or collect changes and apply them after the loop.",
                    "HIGH");

            case "OutOfMemoryError" -> new Result(
                    "Out of Memory",
                    "The JVM ran out of heap memory.",
                    "Investigate for memory leaks, large data structures held in scope, or unclosed streams. Increase -Xmx if the workload legitimately requires it.",
                    "HIGH");

            case "StackOverflowError" -> new Result(
                    "Stack Overflow",
                    "The call stack exceeded its limit, likely due to infinite recursion.",
                    "Check for circular method calls or missing base cases in recursive methods.",
                    "HIGH");

            // --- I/O and networking ---
            case "FileNotFoundException" -> new Result(
                    "File Not Found",
                    "A required file or resource was not found at the specified path.",
                    "Verify the file path is correct relative to the working directory and that the file exists in the test environment.",
                    "HIGH");

            case "IOException" -> new Result(
                    "I/O Error",
                    "An input/output operation failed.",
                    "Check file permissions, disk space, and that the target file or socket is accessible.",
                    "MEDIUM");

            case "ConnectException" -> new Result(
                    "Connection Refused",
                    "Could not establish a network connection to the target host.",
                    "Verify the target service is running and accessible on the expected host and port.",
                    "MEDIUM");

            case "SocketTimeoutException" -> new Result(
                    "Socket Timeout",
                    "A network socket operation timed out.",
                    "Check network connectivity and consider increasing the socket timeout if the operation is legitimately slow.",
                    "MEDIUM");

            case "UnknownHostException" -> new Result(
                    "Unknown Host",
                    "The target hostname could not be resolved.",
                    "Check the hostname for typos and ensure DNS resolution is working in the test environment.",
                    "MEDIUM");

            // --- Database ---
            case "SQLException" -> new Result(
                    "Database Error",
                    "A database operation failed.",
                    "Check the SQL syntax, database connection, and whether the schema matches the expected structure.",
                    "MEDIUM");

            // --- Classpath ---
            case "ClassNotFoundException", "NoClassDefFoundError" -> new Result(
                    "Class Not Found",
                    "A required class was not found on the classpath.",
                    "Check that all dependencies are correctly declared and available at runtime.",
                    "HIGH");

            // --- Cucumber ---
            case "MissingStepDefinitionException", "UndefinedStepException" -> new Result(
                    "Missing Step Definition",
                    "A Cucumber step has no matching step definition.",
                    "Implement the missing step definition or check for typos in the Gherkin step text.",
                    "HIGH");

            case "AmbiguousStepDefinitionsException" -> new Result(
                    "Ambiguous Step",
                    "Multiple step definitions matched the same Cucumber step.",
                    "Refine the regular expressions in your step definitions so each step is uniquely matched.",
                    "HIGH");

            default -> null;
        };
    }

    // ============================================================
    // Fallback: pattern-match on the error message text
    // ============================================================

    private Result matchByMessagePattern(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) return null;

        String detail = parseAssertionDetail(errorMessage);
        if (detail != null) {
            return new Result("Assertion Failure", detail,
                    "Verify the expected value matches the actual output.", "MEDIUM");
        }

        String lower = errorMessage.toLowerCase(java.util.Locale.ROOT);

        if (lower.contains("unable to locate element") || lower.contains("no such element")) {
            return new Result("Element Not Found",
                    "The expected element was not found in the page.",
                    "The selector may be outdated or the page may not have fully loaded.", "MEDIUM");
        }
        if (lower.contains("timed out") || lower.contains("timeout")) {
            return new Result("Timeout",
                    "An operation exceeded the allowed time limit.",
                    "Check for slow-loading pages or increase wait timeouts.", "MEDIUM");
        }
        if (lower.contains("connection refused")) {
            return new Result("Connection Refused",
                    "Could not connect to the target service.",
                    "Verify the service is running on the expected host and port.", "MEDIUM");
        }
        if (lower.contains("no route to host") || lower.contains("network unreachable")) {
            return new Result("Network Error",
                    "The target host is unreachable.",
                    "Check network configuration and ensure the target host is accessible.", "MEDIUM");
        }
        if (lower.contains("null") && (lower.contains("is null") || lower.contains("was null")
                || lower.contains("null pointer"))) {
            return new Result("Null Reference",
                    "A null value was encountered unexpectedly.",
                    "Ensure the object is properly initialized before use.", "MEDIUM");
        }
        if (lower.contains("stale element")) {
            return new Result("Stale Element",
                    "The element became invalid after the DOM was refreshed.",
                    "Re-locate the element after any action that triggers a DOM update.", "MEDIUM");
        }
        if (lower.contains("permission denied") || lower.contains("access denied")) {
            return new Result("Permission Denied",
                    "The test does not have permission to perform the requested operation.",
                    "Check file/directory permissions and user roles in the test environment.", "MEDIUM");
        }

        return null;
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String parseAssertionDetail(String msg) {
        if (msg == null) return null;
        Matcher m = ASSERT_EXPECTED_BUT_WAS.matcher(msg);
        if (m.find()) {
            return "Expected [" + m.group(1).trim() + "] but got [" + m.group(2).trim() + "].";
        }
        Matcher m2 = ASSERT_TO_BE.matcher(msg);
        if (m2.find()) {
            return "Assertion failed — value did not match [" + m2.group(1).trim() + "].";
        }
        return null;
    }

    private record Result(String category, String explanation, String suggestion, String confidence) {}
}
