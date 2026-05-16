package io.camunda.kiwi.reviewer;

import io.camunda.kiwi.reviewer.RuleEvaluationResult.Details.ExpectedValue;
import io.camunda.kiwi.reviewer.RuleEvaluationResult.Details.UnexpectedValue;
import io.camunda.kiwi.reviewer.VersionRange.Equals;
import com.fasterxml.jackson.core.Version;
import io.camunda.kiwi.reviewer.helm.HelmService;
import org.camunda.feel.FeelEngine;
import org.camunda.feel.api.EvaluationResult;
import org.camunda.feel.api.FeelEngineApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.camunda.kiwi.reviewer.NestedMapUtil.*;

@Service
public class ReviewService {

  private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

  public static final String DEFAULT_VALUES_GROUP_ID = "default-values";
  public static final String NON_EXISTING_VALUES_GROUP_ID = "non-existing-values";

  private final VersionRangeParser versionRangeParser;
  private final ReviewProperties reviewProperties;
  private final HelmService helmService;
  private final FeelEngineApi feelEngine;
  private final List<Pattern> wildcardProperties;

  public ReviewService(
      VersionRangeParser versionRangeParser,
      ReviewProperties reviewProperties,
      HelmService helmService,
      FeelEngineApi feelEngine) {
    this.versionRangeParser = versionRangeParser;
    this.reviewProperties = reviewProperties;
    this.helmService = helmService;
    this.feelEngine = feelEngine;
    this.wildcardProperties =
        reviewProperties.wildcardProperties().stream().map(Pattern::compile).toList();
  }

  @SafeVarargs
  public final List<RuleEvaluationResultGroup> reviewValues(
      String versionString, Map<String, Object>... valuesUnmerged) {
    Map<String, Object> values = NestedMapUtil.merge(valuesUnmerged);

    return reviewMergedValues(versionString, values);
  }

  final List<RuleEvaluationResultGroup> reviewMergedValues(
      String versionString, Map<String, Object> values) {
    Version version = versionRangeParser.parse(versionString).as(Equals.class).version();
    List<RuleEvaluationResultGroup> results = new ArrayList<>();
    Map<String, FeelEngine.Failure> failures = new HashMap<>();

    runDedicatedRules(values, version, results, failures);
    nonExistentProperties(values, version, results, failures);

    onlyNonDefaults(values, version, results, failures);
    if (!failures.isEmpty()) {
      logger.warn(
          "There are failures: \n{}",
          failures.entrySet().stream()
              .map(e -> e.getKey() + ": " + e.getValue())
              .collect(Collectors.joining("\n")));
    }

    return results;
  }

  /**
   * Verify that only non-defaults are being set to follow Best Practice to keep values.yaml file
   * small
   */
  private void onlyNonDefaults(
      Map<String, Object> values,
      Version version,
      List<RuleEvaluationResultGroup> results,
      Map<String, FeelEngine.Failure> failures) {
    List<RuleEvaluationResult> defaultResults = new ArrayList<>();
    Map<String, Object> defaultValues =
        buildDefaultValues(values, "camunda", "camunda-platform", version.toString(), null);
    Map<String, Object> defaultEntries = getDefaultEntries(values, defaultValues);
    Map<ComposedKey, String> flattenedDefaultEntries = flattenMap(defaultEntries);
    flattenedDefaultEntries.forEach(
        (k, v) -> {
          if (!isMandatoryProperty(k)) {
            defaultResults.add(
                new RuleEvaluationResult(
                    k.flattened() + "_isDefault",
                    "property `" + k.flattened() + "` with value `" + v + "` has default value",
                    "The property `"
                        + k.flattened()
                        + "` has the default value and can therefore be omitted",
                    FindingLevels.INFO,
                    false,
                    new UnexpectedValue(formatPathAsYaml(k, v)),
                    Collections.emptyList()));
          } else {
            logger.debug("Ignoring '{}' as mandatory property", k.flattened());
          }
        });
    if (!defaultResults.isEmpty()) {
      results.add(
          new RuleEvaluationResultGroup(
              DEFAULT_VALUES_GROUP_ID,
              "Default values",
              "We recommend to only mention non-default values",
              defaultResults));
    }
  }

