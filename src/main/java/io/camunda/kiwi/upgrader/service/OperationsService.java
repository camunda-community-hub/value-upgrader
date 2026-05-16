package io.camunda.kiwi.upgrader.service;

import io.camunda.kiwi.upgrader.model.ApplyResult;
import io.camunda.kiwi.upgrader.model.Rule;
import io.camunda.kiwi.upgrader.model.TargetType;
import io.camunda.kiwi.upgrader.yaml.YamlNodeService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements each transformation operation.
 * Equivalent to Go's pkg/transform/operations.go.
 *
 * Note on templates: Go templates use {{.Values.key}} syntax.
 * Java does not have a built-in equivalent, so we use simple
 * {{key}} substitution backed by the flat values map.
 * For advanced templates, consider adding FreeMarker or Mustache.
 */
@Service
public class OperationsService {

    private final YamlNodeService yamlNodeService;

    public OperationsService(YamlNodeService yamlNodeService) {
        this.yamlNodeService = yamlNodeService;
    }

    /**
     * Moves a value from one path to another.
     * If the target already exists in originalUserValues, skips with a warning.
     * Falls back to defaults if the source path is not in root.
     * Equivalent to Go's applyMove.
     */
    public ApplyResult applyMove(Map<String, Object> root,
                                 Map<String, Object> originalUserValues,
                                 Map<String, Object> defaults,
                                 Rule rule) {
        String fromPath = rule.fromPath();
        String toPath = rule.getTo();

        // Check if target already exists in user's original values
        if (yamlNodeService.exists(originalUserValues, toPath)) {
            return ApplyResult.skipped("target \"" + toPath + "\" already exists in user values, skipping");
        }

        // Get source value, falling back to defaults
        FallbackResult fb = getWithFallback(root, defaults, fromPath);
        if (fb.value() == null) {
            return ApplyResult.notFound("source path \"" + fromPath + "\" not found");
        }

        // If only in defaults (user never set it), skip
        if (fb.fromDefaults()) {
            yamlNodeService.delete(root, fromPath);
            return ApplyResult.notFound("source path \"" + fromPath + "\" only found in defaults, skipping");
        }

        // Deep-copy and set at new location
        Object cloned = deepCopyValue(fb.value());
        yamlNodeService.set(root, toPath, cloned);

        // Remove from old location
        yamlNodeService.delete(root, fromPath);

        return ApplyResult.applied(fromPath + " -> " + toPath);
    }

    /**
     * Removes a key from the tree.
     * Equivalent to Go's applyDelete.
     */
    public ApplyResult applyDelete(Map<String, Object> root, Rule rule) {
        String path = rule.getPath();
        Object removed = yamlNodeService.delete(root, path);
        if (removed == null) {
            return ApplyResult.notFound("path \"" + path + "\" not found, nothing to delete");
        }
        return ApplyResult.applied("deleted " + path);
    }

    /**
     * Changes the type of a scalar value.
     * Falls back to defaults if the path is not in root.
     * Equivalent to Go's applyRetype.
     */
    public ApplyResult applyRetype(Map<String, Object> root, Map<String, Object> defaults, Rule rule) {
        String path = rule.getPath();
        FallbackResult fb = getWithFallback(root, defaults, path);
        if (fb.value() == null) {
            return ApplyResult.notFound("path \"" + path + "\" not found");
        }

        String oldValue = fb.value().toString();
        TargetType targetType = TargetType.fromValue(rule.getToType());

        Object newValue;
        try {
            newValue = switch (targetType) {
                case INT -> Long.parseLong(oldValue);
                case FLOAT -> Double.parseDouble(oldValue);
                case BOOL -> parseBoolean(oldValue);
                case STRING -> oldValue;
            };
        } catch (NumberFormatException e) {
            return ApplyResult.error("cannot convert \"" + oldValue + "\" to " + targetType + ": " + e.getMessage());
        }

        yamlNodeService.set(root, path, newValue);
        return ApplyResult.applied(path + ": \"" + oldValue + "\" retyped to " + targetType);
    }

    /**
     * Maps discrete old values to new ones.
     * Falls back to defaults if the path is not in root.
     * Equivalent to Go's applyMapValues.
     */
    public ApplyResult applyMapValues(Map<String, Object> root, Map<String, Object> defaults, Rule rule) {
        String path = rule.getPath() != null && !rule.getPath().isEmpty() ? rule.getPath() : rule.fromPath();

        FallbackResult fb = getWithFallback(root, defaults, path);
        if (fb.value() == null) {
            return ApplyResult.notFound("path \"" + path + "\" not found");
        }

        String currentValue = fb.value().toString();
        String newValue = rule.getMapping().get(currentValue);
        if (newValue == null) {
            return ApplyResult.skipped("value \"" + currentValue + "\" at " + path + " not in mapping, leaving unchanged");
        }

        yamlNodeService.set(root, path, newValue);
        return ApplyResult.applied(path + ": \"" + currentValue + "\" -> \"" + newValue + "\"");
    }

