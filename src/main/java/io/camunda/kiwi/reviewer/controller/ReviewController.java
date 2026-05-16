package io.camunda.kiwi.reviewer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.camunda.kiwi.reviewer.ReviewService;
import io.camunda.kiwi.reviewer.model.AnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/review/api/v1")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final YAMLMapper yamlMapper;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
        this.yamlMapper = new YAMLMapper();
    }

    @GetMapping
    public String test() {
        return "successful";
    }

    @PostMapping(
            value = "/analysis",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> reviewValues(
            @RequestParam(name = "version", required = true) String version,
            @RequestPart("File") List<MultipartFile> uploadedfiles,

            @RequestHeader(HttpHeaders.ACCEPT) String accept) {

        String valuesToAnalyse = loadFileToString(uploadedfiles.get(0));

        logger.info("analysing version[{}]", version);
        try {
            Map<String, Object> valuesMap =
                    yamlMapper.readValue(valuesToAnalyse, new TypeReference<>() {
                    });
            AnalysisResponse response = new AnalysisResponse();
            response.rulesEvaluation = reviewService.reviewValues(version, valuesMap);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("reviewValues version [{}] :", version, e);
            return ResponseEntity.badRequest().build();
        }

    }

    @PostMapping(
            value = "/non-defaults/{version}",
            consumes = "application/x-yaml",
            produces = {"application/x-yaml"})
    public ResponseEntity<?> getNonDefaultValues(
            @PathVariable("version") String version,
            @RequestBody Map<String, Object> values) { // TODO varargs?

        logger.debug(
                "Getting non-default values for version ={} with provided values={}", version, values);

        return ResponseEntity.ok(reviewService.getNonDefaultValues(values, version));
    }

    @PostMapping(
            value = "/bulk-analysis/{version}",
            consumes = "multipart/form-data",
            produces = {"application/json"})
    public ResponseEntity<?> reviewBulkValues(
            @PathVariable("version") String version,
            @RequestParam("files") List<MultipartFile> files)
            throws IOException {

        List<Map<String, Object>> valuesMapsAsList = new ArrayList<>();
        for (MultipartFile file : files) {
            String contentAsString = new String(file.getBytes());
            Map<String, Object> valuesMap =
                    yamlMapper.readValue(contentAsString, new TypeReference<>() {
                    });
            valuesMapsAsList.add(valuesMap);
        }

        int numberOfValuesFiles = files.size();

        logger.info(
                "Bulk analysis for Charts of version ={} with {} values files",
                version,
                numberOfValuesFiles);
        return ResponseEntity.ok(reviewService.reviewValuesAsBulk(version, valuesMapsAsList));
    }


    private String loadFileToString(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            return reader.lines().collect(Collectors.joining("\n"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to read file " + file.getOriginalFilename(), e);
        }
    }
}
