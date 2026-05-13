package io.camunda.valuestransformer.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Identifies the kind of transformation.
 * Equivalent to Go's RuleType string constants.
 */
public enum RuleType {
    MOVE("move"),
    DELETE("delete"),
    RETYPE("retype"),
    MAP_VALUES("map-values"),
    TEMPLATE("template"),
    MERGE_TO_LIST("merge-to-list"),
    NOTIFY("notify"),
    SET_DEFAULT("set-default");

    private final String value;

    RuleType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RuleType fromValue(String value) {
        for (RuleType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown rule type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
