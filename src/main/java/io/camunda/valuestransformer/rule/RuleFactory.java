package io.camunda.valuestransformer.rule;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@Component
public class RuleFactory {

    public enum VERSION {
        V87_88,
        V88_89
    }
    public String getRule(VERSION version) {
        String fileName = resolveFileName(version);

        try (InputStream is = new ClassPathResource(fileName).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load rules file: " + fileName, e);
        }
    }

    private String resolveFileName(VERSION version) {
        return switch (version) {
            case V87_88 -> "camunda-87-to-88-rules.yaml";
            case V88_89 -> "camunda-88-to-89-rules.yaml";
        };
    }
}
