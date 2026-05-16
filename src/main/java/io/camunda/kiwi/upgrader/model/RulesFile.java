package io.camunda.kiwi.upgrader.model;

import java.util.List;

/**
 * Top-level structure of a rules YAML file.
 * Equivalent to Go's RulesFile struct.
 */

public class RulesFile {

    /** Schema version of the rules file. Defaults to 1. */
    private int version = 1;

    /** Human-readable description of this migration. */
    private String description;

    /** Ordered list of transformation rules. */
    private List<Rule> rules;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
}
