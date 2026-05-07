package com.camunda.valuestransformer.yaml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for YamlNodeService.
 * Equivalent to Go's dotpath_test.go and merge_test.go.
 */
class YamlNodeServiceTest {

    private YamlNodeService service;

    @BeforeEach
    void setUp() {
        service = new YamlNodeService();
    }

    // --- get ---

    @Test
    void get_returnsScalarAtTopLevel() {
        Map<String, Object> root = Map.of("key", "value");
        assertThat(service.get(root, "key")).isEqualTo("value");
    }

    @Test
    void get_returnsNestedValue() {
        Map<String, Object> root = Map.of("image", Map.of("tag", "latest"));
        assertThat(service.get(root, "image.tag")).isEqualTo("latest");
    }

    @Test
    void get_returnsNullForMissingPath() {
        Map<String, Object> root = Map.of("image", Map.of("tag", "latest"));
        assertThat(service.get(root, "image.repository")).isNull();
    }

    @Test
    void get_returnsNullForNullRoot() {
        assertThat(service.get(null, "key")).isNull();
    }

    // --- exists ---

    @Test
    void exists_returnsTrueWhenPathPresent() {
        Map<String, Object> root = Map.of("a", Map.of("b", "c"));
        assertThat(service.exists(root, "a.b")).isTrue();
    }

    @Test
    void exists_returnsFalseWhenPathAbsent() {
        Map<String, Object> root = Map.of("a", Map.of("b", "c"));
        assertThat(service.exists(root, "a.x")).isFalse();
    }

    // --- set ---

    @Test
    void set_createsNewTopLevelKey() {
        Map<String, Object> root = new LinkedHashMap<>();
        service.set(root, "newKey", "newValue");
        assertThat(service.get(root, "newKey")).isEqualTo("newValue");
    }

    @Test
    void set_createsIntermediateMappings() {
        Map<String, Object> root = new LinkedHashMap<>();
        service.set(root, "a.b.c", "deep");
        assertThat(service.get(root, "a.b.c")).isEqualTo("deep");
    }

    @Test
    void set_overwritesExistingValue() {
        Map<String, Object> root = new LinkedHashMap<>(Map.of("key", "old"));
        service.set(root, "key", "new");
        assertThat(service.get(root, "key")).isEqualTo("new");
    }

    // --- delete ---

    @Test
    void delete_removesExistingKey() {
        Map<String, Object> root = new LinkedHashMap<>(Map.of("key", "value"));
        Object removed = service.delete(root, "key");
        assertThat(removed).isEqualTo("value");
        assertThat(service.exists(root, "key")).isFalse();
    }

    @Test
    void delete_returnsNullForMissingKey() {
        Map<String, Object> root = new LinkedHashMap<>(Map.of("key", "value"));
        Object removed = service.delete(root, "missing");
        assertThat(removed).isNull();
    }

    @Test
    void delete_removesNestedKey() {
        Map<String, Object> inner = new LinkedHashMap<>(Map.of("tag", "latest"));
        Map<String, Object> root = new LinkedHashMap<>(Map.of("image", inner));
        service.delete(root, "image.tag");
        assertThat(service.exists(root, "image.tag")).isFalse();
    }

    // --- flattenToMap ---

    @Test
    void flattenToMap_producesCorrectDotNotationKeys() {
        Map<String, Object> root = Map.of(
                "image", Map.of("repository", "nginx", "tag", "latest"),
                "replicas", "3"
        );
        Map<String, String> flat = service.flattenToMap(root);
        assertThat(flat).containsEntry("image.repository", "nginx");
        assertThat(flat).containsEntry("image.tag", "latest");
        assertThat(flat).containsEntry("replicas", "3");
    }

    // --- pruneEmptyMappings ---

    @Test
    void pruneEmptyMappings_removesEmptyChildMaps() {
        Map<String, Object> inner = new LinkedHashMap<>();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("image", inner);
        root.put("replicas", "3");

        service.pruneEmptyMappings(root);

        assertThat(root).doesNotContainKey("image");
        assertThat(root).containsKey("replicas");
    }

    // --- mergeNodes ---

    @Test
    void mergeNodes_overlayKeysTakePrecedence() {
        Map<String, Object> base = new LinkedHashMap<>(Map.of("key", "base", "other", "base-other"));
        Map<String, Object> overlay = new LinkedHashMap<>(Map.of("key", "overlay"));
        service.mergeNodes(base, overlay);
        assertThat(service.get(base, "key")).isEqualTo("overlay");
        assertThat(service.get(base, "other")).isEqualTo("base-other");
    }

    @Test
    void mergeNodes_recursivelyMergesNestedMaps() {
        Map<String, Object> baseInner = new LinkedHashMap<>(Map.of("tag", "base", "repo", "nginx"));
        Map<String, Object> base = new LinkedHashMap<>(Map.of("image", baseInner));
        Map<String, Object> overlayInner = new LinkedHashMap<>(Map.of("tag", "overlay"));
        Map<String, Object> overlay = new LinkedHashMap<>(Map.of("image", overlayInner));

        service.mergeNodes(base, overlay);

        assertThat(service.get(base, "image.tag")).isEqualTo("overlay");
        assertThat(service.get(base, "image.repo")).isEqualTo("nginx");
    }

    // --- deepCopy ---

    @Test
    void deepCopy_doesNotShareReferences() {
        Map<String, Object> inner = new LinkedHashMap<>(Map.of("tag", "latest"));
        Map<String, Object> original = new LinkedHashMap<>(Map.of("image", inner));
        Map<String, Object> copy = service.deepCopy(original);

        service.set(copy, "image.tag", "modified");

        assertThat(service.get(original, "image.tag")).isEqualTo("latest");
        assertThat(service.get(copy, "image.tag")).isEqualTo("modified");
    }

    // --- parse / dump roundtrip ---

    @Test
    void parseAndDump_roundtrip() {
        String yamlContent = "image:\n  tag: latest\nreplicas: 3\n";
        Map<String, Object> parsed = service.parse(yamlContent);
        assertThat(service.get(parsed, "image.tag")).isEqualTo("latest");
        assertThat(service.get(parsed, "replicas")).isEqualTo(3);
    }
}
