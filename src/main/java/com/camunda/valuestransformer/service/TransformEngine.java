package com.camunda.valuestransformer.service;

import com.camunda.valuestransformer.model.*;
import com.camunda.valuestransformer.yaml.YamlNodeService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Performs the full transformation pipeline on a values.yaml document.
 * Equivalent to Go's pkg/transform/engine.go (Engine struct and Run method).
 *
 * Replaces CLI-based I/O with in-memory string processing,
 * making it suitable for a REST API.
 */
@Service
public class TransformEngine {

    private final RulesService rulesService;
    private final OperationsService operationsService;
    private final YamlNodeService yamlNodeService;

    public TransformEngine(RulesService rulesService,
                           OperationsService operationsService,
                           YamlNodeService yamlNodeService) {
        this.rulesService = rulesService;
        this.operationsService = operationsService;
        this.yamlNodeService = yamlNodeService;
    }

    /**
     * Executes the full transformation pipeline:
     * 1. Parse rules
     * 2. Parse user values
     * 3. Optionally parse old chart defaults (kept as a separate fallback map)
     * 4. Apply rules (operations fall back to defaults for missing keys)
     * 5. Prune empty maps left behind by moves/deletes
     * 6. Return result
     *
     * Equivalent to Go's Engine.Run().
     *
     * @param rulesYaml        YAML string of the rules file (required)
     * @param inputYaml        YAML string of the user's values.yaml (required)
     * @param oldDefaultsYaml  YAML string of the old chart defaults (optional, may be null)
     * @param dryRun           if true, returns null outputYaml
     * @return EngineResult with the transformed YAML and report
     */
    public EngineResult run(String rulesYaml, String inputYaml, String oldDefaultsYaml, boolean dryRun) {

        // 1. Parse rules
        RulesFile rules = rulesService.parseRules(rulesYaml);

        // 2. Parse user values
        Map<String, Object> userDoc = yamlNodeService.parse(inputYaml);

        // 3. Keep a pristine clone for conflict detection
        Map<String, Object> originalUserValues = yamlNodeService.deepCopy(userDoc);

        // 4. Optionally parse old chart defaults
        Map<String, Object> defaultsDoc = null;
        if (oldDefaultsYaml != null && !oldDefaultsYaml.isBlank()) {
            defaultsDoc = yamlNodeService.parse(oldDefaultsYaml);
        }

        // 5. Apply rules
        TransformReport report = applyRules(userDoc, originalUserValues, defaultsDoc, rules);

        // 6. Prune empty mappings
        yamlNodeService.pruneEmptyMappings(userDoc);

        // 7. Serialize output (or null if dry run)
        String outputYaml = dryRun ? null : yamlNodeService.dump(userDoc);

        return new EngineResult(outputYaml, report);
    }

    /**
     * Applies each rule in order and collects results into a report.
     * Equivalent to Go's Engine.applyRules.
     */
    private TransformReport applyRules(Map<String, Object> root,
                                       Map<String, Object> originalUserValues,
                                       Map<String, Object> defaults,
                                       RulesFile rf) {
        TransformReport report = new TransformReport();

        for (Rule rule : rf.getRules()) {
            String ruleType = rule.getType().getValue();
            String desc = rule.getDescription();

            ApplyResult result = switch (rule.getType()) {
                case MOVE         -> operationsService.applyMove(root, originalUserValues, defaults, rule);
                case DELETE       -> operationsService.applyDelete(root, rule);
                case RETYPE       -> operationsService.applyRetype(root, defaults, rule);
                case MAP_VALUES   -> operationsService.applyMapValues(root, defaults, rule);
                case TEMPLATE     -> operationsService.applyTemplate(root, originalUserValues, defaults, rule);
                case MERGE_TO_LIST -> operationsService.applyMergeToList(root, originalUserValues, defaults, rule);
                case NOTIFY       -> operationsService.applyNotify(originalUserValues, rule);
                case SET_DEFAULT  -> operationsService.applySetDefault(root, originalUserValues, rule);
            };

            // Resolve the most meaningful path for the report
            String path = rule.getPath();
            if (path == null || path.isEmpty()) path = rule.fromPath();
            if (path == null || path.isEmpty()) path = rule.getTo();

            if (result.hasError()) {
                report.addError(ruleType, path, desc, result.getError());
            } else if (result.isNotFound()) {
                report.addSkip(ruleType, path, desc, result.getDetail());
            } else if (result.isSkipped()) {
                report.addWarning(ruleType, path, desc, result.getDetail());
            } else if (result.isApplied()) {
                report.addChange(ruleType, path, desc, result.getDetail());
            }
        }

        return report;
    }

    /**
     * Result of a transformation run.
     */
    public record EngineResult(String outputYaml, TransformReport report) {}
}
