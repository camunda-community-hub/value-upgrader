package io.camunda.kiwi.reviewer.helm;

import java.net.URI;
import java.util.List;

public record HelmRepoProperties(URI url, String repoName, List<URI> alternativeRepoNames) {
  public static final HelmRepoProperties CAMUNDA =
      new HelmRepoProperties(URI.create("https://helm.camunda.io"), "camunda", List.of());
}
