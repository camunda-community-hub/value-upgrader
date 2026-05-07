package com.camunda.valuestransformer.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents a single transformation rule.
 * Equivalent to Go's Rule struct in pkg/transform/types.go.
 *
 * In Go, `from` could be a scalar or a list (yaml.Node).
 * Here we model it as a List<String> populated by the RulesFile deserializer.
 */
@Data
public class Rule {

    /** Type of transformation. */
    private RuleType type;

    /** Human-readable explanation of why this rule exists. */
    private String description;

    // --- move / template ---
    /** Source dot-path(s). Can be a single string or a list (for template rules). */
    private List<String> from;

    /** Destination dot-path (move, template, merge-to-list). */
    private String to;

    // --- delete / retype / notify / set-default ---
    /** Dot-path for delete, retype, notify, set-default, and map-values operations. */
    private String path;

    // --- retype ---
    /** Target type for retype operations: int, float, bool, string. */
    private String toType;

    // --- map-values ---
    /** Maps old discrete values to new ones. */
    private Map<String, String> mapping;

    // --- template ---
    /** Go/Java template string for computing new values. */
    private String template;

    // --- notify ---
    /** Message to display to the user when a notify rule triggers. */
    private String message;

    // --- set-default ---
    /** Raw YAML string value to set when a set-default rule fires. */
    private String value;

    /**
     * Returns the source path(s) for this rule.
     * Equivalent to Go's Rule.FromPaths().
     */
    public List<String> fromPaths() {
        if (from != null && !from.isEmpty()) {
            return from;
        }
        if (path != null && !path.isEmpty()) {
            return List.of(path);
        }
        return List.of();
    }

    /**
     * Returns the single source path for this rule.
     * Equivalent to Go's Rule.FromPath().
     */
    public String fromPath() {
        List<String> paths = fromPaths();
        return paths.isEmpty() ? "" : paths.get(0);
    }
}
