package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MaskingRule {
  String mask(String text, Map<String, String> replacements);

  static MaskingRule of(String typeName, String pattern) {
    Pattern compiled = Pattern.compile(pattern);
    return (text, replacements) -> apply(text, typeName, compiled, replacements, _ -> true);
  }

  static MaskingRule of(String typeName, String pattern, Predicate<String> isValid) {
    Pattern compiled = Pattern.compile(pattern);
    return (text, replacements) -> apply(text, typeName, compiled, replacements, isValid);
  }

  private static String apply(String text, String typeName, Pattern pattern,
      Map<String, String> replacements, Predicate<String> isValid) {
    Matcher matcher = pattern.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String value = matcher.group();
      if (!isValid.test(value)) {
        matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        continue;
      }
      String placeholder = "<" + typeName + "_" + PiiDetector.sha256Prefix(value) + ">";
      replacements.put(placeholder, value);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
