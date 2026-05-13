package io.camunda.valuestransformer.yaml;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Provides dot-notation path operations on YAML documents represented as
 * nested Map<String, Object> trees (SnakeYAML's natural representation).
 *
 * Equivalent to Go's pkg/yaml/dotpath.go and pkg/yaml/merge.go.
 *
 * Note: Unlike the Go version which preserves comments via yaml.Node trees,
 * SnakeYAML's map representation does not preserve YAML comments.
 * Comments are therefore not preserved in the output (acceptable tradeoff for Java).
 */
@Component
public class YamlNodeService {

    private final Yaml yaml;

    public YamlNodeService() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
    }

    /**
     * Parses a YAML string into a mutable nested Map tree.
     * Equivalent to Go's loadYAMLFile / yaml.Unmarshal.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object parsed = yaml.load(yamlContent);
        if (parsed instanceof Map<?, ?> map) {
            return deepCopy((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    /**
     * Serializes a Map tree back to a YAML string.
     * Equivalent to Go's yaml.Encoder.Encode.
     */
    public String dump(Map<String, Object> data) {
        return yaml.dump(data);
    }

    /**
     * Returns the value at the given dot-separated path, or null if not found.
     * Equivalent to Go's yamlpath.Get.
     */
    @SuppressWarnings("unchecked")
    public Object get(Map<String, Object> root, String path) {
        if (root == null || path == null || path.isEmpty()) return null;
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) return null;
            current = (Map<String, Object>) next;
        }
        return current.get(parts[parts.length - 1]);
    }

    /**
     * Returns true if the given dot-separated path exists in the tree.
     * Equivalent to Go's yamlpath.Exists.
     */
    public boolean exists(Map<String, Object> root, String path) {
        return get(root, path) != null;
    }

    /**
     * Sets the value at the given dot-separated path, creating intermediate
     * maps as needed. Returns the previous value, or null.
     * Equivalent to Go's yamlpath.Set.
     */
    @SuppressWarnings("unchecked")
    public Object set(Map<String, Object> root, String path, Object value) {
        if (root == null || path == null || path.isEmpty()) return null;
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(parts[i], newMap);
                current = newMap;
            } else {
                current = (Map<String, Object>) next;
            }
        }
        String lastKey = parts[parts.length - 1];
        Object old = current.get(lastKey);
        current.put(lastKey, value);
        return old;
    }

    /**
     * Removes the key at the given dot-separated path.
     * Returns the removed value, or null if the path did not exist.
     * Equivalent to Go's yamlpath.Delete.
     */
    @SuppressWarnings("unchecked")
    public Object delete(Map<String, Object> root, String path) {
        if (root == null || path == null || path.isEmpty()) return null;
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) return null;
            current = (Map<String, Object>) next;
        }
        return current.remove(parts[parts.length - 1]);
    }

    /**
     * Deep-copies a Map tree.
     * Equivalent to Go's yamlpath.Clone.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deepCopy(Map<String, Object> source) {
        if (source == null) return null;
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) value));
            } else if (value instanceof List) {
                copy.put(entry.getKey(), deepCopyList((List<Object>) value));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private List<Object> deepCopyList(List<Object> list) {
        List<Object> copy = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                copy.add(deepCopy((Map<String, Object>) item));
            } else if (item instanceof List) {
                copy.add(deepCopyList((List<Object>) item));
            } else {
                copy.add(item);
            }
        }
        return copy;
    }

    /**
     * Flattens a Map tree into a flat map with dot-notation keys.
     * e.g. {"server.host": "localhost", "server.port": "8080"}
     * Equivalent to Go's yamlpath.FlattenToMap.
     */
    public Map<String, String> flattenToMap(Map<String, Object> root) {
        Map<String, String> result = new LinkedHashMap<>();
        flattenNode(root, "", result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void flattenNode(Object node, String prefix, Map<String, String> result) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                flattenNode(entry.getValue(), newPrefix, result);
            }
        } else if (node != null) {
            result.put(prefix, node.toString());
        }
    }

    /**
     * Merges overlay on top of base (overlay keys take precedence).
     * The base map is modified in place.
     * Equivalent to Go's yamlpath.MergeNodes / mergeMapping.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> mergeNodes(Map<String, Object> base, Map<String, Object> overlay) {
        if (base == null) return overlay;
        if (overlay == null) return base;
        for (Map.Entry<String, Object> entry : overlay.entrySet()) {
            String key = entry.getKey();
            Object overlayVal = entry.getValue();
            Object baseVal = base.get(key);
            if (baseVal instanceof Map && overlayVal instanceof Map) {
                mergeNodes((Map<String, Object>) baseVal, (Map<String, Object>) overlayVal);
            } else {
                base.put(key, overlayVal);
            }
        }
        return base;
    }

    /**
     * Recursively removes map entries whose values are empty maps.
     * Equivalent to Go's yamlpath.PruneEmptyMappings.
     */
    @SuppressWarnings("unchecked")
    public void pruneEmptyMappings(Map<String, Object> root) {
        if (root == null) return;
        Iterator<Map.Entry<String, Object>> it = root.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            Object val = entry.getValue();
            if (val instanceof Map<?, ?> childMap) {
                pruneEmptyMappings((Map<String, Object>) childMap);
                if (childMap.isEmpty()) {
                    it.remove();
                }
            }
        }
    }
}
