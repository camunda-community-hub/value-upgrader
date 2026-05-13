package io.camunda.valuestransformer.controller;

import io.camunda.valuestransformer.rule.RuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rule/api/v1")

public class RuleController {
    Logger logger = LoggerFactory.getLogger(RuleController.class.getName());

    private final RuleFactory ruleFactory;

    public RuleController(RuleFactory ruleFactor) {
        this.ruleFactory = ruleFactor;
    }

    @GetMapping("/content")
    public ResponseEntity<String> migrate(@RequestParam(name = "version", required = true) String version) {
        RuleFactory.VERSION versionToTransform;
        try {
            versionToTransform = RuleFactory.VERSION.valueOf(version);
        } catch (Exception e) {
            logger.error("Can't convert [{}] to VERSION. [{},{}] expected", version,
                    RuleFactory.VERSION.V87_88.toString(),
                    RuleFactory.VERSION.V88_89.toString());
            return ResponseEntity.badRequest().build();

        }
        String rule = ruleFactory.getRule(versionToTransform);
        return ResponseEntity.ok(rule);
    }

}
