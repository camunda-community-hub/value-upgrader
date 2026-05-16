package io.camunda.kiwi.upgrader.model;

/**
 * Describes the outcome of applying a single rule.
 * Equivalent to Go's ApplyResult struct in operations.go.
 */

public class ApplyResult {

    private final boolean applied;
    private final boolean skipped;
    private final boolean notFound;
    private final String error;
    private final String detail;

    private ApplyResult(boolean applied, boolean skipped, boolean notFound, String error, String detail) {
        this.applied = applied;
        this.skipped = skipped;
        this.notFound = notFound;
        this.error = error;
        this.detail = detail;
    }

    public static ApplyResult applied(String detail) {
        return new ApplyResult(true, false, false, null, detail);
    }

    public static ApplyResult skipped(String detail) {
        return new ApplyResult(false, true, false, null, detail);
    }

    public static ApplyResult notFound(String detail) {
        return new ApplyResult(false, false, true, null, detail);
    }

    public static ApplyResult error(String message) {
        return new ApplyResult(false, false, false, message, message);
    }

    public boolean hasError() {
        return error != null;
    }


    public boolean isApplied() {
        return applied;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean isNotFound() {
        return notFound;
    }

    public String getError() {
        return error;
    }

    public String getDetail() {
        return detail;
    }
}
