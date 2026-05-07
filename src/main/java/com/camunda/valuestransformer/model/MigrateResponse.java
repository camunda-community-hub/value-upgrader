package com.camunda.valuestransformer.model;

import lombok.Data;

import java.util.List;

/**
 * Response body for the POST /migrate endpoint.
 */
@Data
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
}