  /**
   * @param path the path to a property
   * @param value the value of the property
   * @return a yaml-formatted string
   */
  private String formatPathAsYaml(ComposedKey path, Object value) {
    final int increaseIndent = 2;
    int level = 0;
    StringBuilder result = new StringBuilder();
    for (String pathElement : path.keySequence()) {
      if (level > 0) {
        result.append("\n");
      }
      for (int i = 0; i < (increaseIndent * level); i++) {
        result.append(' ');
      }
      result.append(pathElement).append(":");
      level++;
    }
    result.append(" ").append(value).append("\n");
    return result.toString();
  }

  /** Run Rules specified in RulesInitializer */
  private void runDedicatedRules(
      Map<String, Object> values,
      Version version,
      List<RuleEvaluationResultGroup> results,
      Map<String, FeelEngine.Failure> failures) {
    reviewProperties
        .ruleGroups()
        .forEach(
            (groupId, group) -> {
              List<RuleEvaluationResult> resultsForGroup = new ArrayList<>();
              group
                  .rules()
                  .forEach(
                      (ruleName, rule) -> {
                        try {
                          if (rule.appliedVersions().stream()
                              .map(versionRangeParser::parse)
                              .anyMatch(vr -> vr.matches(version))) {
                            logger.debug("Rule '{}' is applicable", ruleName);
                            String expression = rule.expression();
                            logger.debug("Evaluating expression: {}", expression);
                            EvaluationResult result =
                                feelEngine.evaluateExpression(expression, values);
                            logger.debug("Result of evaluation: {}", result);

                            if (result.isSuccess()) {
                              // false by default to prevent false positives
                              boolean ruleFollowed = false;
                              if (result.result() instanceof Boolean resultBoolean) {
                                ruleFollowed = resultBoolean;
                              } else {
                                logger.warn(
                                    "Rule '{}' could not be evaluated, result is '{}'",
                                    ruleName,
                                    result.result());
                              }
                              resultsForGroup.add(
                                  new RuleEvaluationResult(
                                      ruleName,
                                      rule.comment(),
                                      rule.description(),
                                      rule.level(),
                                      ruleFollowed,
                                      new ExpectedValue(rule.expectedValue()),
                                      rule.links()));
                            } else {
                              final FeelEngine.Failure failure = result.failure();
                              failures.put(ruleName, failure);
                            }
                          } else {
                            logger.debug("Rule {} not applicable", ruleName);
                          }
                        } catch (Exception e) {
                          throw new RuntimeException("Error evaluating rule: " + ruleName, e);
                        }
                      });
              if (!resultsForGroup.isEmpty()) {
                results.add(
                    new RuleEvaluationResultGroup(
                        groupId, group.name(), group.description(), resultsForGroup));
              }
            });
  }

  /** Checks if values.yaml contains any non-existing properties. If yes, returns them. */
  void nonExistentProperties(
      Map<String, Object> values,
      Version version,
      List<RuleEvaluationResultGroup> results,
      Map<String, FeelEngine.Failure> failures) {
    List<RuleEvaluationResult> nonExistentProperties = new ArrayList<>();
    // load default values.yaml for passed version
    Map<String, Object> defaultValuesAsMap =
        buildDefaultValues(values, "camunda", "camunda-platform", version.toString(), null);
    Map<String, Object> missingEntries =
        NestedMapUtil.getMissingEntries(values, defaultValuesAsMap);
    Map<ComposedKey, String> flattenedMissingEntries = flattenMap(missingEntries);
    flattenedMissingEntries.forEach(
        (k, v) -> {
          if (!isWildcardProperty(k)) {
            nonExistentProperties.add(
                new RuleEvaluationResult(
                    k.flattened() + "_DoesNotExist",
                    "property `" + k.flattened() + "` with value `" + v + "` doesn't exist",
                    "The property `"
                        + k.flattened()
                        + "` does not exist in the helm chart, please check whether you need a proper replacement that is working",
                    FindingLevels.INFO,
                    false,
                    new UnexpectedValue(formatPathAsYaml(k, v)),
                    Collections.emptyList()));
          } else {
            logger.debug("Ignoring '{}' as wildcard property", k.flattened());
          }
        });
    if (!nonExistentProperties.isEmpty()) {
      results.add(
          new RuleEvaluationResultGroup(
              NON_EXISTING_VALUES_GROUP_ID,
              "Non-existing values",
              "We recommend to only use properties that actually exist",
              nonExistentProperties));
    }
  }

