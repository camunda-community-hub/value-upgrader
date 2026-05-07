package com.camunda.valuestransformer.model;

import lombok.Data;

import java.util.List;

/**
 * Top-level structure of a rules YAML file.
 * Equivalent to Go's RulesFile struct.
 */
@Data
public class RulesFile {

    /** Schema version of the rules file. Defaults to 1. */
    private int version = 1;

    /** Human-readable description of this migration. */
    private String description;

    /** Ordered list of transformation rules. */
    private List<Rule> rules;
}
