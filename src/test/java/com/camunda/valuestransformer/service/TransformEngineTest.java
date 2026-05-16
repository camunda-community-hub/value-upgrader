package com.camunda.valuestransformer.service;

import io.camunda.kiwi.upgrader.service.OperationsService;
import io.camunda.kiwi.upgrader.service.RulesService;
import io.camunda.kiwi.upgrader.service.TransformEngine;
import io.camunda.kiwi.upgrader.yaml.YamlNodeService;
import io.camunda.kiwi.upgrader.model.TransformReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the TransformEngine.
 * Equivalent to Go's engine_test.go and operations_test.go.
 */
class TransformEngineTest {

    private TransformEngine engine;
    private YamlNodeService yamlNodeService;

    @BeforeEach
    void setUp() {
        yamlNodeService = new YamlNodeService();
        OperationsService ops = new OperationsService(yamlNodeService);
        RulesService rules = new RulesService();
        engine = new TransformEngine(rules, ops, yamlNodeService);
    }

    // --- move ---

    @Test
    void move_renamesKey() {
        String rules = """
                version: 1
                rules:
                  - type: move
                    from: image.tag
                    to: global.image.tag
                """;
        String input = """
                image:
                  tag: latest
                """;

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().hasErrors()).isFalse();
        assertThat(result.report().countByKind(TransformReport.EntryKind.CHANGE)).isEqualTo(1);

        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.get(output, "global.image.tag")).isEqualTo("latest");
        assertThat(yamlNodeService.exists(output, "image.tag")).isFalse();
    }

    @Test
    void move_skipsWhenTargetAlreadyExistsInUserValues() {
        String rules = """
                version: 1
                rules:
                  - type: move
                    from: image.tag
                    to: global.image.tag
                """;
        String input = """
                image:
                  tag: latest
                global:
                  image:
                    tag: already-set
                """;

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().countByKind(TransformReport.EntryKind.WARNING)).isEqualTo(1);
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.get(output, "global.image.tag")).isEqualTo("already-set");
    }

    // --- delete ---

    @Test
    void delete_removesKey() {
        String rules = """
                version: 1
                rules:
                  - type: delete
                    path: deprecated.key
                """;
        String input = """
                deprecated:
                  key: value
                keep: me
                """;

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().hasErrors()).isFalse();
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.exists(output, "deprecated.key")).isFalse();
        assertThat(yamlNodeService.get(output, "keep")).isEqualTo("me");
    }

    @Test
    void delete_skipsWhenKeyNotFound() {
        String rules = """
                version: 1
                rules:
                  - type: delete
                    path: nonexistent.key
                """;
        String input = "keep: me\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().countByKind(TransformReport.EntryKind.SKIP)).isEqualTo(1);
    }

    // --- retype ---

    @Test
    void retype_convertsStringToInt() {
        String rules = """
                version: 1
                rules:
                  - type: retype
                    path: replicas
                    to_type: int
                """;
        String input = "replicas: \"3\"\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().hasErrors()).isFalse();
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat( ((Number)yamlNodeService.get(output, "replicas")).longValue()).isEqualTo(3L);
    }

    @Test
    void retype_convertsStringToBool() {
        String rules = """
                version: 1
                rules:
                  - type: retype
                    path: enabled
                    to_type: bool
                """;
        String input = "enabled: \"true\"\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().hasErrors()).isFalse();
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.get(output, "enabled")).isEqualTo(true);
    }

    // --- map-values ---

    @Test
    void mapValues_replacesKnownValue() {
        String rules = """
                version: 1
                rules:
                  - type: map-values
                    path: logLevel
                    mapping:
                      DEBUG: trace
                      INFO: info
                """;
        String input = "logLevel: DEBUG\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().hasErrors()).isFalse();
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.get(output, "logLevel")).isEqualTo("trace");
    }

    @Test
    void mapValues_leavesUnknownValueUnchanged() {
        String rules = """
                version: 1
                rules:
                  - type: map-values
                    path: logLevel
                    mapping:
                      DEBUG: trace
                """;
        String input = "logLevel: WARN\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().countByKind(TransformReport.EntryKind.WARNING)).isEqualTo(1);
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.get(output, "logLevel")).isEqualTo("WARN");
    }

    // --- notify ---

    @Test
    void notify_recordsWarningWhenPathExists() {
        String rules = """
                version: 1
                rules:
                  - type: notify
                    path: oldKey
                    message: "Please migrate oldKey manually"
                """;
        String input = "oldKey: someValue\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().countByKind(TransformReport.EntryKind.WARNING)).isEqualTo(1);
        // notify never modifies the document
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.get(output, "oldKey")).isEqualTo("someValue");
    }

    // --- set-default ---

    @Test
    void setDefault_setsValueWhenPathAbsent() {
        String rules = """
                version: 1
                rules:
                  - type: set-default
                    path: newKey
                    value: "defaultValue"
                """;
        String input = "existingKey: existingValue\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().hasErrors()).isFalse();
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.get(output, "newKey")).isEqualTo("defaultValue");
    }

    @Test
    void setDefault_skipsWhenPathAlreadySet() {
        String rules = """
                version: 1
                rules:
                  - type: set-default
                    path: existingKey
                    value: "ignored"
                """;
        String input = "existingKey: userValue\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, false);

        assertThat(result.report().countByKind(TransformReport.EntryKind.WARNING)).isEqualTo(1);
        var output = yamlNodeService.parse(result.outputYaml());
        assertThat(yamlNodeService.get(output, "existingKey")).isEqualTo("userValue");
    }

    // --- dry run ---

    @Test
    void dryRun_returnsNullOutputYaml() {
        String rules = """
                version: 1
                rules:
                  - type: delete
                    path: key
                """;
        String input = "key: value\n";

        TransformEngine.EngineResult result = engine.run(rules, input, null, true);

        assertThat(result.outputYaml()).isNull();
        assertThat(result.report().countByKind(TransformReport.EntryKind.CHANGE)).isEqualTo(1);
    }

    // --- old defaults fallback ---

    @Test
    void move_usesDefaultsAsFallbackWhenUserValueAbsent() {
        String rules = """
                version: 1
                rules:
                  - type: move
                    from: image.tag
                    to: global.image.tag
                """;
        String input = "replicas: 1\n"; // user never set image.tag
        String defaults = "image:\n  tag: stable\n";

        TransformEngine.EngineResult result = engine.run(rules, input, defaults, false);

        // Should skip because the value only came from defaults
        assertThat(result.report().countByKind(TransformReport.EntryKind.SKIP)).isEqualTo(1);
    }

    // --- invalid rules ---

    @Test
    void invalidRulesVersion_throwsException() {
        String rules = """
                version: 99
                rules: []
                """;
        assertThatThrownBy(() -> engine.run(rules, "{}", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported rules file version");
    }

    @Test
    void missingRequiredField_throwsException() {
        String rules = """
                version: 1
                rules:
                  - type: move
                    to: somewhere
                """;  // missing 'from'
        assertThatThrownBy(() -> engine.run(rules, "{}", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'from' is required");
    }
}
