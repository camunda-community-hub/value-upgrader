package io.camunda.kiwi.reviewer.rule;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class RuleReviewFactory {


    public enum VERSION {
        V82,
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
            case V82 -> "camunda-review-82-rules.yaml";
            case V88_89 -> "camunda-review-88-rules.yaml";
        };
    }
}


