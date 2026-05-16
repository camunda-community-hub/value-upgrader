package io.camunda.kiwi.reviewer;

import java.util.Map;

public record TxReviewRuleGroup(String name, String description, Map<String, TxReviewRule> rules) {}
