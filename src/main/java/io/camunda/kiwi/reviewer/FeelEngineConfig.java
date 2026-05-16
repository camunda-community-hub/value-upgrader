package io.camunda.kiwi.reviewer;

import org.camunda.feel.FeelEngine;
import org.camunda.feel.api.FeelEngineApi;
import org.camunda.feel.impl.SpiServiceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeelEngineConfig {

  @Bean
  public FeelEngineApi feelEngine() {
    return new FeelEngineApi(
        new FeelEngine(
            SpiServiceLoader.loadFunctionProvider(),
            SpiServiceLoader.loadValueMapper(),
            FeelEngine.defaultConfiguration(),
            FeelEngine.defaultClock()));
  }
}
