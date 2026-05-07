package com.camunda.valuestransformer.model;

import lombok.Data;

/**
 * Request body for the POST /migrate endpoint.
 * Replaces the CLI flags from Go's cmd/root.go (cobra command).
 *
 * All YAML content is passed as raw strings so the API is stateless
 * and does not depend on the server's filesystem.
 */
@Data
public class MigrateRequest {

    /**
     * The YAML content of the transformation rules file (required).
     * Equivalent to --rules flag.
     */
    private String rulesYaml;

    /**
     * The YAML content of the user's values.yaml to transform (required).
     * Equivalent to --input flag.
     */
    private String inputYaml;

    /**
     * The YAML content of the old chart's default values.yaml (optional).
     * Used as fallback when a rule references a key the user hasn't set.
     * Equivalent to --old-defaults flag.
     */
    private String oldDefaultsYaml;

    /**
     * If true, only report what would change without returning the transformed YAML.
     * Equivalent to --dry-run flag.
     */
    private boolean dryRun;

    private String version;
}
