package io.camunda.kiwi.upgrader.model;

import java.util.List;

/**
 * Response body for the POST /migrate endpoint.
 */

public class MigrateResponse {

    /** The transformed YAML content. Null when dryRun=true. */
    private String outputYaml;

    /** Human-readable transformation report. */
    private List<TransformReport.Entry> reportEntries;

    private String reportString;

    /** One-line summary (e.g. "3 changes, 1 warnings, 2 skipped, 0 errors"). */
    private String summary;

    /** True if there were any errors during transformation. */
    private boolean hasErrors;

    public String getOutputYaml() {
        return outputYaml;
    }

    public void setOutputYaml(String outputYaml) {
        this.outputYaml = outputYaml;
    }

    public List<TransformReport.Entry> getReportEntries() {
        return reportEntries;
    }

    public void setReportEntries(List<TransformReport.Entry> reportEntries) {
        this.reportEntries = reportEntries;
    }

    public String getReportString() {
        return reportString;
    }

    public void setReportString(String reportString) {
        this.reportString = reportString;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }
}
