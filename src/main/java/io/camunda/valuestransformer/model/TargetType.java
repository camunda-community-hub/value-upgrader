package io.camunda.valuestransformer.model;

/**
 * Valid types for retype operations.
 * Equivalent to Go's TargetType constants.
 */
public enum TargetType {
    INT("int"),
    FLOAT("float"),
    BOOL("bool"),
    STRING("string");

    private final String value;

    TargetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TargetType fromValue(String value) {
        for (TargetType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown target type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
