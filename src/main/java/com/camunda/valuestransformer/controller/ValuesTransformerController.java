package com.camunda.valuestransformer.controller;

import com.camunda.valuestransformer.model.MigrateRequest;
import com.camunda.valuestransformer.model.MigrateResponse;
import com.camunda.valuestransformer.rule.RuleFactory;
import com.camunda.valuestransformer.service.TransformEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for the values transformation API.
 * <p>
 * Replaces Go's CLI interface (cobra command in cmd/root.go).
 * Instead of --rules, --input, --output, --old-defaults flags,
 * the caller posts JSON with the YAML content as strings.
 */
@RestController
@RequestMapping("/upgraded/api/v1")
public class ValuesTransformerController {

    Logger logger = LoggerFactory.getLogger(ValuesTransformerController.class.getName());
    @Autowired
    RuleFactory ruleFactory;

    private final TransformEngine engine;

    public ValuesTransformerController(TransformEngine engine) {
        this.engine = engine;
    }

    /**
     * Applies transformation rules to a values.yaml and returns the result.
     * <p>
     * Equivalent to Go's CLI:
     * values-transform migrate --rules rules.yaml --input values.yaml [--old-defaults defaults.yaml] [--dry-run]
     * <p>
     * POST /api/v1/migrate
     * Content-Type: application/json
     * <p>
     * Body:
     * {
     * "rulesYaml":       "version: 1\nrules:\n  ...",
     * "inputYaml":       "image:\n  tag: latest\n...",
     * "oldDefaultsYaml": "image:\n  tag: stable\n...",  // optional
     * "dryRun": false
     * }
     * <p>
     * Response (200):
     * {
     * "outputYaml": "...",  // null if dryRun=true
     * "report":    "=== Transformation Report ===\n...",
     * "summary":   "2 changes, 0 warnings, 1 skipped, 0 errors",
     * "hasErrors": false
     * }
     * <p>
     * Response (400): if rules or input YAML are invalid.
     * Response (422): if transformation completed but with errors.
     */
    @PostMapping("/migrate")
    public ResponseEntity<MigrateResponse> migrate(@RequestBody MigrateRequest request) {
        if (request.getInputYaml() == null || request.getInputYaml().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String rule = ruleFactory.getRule(RuleFactory.VERSION.V87_88);
        TransformEngine.EngineResult result = engine.run(
                rule,
                request.getInputYaml(),
                request.getOldDefaultsYaml(),
                false
        );

        MigrateResponse response = new MigrateResponse();
        response.setOutputYaml(result.outputYaml());
        response.setReportString(result.report().print());
        response.setSummary(result.report().summary());
        response.setHasErrors(result.report().hasErrors());

        // If transformation had errors, return 422 Unprocessable Entity
        // (equivalent to Go returning a non-nil error with os.Exit(1))
        if (result.report().hasErrors()) {
            return ResponseEntity.unprocessableEntity().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/migratefile", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MigrateResponse> migratefile(@RequestPart("File") List<MultipartFile> uploadedfiles) {

        logger.info("migratefile start");
        String valuesToTransform = toString(uploadedfiles.get(0));

        String rules = ruleFactory.getRule(RuleFactory.VERSION.V87_88);

        TransformEngine.EngineResult result = engine.run(
                rules,
                valuesToTransform,
                null,
                false
        );

        MigrateResponse response = new MigrateResponse();
        response.setOutputYaml(result.outputYaml());
        response.setReportEntries(result.report().getEntries());
        response.setSummary(result.report().summary());
        response.setHasErrors(result.report().hasErrors());

        // If transformation had errors, return 422 Unprocessable Entity
        // (equivalent to Go returning a non-nil error with os.Exit(1))
        if (result.report().hasErrors()) {
            return ResponseEntity.unprocessableEntity().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Validates a rules YAML file without applying it.
     * <p>
     * POST /api/v1/validate-rules
     * Content-Type: text/plain (raw YAML)
     */
    @PostMapping(value = "/validate-rules", consumes = "text/plain")
    public ResponseEntity<String> validateRules(@RequestBody String rulesYaml) {
        try {
            engine.run(rulesYaml, "{}", null, true);
            return ResponseEntity.ok("Rules file is valid.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid rules: " + e.getMessage());
        }
    }


    private String toString(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            return reader.lines().collect(Collectors.joining("\n"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to read file " + file.getOriginalFilename(), e);
        }
    }
}