    /**
     * Computes a new value using a simple template ({{key}} substitution).
     * Falls back to defaults for source paths not in root.
     * Equivalent to Go's applyTemplate.
     *
     * Template syntax: use {{Values.dot.path.key}} to reference values.
     * Example: "{{Values.image.repository}}:{{Values.image.tag}}"
     */
    public ApplyResult applyTemplate(Map<String, Object> root,
                                     Map<String, Object> originalUserValues,
                                     Map<String, Object> defaults,
                                     Rule rule) {
        String toPath = rule.getTo();

        // Check if target already exists in user's original values
        if (yamlNodeService.exists(originalUserValues, toPath)) {
            return ApplyResult.skipped("target \"" + toPath + "\" already exists in user values, skipping");
        }

        // Ensure at least one source path exists
        boolean anyFound = rule.fromPaths().stream()
                .anyMatch(p -> getWithFallback(root, defaults, p).value() != null);
        if (!anyFound) {
            return ApplyResult.notFound("none of the source paths " + rule.fromPaths() + " found");
        }

        // Build flat values map for template substitution
        Map<String, String> flatValues = yamlNodeService.flattenToMap(root);

        // Substitute {{Values.x.y}} and {{.Values.x.y}} patterns
        String result = substituteTemplate(rule.getTemplate(), flatValues);

        yamlNodeService.set(root, toPath, result);

        // Remove source paths that differ from target
        for (String p : rule.fromPaths()) {
            if (!p.equals(toPath)) {
                yamlNodeService.delete(root, p);
            }
        }

        return ApplyResult.applied("template -> " + toPath + " = \"" + result + "\"");
    }

    /**
     * Collects scalar values from multiple source paths and combines them
     * into a list at the target path.
     * Equivalent to Go's applyMergeToList.
     */
    public ApplyResult applyMergeToList(Map<String, Object> root,
                                        Map<String, Object> originalUserValues,
                                        Map<String, Object> defaults,
                                        Rule rule) {
        String toPath = rule.getTo();

        if (yamlNodeService.exists(originalUserValues, toPath)) {
            return ApplyResult.skipped("target \"" + toPath + "\" already exists in user values, skipping");
        }

        List<Object> items = new ArrayList<>();
        for (String p : rule.fromPaths()) {
            FallbackResult fb = getWithFallback(root, defaults, p);
            if (fb.value() != null) {
                items.add(fb.value());
            }
        }

        if (items.isEmpty()) {
            return ApplyResult.notFound("none of the source paths " + rule.fromPaths() + " found");
        }

        yamlNodeService.set(root, toPath, items);

        for (String p : rule.fromPaths()) {
            yamlNodeService.delete(root, p);
        }

        return ApplyResult.applied("merged " + rule.fromPaths() + " -> " + toPath + " = " + items);
    }

    /**
     * Checks if a path exists in user values and emits a notification.
     * Never modifies the document.
     * Equivalent to Go's applyNotify.
     */
    public ApplyResult applyNotify(Map<String, Object> originalUserValues, Rule rule) {
        if (!yamlNodeService.exists(originalUserValues, rule.getPath())) {
            return ApplyResult.notFound("path \"" + rule.getPath() + "\" not set by user, notification skipped");
        }
        return ApplyResult.skipped(rule.getMessage());
    }

    /**
     * Sets a path to a specific value if the user hasn't already set it.
     * Equivalent to Go's applySetDefault.
     */
    public ApplyResult applySetDefault(Map<String, Object> root,
                                       Map<String, Object> originalUserValues,
                                       Rule rule) {
        String path = rule.getPath();

        if (yamlNodeService.exists(originalUserValues, path)) {
            return ApplyResult.skipped("path \"" + path + "\" already set by user, leaving unchanged");
        }

        // Parse the value — try to detect type (bool, int, string)
        Object typedValue = parseTypedValue(rule.getValue());
        yamlNodeService.set(root, path, typedValue);

        return ApplyResult.applied(path + " set to " + rule.getValue());
    }

    // --- Helpers ---

    /**
     * Looks up a path in root first, then falls back to defaults.
     * Equivalent to Go's getWithFallback.
     */
    private FallbackResult getWithFallback(Map<String, Object> root,
                                           Map<String, Object> defaults,
                                           String path) {
        Object value = yamlNodeService.get(root, path);
        if (value != null) return new FallbackResult(value, false);

        if (defaults == null) return new FallbackResult(null, false);

        Object defValue = yamlNodeService.get(defaults, path);
        if (defValue == null) return new FallbackResult(null, false);

        // Clone from defaults and insert into root
        Object cloned = deepCopyValue(defValue);
        yamlNodeService.set(root, path, cloned);
        return new FallbackResult(yamlNodeService.get(root, path), true);
    }

    private boolean parseBoolean(String value) {
        return switch (value.toLowerCase()) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> throw new IllegalArgumentException("Cannot convert \"" + value + "\" to bool");
        };
    }

    /**
     * Substitutes {{Values.x.y.z}} placeholders in a template string.
     * Equivalent to Go's text/template execution with .Values flat map.
     */
    private String substituteTemplate(String template, Map<String, String> flatValues) {
        // Support both {{Values.key}} and {{.Values.key}} syntaxes
        Pattern pattern = Pattern.compile("\\{\\{[.]?Values[.]([^}]+)}}");
        Matcher matcher = pattern.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = flatValues.getOrDefault(key, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Attempts to parse a string value into its natural type (bool, int, double, string).
     */
    private Object parseTypedValue(String value) {
        if (value == null) return null;
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        try { return Long.parseLong(value); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) {}
        return value;
    }

    @SuppressWarnings("unchecked")
    private Object deepCopyValue(Object value) {
        if (value instanceof Map) {
            return yamlNodeService.deepCopy((Map<String, Object>) value);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object item : list) copy.add(deepCopyValue(item));
            return copy;
        }
        return value;
    }

    /** Result of a fallback lookup. */
    private record FallbackResult(Object value, boolean fromDefaults) {}
}
