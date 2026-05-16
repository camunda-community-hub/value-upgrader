package io.camunda.kiwi.reviewer;

import io.camunda.kiwi.reviewer.VersionRange.*;
import com.fasterxml.jackson.core.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VersionRangeParser {
  private static final Logger LOG = LoggerFactory.getLogger(VersionRangeParser.class);
  private static final Pattern OPERATOR_GROUP = Pattern.compile("(=?[<>]=?)");
  private static final Map<String, Function<Version, VersionRange>> OPERATOR_MAPPINGS;

  static {
    OPERATOR_MAPPINGS = new HashMap<>();
    OPERATOR_MAPPINGS.put(">", Greater::new);
    OPERATOR_MAPPINGS.put(">=", GreaterEquals::new);
    OPERATOR_MAPPINGS.put("=>", GreaterEquals::new);
    OPERATOR_MAPPINGS.put("<", Lower::new);
    OPERATOR_MAPPINGS.put("<=", LowerEquals::new);
    OPERATOR_MAPPINGS.put("=<", LowerEquals::new);
  }

  public VersionRange parse(String version) {
    if (version == null || version.trim().isEmpty()) {
      throw new IllegalArgumentException("version cannot be null or empty");
    }
    version = version.trim();
    // handle wildcard
    if (version.equals("*")) {
      LOG.debug("Is Wildcard: {}", version);
      return new Wildcard();
    }
    if (hasOperator(version)) {
      return applyOperator(version);
    } else {
      LOG.debug("Is Equals: {}", version);
      return applyEquals(version);
    }
  }

  private VersionRange applyOperator(String version) {
    // operator may be 1 or 2 signs and may or may not be separated by space
    Matcher matcher = OPERATOR_GROUP.matcher(version);
    if (matcher.find()) {
      String operator = matcher.group(0);
      Function<Version, VersionRange> versionRangeProvider =
          OPERATOR_MAPPINGS.getOrDefault(
              operator,
              v -> {
                throw new IllegalStateException("Unrecognized operator +" + operator);
              });
      VersionRange versionRange =
          versionRangeProvider.apply(parseVersion(version.substring(operator.length()).trim()));
      LOG.debug("Is {}: {}", versionRange.getClass().getSimpleName(), version);
      return versionRange;
    }
    throw new IllegalArgumentException("No operator detected: " + version);
  }

  private boolean hasOperator(String version) {
    return OPERATOR_GROUP.matcher(version).find();
  }

  private Equals applyEquals(String versionString) {
    return new Equals(parseVersion(versionString));
  }

  private Version parseVersion(String versionString) {
    List<Integer> versionsList =
        Arrays.stream(versionString.split("\\.")).map(String::trim).map(Integer::parseInt).toList();
    int major = versionsList.get(0);
    int minor = versionsList.size() > 1 ? versionsList.get(1) : 0;
    int patch = versionsList.size() > 2 ? versionsList.get(2) : 0;
    return new Version(major, minor, patch, null, null, null);
  }
}
