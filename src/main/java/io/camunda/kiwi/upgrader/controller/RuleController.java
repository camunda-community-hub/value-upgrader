package io.camunda.kiwi.upgrader.controller;

import io.camunda.kiwi.upgrader.rule.RuleUpgradeFactory;
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

    private final RuleUpgradeFactory ruleUpgradeFactory;

    public RuleController(RuleUpgradeFactory ruleFactor) {
        this.ruleUpgradeFactory = ruleFactor;
    }

    @GetMapping("/content")
    public ResponseEntity<String> migrate(@RequestParam(name = "version", required = true) String version) {
        RuleUpgradeFactory.VERSION versionToTransform;
        try {
            versionToTransform = RuleUpgradeFactory.VERSION.valueOf(version);
        } catch (Exception e) {
            logger.error("Can't convert [{}] to VERSION. [{},{}] expected", version,
                    RuleUpgradeFactory.VERSION.V87_88.toString(),
                    RuleUpgradeFactory.VERSION.V88_89.toString());
            return ResponseEntity.badRequest().build();

        }
        String rule = ruleUpgradeFactory.getRule(versionToTransform);
        return ResponseEntity.ok(rule);
    }

}
