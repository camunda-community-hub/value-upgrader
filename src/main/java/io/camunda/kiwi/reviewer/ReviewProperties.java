package io.camunda.kiwi.reviewer;

import io.camunda.kiwi.reviewer.helm.HelmRepoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties("review")
public record ReviewProperties(
    Map<String, TxReviewRuleGroup> ruleGroups,
    List<HelmRepoProperties> helmRepos,
    List<String> wildcardProperties,
    List<String> mandatoryProperties) {};




