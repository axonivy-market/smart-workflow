package com.axonivy.utils.ai.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringProcessingUtils {
  // Consolidated delimiter patterns for JSON extraction
  private static final Pattern[] DELIMITER_PATTERNS = new Pattern[] { Pattern.compile("<<(.*?)>>>", Pattern.DOTALL),
      Pattern.compile("<<<(.*?)>>", Pattern.DOTALL), Pattern.compile("<<(.*?)>>", Pattern.DOTALL),
      Pattern.compile("<<(.*?)>", Pattern.DOTALL), Pattern.compile("<(.*?)>>", Pattern.DOTALL),
      Pattern.compile("<<\\s*(\\{.*?\\})\\s*>>", Pattern.DOTALL),
      Pattern.compile("<<\\s*(\\[.*?\\])\\s*>>", Pattern.DOTALL), Pattern.compile("<<(.*)", Pattern.DOTALL),
      Pattern.compile("(.*)>>", Pattern.DOTALL)
  };

  // Markdown extraction patterns
  private static final Pattern JSON_MD_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern CODE_MD_PATTERN = Pattern.compile("```\\s*([\\s\\S]*?)\\s*```", Pattern.DOTALL);

  // Constants for double brackets adjustments
  private static final String DOUBLE_BRACKETS_START = "[[";
  private static final String DOUBLE_BRACKETS_END = "]]";
  private static final String DOUBLE_BRACKETS_START_2 = "{{";
  private static final String DOUBLE_BRACKETS_END_2 = "}}";

  private StringProcessingUtils() {
    // utility class
  }

  /**
   * Standardizes the result by extracting JSON content from the provided text.
   *
   * @param result     The raw result containing JSON.
   * @param useWrapper If true, use custom delimiters like << >>.
   * @return A standardized JSON string, or the original text if no extraction
   *         occurred.
   */
  public static String standardizeResult(String result, boolean useWrapper) {
    String json = useWrapper ? extractJson(result) : extractJsonWithoutDelimiters(result);
    return preProcessJson(json.strip());
  }

  /**
   * Check if number of open-close brackets are same
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
   * Extracts JSON using wrapper delimiters and markdown code blocks.
   */
  private static String extractJson(String text) {
    String result = text;

    // FIRST: Try markdown JSON block
    Matcher mdJsonMatcher = JSON_MD_PATTERN.matcher(result);
    if (mdJsonMatcher.find()) {
      return mdJsonMatcher.group(1).strip();
    }

    // THEN: Try delimiter patterns
    for (Pattern pattern : DELIMITER_PATTERNS) {
      Matcher matcher = pattern.matcher(result);
      if (matcher.find()) {
        return matcher.group(1).strip();
      }
    }

    return result;
  }

  /**
   * Extracts JSON content from markdown blocks or raw arrays/objects only,
   * without using custom delimiters like << >>.
   */
  public static String extractJsonWithoutDelimiters(String text) {
    String result = text.strip();

    // Try markdown ```json``` block
    Matcher mdJsonMatcher = JSON_MD_PATTERN.matcher(result);
    if (mdJsonMatcher.find()) {
      return mdJsonMatcher.group(1).strip();
    }

    // Fallback: general ```...``` code block
    Matcher codeBlockMatcher = CODE_MD_PATTERN.matcher(result);
    if (codeBlockMatcher.find()) {
      String candidate = codeBlockMatcher.group(1).strip();
      if (isLikelyJson(candidate)) {
        return candidate;
      }
    }

    // Lastly: raw JSON array or object
    if (isLikelyJson(result)) {
      return result;
    }

    return result;
  }

  /**
   * Checks if the given text looks like JSON.
   */
  private static boolean isLikelyJson(String text) {
    text = text.strip();
    return (text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"));
  }

  /**
   * Processes markdown code blocks and extra bracket adjustments.
   */
  private static String preProcessJson(String json) {
    json = processMarkdown(json);

    // Adjust double brackets
    if (json.startsWith(DOUBLE_BRACKETS_START)) {
      json = json.substring(DOUBLE_BRACKETS_START.length() - 1);
    }
    if (json.endsWith(DOUBLE_BRACKETS_END)) {
      json = json.substring(0, json.length() - DOUBLE_BRACKETS_END.length());
    }
    if (json.startsWith(DOUBLE_BRACKETS_START_2) && countBracketBalance(json) == 1) {
      json = json.substring(DOUBLE_BRACKETS_START_2.length() - 1);
    }
    if (json.endsWith(DOUBLE_BRACKETS_END_2) && countBracketBalance(json) == -1) {
      json = json.substring(0, json.length() - DOUBLE_BRACKETS_END_2.length());
    }

    // Correct common mismatches
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
   */
  public static boolean isArray(String json) {
    return json.startsWith("[") && json.endsWith("]");
  }
}
