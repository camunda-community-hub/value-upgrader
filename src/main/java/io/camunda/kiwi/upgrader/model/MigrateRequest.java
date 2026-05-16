package io.camunda.kiwi.upgrader.model;


/**
 * Request body for the POST /migrate endpoint.
 * Replaces the CLI flags from Go's cmd/root.go (cobra command).
 *
 * All YAML content is passed as raw strings so the API is stateless
 * and does not depend on the server's filesystem.
 */

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

    public String getRulesYaml() {
        return rulesYaml;
    }

    public void setRulesYaml(String rulesYaml) {
        this.rulesYaml = rulesYaml;
    }

    public String getInputYaml() {
        return inputYaml;
    }

    public void setInputYaml(String inputYaml) {
        this.inputYaml = inputYaml;
    }

    public String getOldDefaultsYaml() {
        return oldDefaultsYaml;
    }

    public void setOldDefaultsYaml(String oldDefaultsYaml) {
        this.oldDefaultsYaml = oldDefaultsYaml;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
