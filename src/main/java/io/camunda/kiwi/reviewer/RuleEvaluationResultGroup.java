package io.camunda.kiwi.reviewer;

import java.util.List;

public record RuleEvaluationResultGroup(
    String id, String name, String description, List<RuleEvaluationResult> results) {}
