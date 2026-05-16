package io.camunda.kiwi.reviewer.controller;

import io.camunda.kiwi.reviewer.ReviewService;
import io.camunda.kiwi.upgrader.rule.RuleUpgradeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/matrix/api/v1")
public class MatrixController {

    private static final Logger logger = LoggerFactory.getLogger(MatrixController.class);

    public MatrixController(ReviewService reviewService) {
    }

    @GetMapping("/versions")
    public ResponseEntity<List<String>> matrix() {
        RuleUpgradeFactory.VERSION versionToTransform;

        try {
            List<String> listVersions= fetchHelmChartVersions();

            listVersions = listVersions.stream()
                    .filter(version -> ! version.contains("alpha"))
                    .toList();
            logger.info("Matrix: " + listVersions);
            return ResponseEntity.ok(listVersions);

        } catch (Exception e) {
            logger.error("Can't get Matrix",
                    RuleUpgradeFactory.VERSION.V87_88.toString(),
                    RuleUpgradeFactory.VERSION.V88_89.toString());
            return ResponseEntity.badRequest().build();

        }

    }


    private static final String VERSION_MATRIX_URL =
            "https://helm.camunda.io/camunda-platform/version-matrix/";

    /**
     * Récupère toutes les versions Helm chart depuis la page de version matrix Camunda.
     * Exemples de résultats : "15.0.0-alpha1", "14.2.0", "13.8.0", "9.3.4", "8.0.1", ...
     */
    public static List<String> fetchHelmChartVersions() throws IOException, InterruptedException {

        // ── 1. Appel HTTP ────────────────────────────────────────────────────
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VERSION_MATRIX_URL))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP error: " + response.statusCode());
        }

        String html = response.body();

        // ── 2. Extraction des versions par regex ─────────────────────────────
        // La page contient des liens de la forme :
        //   ### [Helm chart 14.2.0](https://helm.camunda.io/.../camunda-8.9/#helm-chart-1420)
        // On capture la version après "Helm chart "
        //
        // Pattern : "Helm chart " suivi d'un numéro de version semver (avec suffixe optionnel)
        Pattern pattern = Pattern.compile(
                "Helm chart\\s+([0-9]+\\.[0-9]+\\.[0-9]+(?:-[a-zA-Z0-9.]+)?)"
        );

        Matcher matcher = pattern.matcher(html);
        List<String> versions = new ArrayList<>();

        while (matcher.find()) {
            versions.add(matcher.group(1));
        }

        return versions;
    }
}
