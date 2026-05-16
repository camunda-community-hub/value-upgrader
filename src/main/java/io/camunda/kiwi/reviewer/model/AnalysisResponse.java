package io.camunda.kiwi.reviewer.model;

import io.camunda.kiwi.reviewer.RuleEvaluationResultGroup;

import java.util.ArrayList;
import java.util.List;

public class AnalysisResponse {
    /** The transformed YAML content. Null when dryRun=true. */

    public List<RuleEvaluationResultGroup> rulesEvaluation;

}
