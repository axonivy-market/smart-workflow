package com.axonivy.utils.ai.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringProcessingUtils {
  // Consolidated delimiter patterns for JSON extraction
  private static final Pattern[] DELIMITER_PATTERNS = new Pattern[] { Pattern.compile("<<(.*?)>>>", Pattern.DOTALL), // <<
                                                                                                                     // ...
                                                                                                                     // >>>
      Pattern.compile("<<<(.*?)>>", Pattern.DOTALL), // <<< ... >>
      Pattern.compile("<<(.*?)>>", Pattern.DOTALL), // << ... >>
      Pattern.compile("<<(.*?)>", Pattern.DOTALL), // << ... >
      Pattern.compile("<(.*?)>>", Pattern.DOTALL), // < ... >>
      Pattern.compile("<<\\s*(\\{.*?\\})\\s*>>", Pattern.DOTALL), // JSON inside << { ... } >>
      Pattern.compile("<<\\s*(\\[.*?\\])\\s*>>", Pattern.DOTALL), // JSON Array inside << [ ... ] >>
      Pattern.compile("<<(.*)", Pattern.DOTALL), // << ...
      Pattern.compile("(.*)>>", Pattern.DOTALL) // ... >>
  };

  // Markdown extraction patterns
  private static final Pattern JSON_MD_PATTERN = Pattern.compile("```json\\s*(\\{.*?\\}|\\[.*?\\])\\s*```",
      Pattern.DOTALL);

  private static final Pattern CODE_MD_PATTERN = Pattern.compile("```\\s*(.*?)\\s*```", Pattern.DOTALL);

  // Constants for double brackets adjustments
  private static final String DOUBLE_BRACKETS_START = "[[";
  private static final String DOUBLE_BRACKETS_END = "]]";
  private static final String DOUBLE_BRACKETS_START_2 = "{{";
  private static final String DOUBLE_BRACKETS_END_2 = "}}";

  /**
   * Standardizes the result by extracting JSON content from the provided text.
   * 
   * @param result The raw result containing JSON.
   * @return A standardized JSON string, or the original text if no extraction
   *         occurred.
   */
  public static String standardizeResult(String result) {
    String json = extractJson(result);
    return preProcessJson(json.strip());
  }

  /**
   * Check if number of open-close brackets are same
   * 
   * @param text
   * @return
   */
  private static int countBracketBalance(String text) {
    int count = 0;
    for (char c : text.toCharArray()) {
      if (c == '{')
        count++;
      else if (c == '}')
        count--;
    }
    return count; // 0 = balanced, >0 = more {, <0 = more }
  }

  /**
   * Iterates over the predefined patterns to extract JSON content.
   * 
   * @param text The text to search.
   * @return Extracted JSON string or the original text if no match is found.
   */
  private static String extractJson(String text) {
    for (Pattern pattern : DELIMITER_PATTERNS) {
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) {
        return matcher.group(1).strip();
      }
    }
    return text;
  }

  /**
   * Processes markdown code blocks and extra bracket adjustments.
   * 
   * @param json The JSON string to process.
   * @return A cleaned-up JSON string.
   */
  private static String preProcessJson(String json) {
    json = processMarkdown(json);

    // Adjust double brackets uniformly
    if (json.startsWith(DOUBLE_BRACKETS_START)) {
      json = json.substring(DOUBLE_BRACKETS_START.length() - 1);
    }
    if (json.endsWith(DOUBLE_BRACKETS_END)) {
      json = json.substring(0, json.length() - (DOUBLE_BRACKETS_END.length()));
    }
    if (json.startsWith(DOUBLE_BRACKETS_START_2) && countBracketBalance(json) == 1) {
      json = json.substring(DOUBLE_BRACKETS_START_2.length() - 1);
    }
    if (json.endsWith(DOUBLE_BRACKETS_END_2) && countBracketBalance(json) == -1) {
      json = json.substring(0, json.length() - (DOUBLE_BRACKETS_END_2.length()));
    }

    // Correct mismatched array/object delimiters if needed
    if (json.startsWith("{") && json.endsWith("]")) {
      json = "[" + json;
    }
    if (json.startsWith("[") && json.endsWith("}")) {
      json = json + "]";
    }

    return json;
  }

  /**
   * Extracts content from markdown code blocks if present.
   * 
   * @param text The text possibly containing markdown code blocks.
   * @return The extracted content if found; otherwise, the original text.
   */
  private static String processMarkdown(String text) {
    Matcher matcher = JSON_MD_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(1).strip();
    }
    matcher = CODE_MD_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(1).strip();
    }
    return text.strip();
  }

  /**
   * Determines if the provided JSON string is a JSON array.
   * 
   * @param json The JSON string.
   * @return True if the string represents a JSON array; false otherwise.
   */
  public static boolean isArray(String json) {
    return json.startsWith("[") && json.endsWith("]");
  }
}