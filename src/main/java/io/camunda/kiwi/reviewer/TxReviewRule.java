package io.camunda.kiwi.reviewer;

import java.util.List;

public record TxReviewRule(
    String expression,
    String comment,
    String description,
    FindingLevels level,
    List<String> appliedVersions,
    List<String> links,
    String expectedValue) {}
