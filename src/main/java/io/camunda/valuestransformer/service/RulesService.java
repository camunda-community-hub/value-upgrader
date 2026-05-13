package io.camunda.valuestransformer.service;

import io.camunda.valuestransformer.model.Rule;
import io.camunda.valuestransformer.model.RuleType;
import io.camunda.valuestransformer.model.RulesFile;
import io.camunda.valuestransformer.model.TargetType;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 * Parses and validates rules YAML files.
 * Equivalent to Go's pkg/transform/rules.go.
 */
@Service
public class RulesService {

    /**
     * Parses a rules YAML string into a RulesFile.
     * Equivalent to Go's ParseRules(data []byte).
     */
    @SuppressWarnings("unchecked")
    public RulesFile parseRules(String yamlContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> raw = yaml.load(yamlContent);

        RulesFile rulesFile = new RulesFile();

        // version
        Object version = raw.get("version");
        int v = (version instanceof Integer i) ? i : 1;
        if (v == 0) v = 1;
        if (v != 1) throw new IllegalArgumentException("Unsupported rules file version: " + v);
        rulesFile.setVersion(v);

        // description
        rulesFile.setDescription((String) raw.getOrDefault("description", ""));

        // rules
        List<Map<String, Object>> rawRules = (List<Map<String, Object>>) raw.get("rules");
        if (rawRules == null) throw new IllegalArgumentException("Rules file must contain a 'rules' list");

        List<Rule> rules = rawRules.stream().map(this::parseRule).toList();
        rulesFile.setRules(rules);

        validateRules(rulesFile);
        return rulesFile;
    }

    @SuppressWarnings("unchecked")
    private Rule parseRule(Map<String, Object> raw) {
        Rule rule = new Rule();

        String typeStr = (String) raw.get("type");
        if (typeStr == null) throw new IllegalArgumentException("Rule is missing 'type' field");
        rule.setType(RuleType.fromValue(typeStr));

        rule.setDescription((String) raw.getOrDefault("description", ""));
        rule.setTo((String) raw.getOrDefault("to", ""));
        rule.setPath((String) raw.getOrDefault("path", ""));
        rule.setToType((String) raw.getOrDefault("to_type", ""));
        rule.setTemplate((String) raw.getOrDefault("template", ""));
        rule.setMessage((String) raw.getOrDefault("message", ""));

        // value is stored as a raw YAML string for set-default
        Object value = raw.get("value");
        rule.setValue(value != null ? value.toString() : null);

        // mapping
        Object mapping = raw.get("mapping");
        if (mapping instanceof Map<?, ?> m) {
            rule.setMapping((Map<String, String>) m);
        }

        // from: can be a scalar string or a list of strings
        Object from = raw.get("from");
        if (from instanceof String s) {
            rule.setFrom(List.of(s));
        } else if (from instanceof List<?> list) {
            rule.setFrom(list.stream().map(Object::toString).toList());
        }

        return rule;
    }

    /**
     * Validates that each rule has the required fields for its type.
     * Equivalent to Go's validateRules.
     */
    private void validateRules(RulesFile rf) {
        List<Rule> rules = rf.getRules();
        for (int i = 0; i < rules.size(); i++) {
            Rule r = rules.get(i);
            int ruleNum = i + 1;
            switch (r.getType()) {
                case MOVE -> {
                    if (r.fromPath().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (move): 'from' is required");
                    if (r.getTo() == null || r.getTo().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (move): 'to' is required");
                }
                case DELETE -> {
                    if (r.getPath() == null || r.getPath().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (delete): 'path' is required");
                }
                case RETYPE -> {
                    if (r.getPath() == null || r.getPath().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (retype): 'path' is required");
                    // validate to_type
                    TargetType.fromValue(r.getToType()); // throws if invalid
                }
                case MAP_VALUES -> {
                    String path = r.getPath() != null && !r.getPath().isEmpty() ? r.getPath() : r.fromPath();
                    if (path.isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (map-values): 'path' is required");
                    if (r.getMapping() == null || r.getMapping().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (map-values): 'mapping' must not be empty");
                }
                case TEMPLATE -> {
                    if (r.fromPaths().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (template): 'from' is required");
                    if (r.getTo() == null || r.getTo().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (template): 'to' is required");
                    if (r.getTemplate() == null || r.getTemplate().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (template): 'template' is required");
                }
                case MERGE_TO_LIST -> {
                    if (r.fromPaths().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (merge-to-list): 'from' is required");
                    if (r.getTo() == null || r.getTo().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (merge-to-list): 'to' is required");
                }
                case NOTIFY -> {
                    if (r.getPath() == null || r.getPath().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (notify): 'path' is required");
                    if (r.getMessage() == null || r.getMessage().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (notify): 'message' is required");
                }
                case SET_DEFAULT -> {
                    if (r.getPath() == null || r.getPath().isEmpty())
                        throw new IllegalArgumentException("Rule " + ruleNum + " (set-default): 'path' is required");
                    if (r.getValue() == null)
                        throw new IllegalArgumentException("Rule " + ruleNum + " (set-default): 'value' is required");
                }
                default ->
                        throw new IllegalArgumentException("Rule " + ruleNum + ": unknown rule type '" + r.getType() + "'");
            }
        }
    }
}
