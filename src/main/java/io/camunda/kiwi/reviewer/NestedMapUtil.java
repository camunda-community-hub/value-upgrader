package io.camunda.kiwi.reviewer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public class NestedMapUtil {

  public static Map<String, Object> getMissingEntries(
      Map<String, Object> providedValues, Map<String, Object> defaultValues) {
    Map<String, Object> missingEntries = new HashMap<>();
    findMissingEntries(providedValues, defaultValues, missingEntries);
    return missingEntries;
  }

  public static Map<String, Object> getDefaultEntries(
      Map<String, Object> providedValues, Map<String, Object> defaultValues) {
    Map<String, Object> defaultEntries = new HashMap<>();
    findDefaultNonDefaultEntries(
        providedValues, defaultValues, defaultEntries, (a, b) -> areInputsEqual(a, b));
    return defaultEntries;
  }

  public static Map<String, Object> getNonDefaultEntries(
      Map<String, Object> providedValues, Map<String, Object> defaultValues) {
    Map<String, Object> defaultEntries = new HashMap<>();
    findDefaultNonDefaultEntries(
        providedValues, defaultValues, defaultEntries, (a, b) -> !areInputsEqual(a, b));
    return defaultEntries;
  }

  static boolean areInputsEqual(Object input1, Object input2) {
    // Check if both inputs are null
    if (input1 == null && input2 == null) {
      return true;
    }

    // Check if either input is null
    if (input1 == null || input2 == null) {
      return false;
    }

    // Check if both inputs are numbers
    if (input1 instanceof Number && input2 instanceof Number) {
      // Compare as numbers
      return ((Number) input1).doubleValue() == ((Number) input2).doubleValue();
    }

    // If either input is a string, attempt to parse as numbers
    try {
      Double num1 = input1 instanceof String ? Double.parseDouble((String) input1) : null;
      Double num2 = input2 instanceof String ? Double.parseDouble((String) input2) : null;
      if (num1 != null && num2 != null) {
        return num1.equals(num2);
      }
    } catch (NumberFormatException e) {
      // Ignore, continue to string comparison
    }

    // Compare as strings (case-sensitive)
    return input1.toString().equals(input2.toString());
  }

  private static void findDefaultNonDefaultEntries(
      Map<String, Object> providedValues,
      Map<String, Object> defaultValues,
      Map<String, Object> defaultEntries,
      BiPredicate<Object, Object> condition) {
    for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
      if (providedValues.containsKey(entry.getKey())) {
        Object providedValue = providedValues.get(entry.getKey());
        Object defaultValue = defaultValues.get(entry.getKey());
        if (providedValue instanceof Map && defaultValue instanceof Map) {
          Map<String, Object> subDefaultEntries = new HashMap<>();
          findDefaultNonDefaultEntries(
              (Map<String, Object>) providedValue,
              (Map<String, Object>) defaultValue,
              subDefaultEntries,
              condition);
          if (!subDefaultEntries.isEmpty()) {
            defaultEntries.put(entry.getKey(), subDefaultEntries);
          }
        } else if (providedValue instanceof List && defaultValue instanceof List) {
          // do nothing, lists cannot be compared like this
        } else if (condition.test(providedValue, defaultValue)) {
          defaultEntries.put(entry.getKey(), providedValue);
        }
      }
    }
  }

  /**
   * Merges all maps, last map overrides
   *
   * @param maps
   * @return the merged map
   */
  @SafeVarargs
  public static Map<String, Object> merge(Map<String, Object>... maps) {
    ObjectMapper mapper = new ObjectMapper();
    // this is required as helm cannot merge lists as well
    mapper.configOverride(ArrayNode.class).setMergeable(false);
    ObjectNode merged = JsonNodeFactory.instance.objectNode();
    for (Map<String, Object> map : maps) {
      ObjectReader objectReader = mapper.readerForUpdating(merged);
      JsonNode jsonNode = mapper.valueToTree(map);
      try {
        merged = objectReader.readValue(jsonNode);
      } catch (IOException e) {
        throw new RuntimeException(
            "Error while merging values from " + jsonNode + " into " + merged, e);
      }
    }
    return mapper.convertValue(merged, new TypeReference<>() {});
  }

  private static void findMissingEntries(
      Map<String, Object> providedValues,
      Map<String, Object> defaultValues,
      Map<String, Object> missingEntries) {
    for (String key : providedValues.keySet()) {
      if (!defaultValues.containsKey(key)) {
        // Key not present in default values, add to missing entries
        missingEntries.put(key, providedValues.get(key));
      } else {
        Object providedValue = providedValues.get(key);
        Object defaultValue = defaultValues.get(key);

        if (providedValue instanceof Map && defaultValue instanceof Map) {
          // Both values are maps, recursively find the missing entries
          @SuppressWarnings("unchecked")
          Map<String, Object> providedValueMap = (Map<String, Object>) providedValue;
          @SuppressWarnings("unchecked")
          Map<String, Object> defaultValueMap = (Map<String, Object>) defaultValue;
          if (!defaultValueMap.isEmpty()) {
            Map<String, Object> subMissingEntries = new HashMap<>();
            findMissingEntries(providedValueMap, defaultValueMap, subMissingEntries);
            if (!subMissingEntries.isEmpty()) {
              missingEntries.put(key, subMissingEntries);
            }
          }
        }
      }
    }
  }
}
