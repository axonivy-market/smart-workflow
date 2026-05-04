package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PiiDetector {

  public record MaskingResult(String maskedText, Map<String, String> placeholderToOriginal) {
    public boolean hasPii() {
      return !placeholderToOriginal.isEmpty();
    }
  }

  public static MaskingResult detectAndMask(String text) {
    if (text == null || text.isEmpty()) {
      return new MaskingResult(text, Map.of());
    }

    Map<String, String> placeholderToOriginal = new LinkedHashMap<>();
    String masked = text;

    for (MaskingRule rule : PiiMaskingRuleRegistry.getRules()) {
      masked = rule.mask(masked, placeholderToOriginal);
    }

    return new MaskingResult(masked, Map.copyOf(placeholderToOriginal));
  }

  public static String restore(String text, Map<String, String> placeholderToOriginal) {
    if (text == null || placeholderToOriginal == null || placeholderToOriginal.isEmpty()) {
      return text;
    }
    List<Map.Entry<String, String>> entries = new ArrayList<>(placeholderToOriginal.entrySet());
    Collections.reverse(entries);
    String restored = text;
    for (Map.Entry<String, String> entry : entries) {
      restored = restored.replace(entry.getKey(), entry.getValue());
    }
    return restored;
  }

  static boolean isValidCreditCard(String raw) {
    String digits = raw.replaceAll("[\\s\\-]", "");
    if (digits.length() < 13 || digits.length() > 19) {
      return false;
    }
    int sum = 0;
    boolean alternate = false;
    for (int i = digits.length() - 1; i >= 0; i--) {
      char c = digits.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
      int n = c - '0';
      if (alternate) {
        n *= 2;
        if (n > 9) {
          n -= 9;
        }
      }
      sum += n;
      alternate = !alternate;
    }
    return sum % 10 == 0;
  }

  static String sha256Prefix(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.substring(0, 12);
    } catch (NoSuchAlgorithmException e) {
      return Integer.toHexString(value.hashCode());
    }
  }
}