  private boolean isWildcardProperty(ComposedKey key) {
    for (Pattern wildcardProperty : wildcardProperties) {
      StringBuilder matchCandidate = new StringBuilder();
      for (String s : key.keySequence()) {
        matchCandidate.append(".").append(s);
        logger.debug(
            "Checking match for '{}' with '{}'",
            matchCandidate.toString(),
            wildcardProperty.pattern());
        if (wildcardProperty.matcher(matchCandidate.toString()).matches()) {
          logger.debug(
              "'{}' (matched path '{}') matches wildcard property '{}'",
              key.flattened(),
              matchCandidate.toString(),
              wildcardProperty);
          return true;
        }
      }
    }
    logger.debug("'{}' does not match any wildcard property", key);
    return false;
  }

  private boolean isMandatoryProperty(ComposedKey key) {
    for (String mandatoryProperty : reviewProperties.mandatoryProperties()) {
      if (mandatoryProperty.equals(key.flattened())) {
        logger.debug("'{}' is a mandatory property", key.flattened());
        return true;
      }
    }
    logger.debug("'{}' is not a mandatory property", key.flattened());
    return false;
  }

  /**
   * buildDefaultValue
   * @param values
   * @param repoName
   * @param chartName
   * @param version
   * @param path
   * @return
   */
  private Map<String, Object> buildDefaultValues(
      Map<String, Object> values, String repoName, String chartName, String version, String path) {
    if (path == null) {
      logger.debug("Building default values for {}/{}:{}", repoName, chartName, version);
    }

    logger.info("BuildDefaultCalues repo[{}] chartName[{}] version[{}] path[{}]", repoName, chartName, version, path);
    Map<String, Object> defaultValuesAsMap = helmService.showValues(repoName, chartName, version);
    Map<String, Object> chart = helmService.showChart(repoName, chartName, version);
    List<Map<String, Object>> dependencies = extractDependencies(chart);
    logger.info("BuildDefaultValues Extract dependencies [{}]", dependencies);

    dependencies.forEach(
        dependency -> {
          // load values
          String dependencyName = (String) dependency.get("name");
          String repository = (String) dependency.get("repository");
          if (repository.isBlank()) {
            return;
          }
          String dependencyRepoName = findRepoName(repository, repoName);
          String dependencyVersion = (String) dependency.get("version");

          // set values to alias
          String dependencyKey =
              Optional.ofNullable(dependency.get("alias"))
                  .map(o -> (String) o)
                  .orElse(dependencyName);
          String subPath = (path == null ? dependencyKey : path + "." + dependencyKey);
          logger.info("Load dependency name[{}] version[{}] repo[{}] path[{}]", dependencyKey, dependencyVersion, subPath, dependencyRepoName);
          Map<String, Object> dependencyValues =null;
          try {
            dependencyValues =
                    buildDefaultValues(
                            (Map<String, Object>) values.getOrDefault(dependencyKey, new HashMap<>()),
                            dependencyRepoName,
                            dependencyName,
                            dependencyVersion,
                            subPath);
          }catch (Exception e) {
            logger.error("Can't load dependency name[{}] version[{}] repo[{}] path[{}] : {}", dependencyKey, dependencyVersion, subPath, dependencyRepoName, e.getMessage());
            return;
          }
          logger.debug(
              "Handling dependency {} - {}/{}:{}",
              subPath,
              dependencyRepoName,
              dependencyName,
              dependencyVersion);
          Map<String, Object> valuesFromParent =
              (Map<String, Object>) defaultValuesAsMap.getOrDefault(dependencyKey, new HashMap<>());
          Map<String, Object> mergedValues =
              NestedMapUtil.merge(dependencyValues, valuesFromParent);
          defaultValuesAsMap.put(dependencyKey, mergedValues);
          // merge global
          Map<String, Object> globalFromParent = (Map<String, Object>) defaultValuesAsMap.getOrDefault("global", new HashMap<>());
          Map<String, Object> dependencyGlobals = (Map<String, Object>) dependencyValues.get("global");
          Map<String, Object> mergedGlobals = NestedMapUtil.merge(dependencyGlobals, globalFromParent);
          defaultValuesAsMap.put("global", mergedGlobals);
        });
    return defaultValuesAsMap;
  }

