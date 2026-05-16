package io.camunda.kiwi.reviewer.helm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.marcnuri.helm.Helm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class HelmService {
  private static final Logger logger = LoggerFactory.getLogger(HelmService.class);

  private final HelmRepoFactory helmRepoFactory;
  private final ObjectMapper mapper;
  private final Map<String, Map<String, Map<String, Map<String, Object>>>> valuesCache =
      new HashMap<>();
  private final Map<String, Map<String, Map<String, Map<String, Object>>>> chartCache =
      new HashMap<>();

  public HelmService( HelmRepoFactory helmRepoFactory) {
    this.mapper = new YAMLMapper(new YAMLFactory());
    this.helmRepoFactory = helmRepoFactory;
  }


  public HelmRepoFactory.HelmRepo getHelmRepo(String repoName) {
    return helmRepoFactory.findHelmRepoByName( repoName, true);
  }



  public void injectValues(
      String repoName, String chartName, String version, Map<String, Object> values) {
    synchronized (valuesCache) {
      valuesCache
          .computeIfAbsent(repoName, k -> new HashMap<>())
          .computeIfAbsent(chartName, k -> new HashMap<>())
          .computeIfAbsent(version, k -> mapper.convertValue(values, new TypeReference<>() {}));
    }
  }

  public void injectChart(
      String repoName, String chartName, String version, Map<String, Object> chart) {
    synchronized (chartCache) {
      chartCache
          .computeIfAbsent(repoName, k -> new HashMap<>())
          .computeIfAbsent(chartName, k -> new HashMap<>())
          .computeIfAbsent(version, k -> mapper.convertValue(chart, new TypeReference<>() {}));
    }
  }

  public HelmRepoFactory.HelmRepo findRepoNameByUrl(String repoUrl) {
    return helmRepoFactory.findHelmRepoByUrl(repoUrl, true);
  }

  private boolean equals(String stringUrl, URI url) {
    return stringUrl.equals(url.toString());
  }

  public Map<String, Object> showValues(String repoName, String chartName, String version) {
    HelmRepoFactory.HelmRepo helmRepo = helmRepoFactory.findHelmRepoByName(repoName, true);




    synchronized (valuesCache) {
      Map<String, Object> values= mapper.convertValue(
          valuesCache
              .computeIfAbsent(repoName, k -> new HashMap<>())
              .computeIfAbsent(chartName, k -> new HashMap<>())
              .computeIfAbsent(
                  version,
                  k -> {
                    try {
                      return mapper.readValue(
                          Helm.show(helmRepo.getRepoName() + "/" + chartName)
                              .values()
                              .withVersion(version)
                              .call(),
                          new TypeReference<>() {});
                    } catch (JsonProcessingException e) {
                      logger.error("Error while reading values for {}", repoName, e);
                      throw new RuntimeException("Error while parsing default values", e);
                    }
                  }),
          new TypeReference<>() {});
      logger.info("HelmService.getValues repo[{}] version[{}]  values: {}", repoName, version, values.size());
      return values;
    }
  }

  public Map<String, Object> showChart(String repoName, String chartName, String version) {
    HelmRepoFactory.HelmRepo helmRepo = helmRepoFactory.findHelmRepoByName(repoName, true);
    synchronized (chartCache) {
      Map<String, Object> values= mapper.convertValue(
          chartCache
              .computeIfAbsent(repoName, k -> new HashMap<>())
              .computeIfAbsent(chartName, k -> new HashMap<>())
              .computeIfAbsent(
                  version,
                  k -> {
                    try {
                      return mapper.readValue(
                          Helm.show(helmRepo.getRepoName() + "/" + chartName)
                              .chart()
                              .withVersion(version)
                              .call(),
                          new TypeReference<>() {});
                    } catch (JsonProcessingException e) {
                      throw new RuntimeException("Error while parsing default values", e);
                    }
                  }),
          new TypeReference<>() {});
      logger.info("getChart repo[{}] version[{}]  Description[{}]", repoName, version, values.get("description"));
      return values;
    }
  }






}
