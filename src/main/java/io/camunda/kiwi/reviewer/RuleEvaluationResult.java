package io.camunda.kiwi.reviewer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import java.util.List;

public record RuleEvaluationResult(
    String ruleName,
    String comment,
    String description,
    FindingLevels level,
    boolean followed,
    Details details,
    List<String> links) {

  @JsonSubTypes({
    @Type(value = Details.ExpectedValue.class, name = "expected value"),
    @Type(value = Details.UnexpectedValue.class, name = "unexpected value")
  })
  public sealed interface Details {
    record ExpectedValue(String expectedValue) implements Details {}

    record UnexpectedValue(String unexpectedValue) implements Details {}
  }
}