  private String findRepoName(String repository, String parentRepository) {
    try {
      return helmService.findRepoNameByUrl(repository).getRepoName();
    } catch (Exception e) {
      if (repository.startsWith("file:")) {
        return parentRepository;
      }
      throw e;
    }
  }

  private boolean processDependencyCondition(String condition, Map<String, Object> values) {
    return Arrays.stream(condition.split(","))
        .map(c -> feelEngine.evaluateExpression(c, values).result())
        .filter(Objects::nonNull)
        .anyMatch(o -> (boolean) o);
  }

  private List<Map<String, Object>> extractDependencies(Map<String, Object> camundaChart) {
    return (List<Map<String, Object>>) camundaChart.getOrDefault("dependencies", List.of());
  }

  private Map<ComposedKey, String> flattenMap(Map<?, ?> source) {
    Map<ComposedKey, String> converted = new HashMap<>();

    for (Map.Entry<?, ?> entry : source.entrySet()) {
      if (entry.getValue() instanceof Map) {
        flattenMap((Map<String, Object>) entry.getValue())
            .forEach(
                (key, value) ->
                    converted.put(new ComposedKey(entry.getKey().toString(), key), value));
      } else if (entry.getValue() == null) {
        converted.put(new ComposedKey(entry.getKey().toString()), null);
      } else {
        converted.put(new ComposedKey(entry.getKey().toString()), entry.getValue().toString());
      }
    }
    return converted;
  }

  public Map<String, Object> getNonDefaultValues(Map<String, Object> values, String version) {
    Map<String, Object> defaultValues =
        buildDefaultValues(values, "camunda", "camunda-platform", version.toString(), null);

    Map<String, Object> missingEntries = getMissingEntries(values, defaultValues);
    Map<String, Object> nonDefaultEntries = getNonDefaultEntries(values, defaultValues);
    return merge(missingEntries, nonDefaultEntries);
  }

  public Map<String, Long> reviewValuesAsBulk(
      String version, List<Map<String, Object>> valuesFiles) {
    List<RuleEvaluationResultGroup> ruleEvaluationResultGroups = new ArrayList<>();
    valuesFiles.forEach(
        values -> {
          List<RuleEvaluationResultGroup> singleResult = reviewValues(version, values);
          ruleEvaluationResultGroups.addAll(singleResult);
        });

    // Flatten all results and collect occurrences of ruleName
    return ruleEvaluationResultGroups.stream()
        .flatMap(group -> group.results().stream())
        .collect(Collectors.groupingBy(RuleEvaluationResult::ruleName, Collectors.counting()));
  }

  private record ComposedKey(List<String> keySequence) {
    ComposedKey(String key) {
      this(List.of(key));
    }

    ComposedKey(String prefix, ComposedKey key) {
      this(merge(prefix, key));
    }

    public String flattened() {
      return String.join(".", keySequence);
    }

    private static List<String> merge(String prefix, ComposedKey key) {
      List<String> merged = new ArrayList<>();
      merged.add(prefix);
      merged.addAll(key.keySequence());
      return merged;
    }
  }
}
