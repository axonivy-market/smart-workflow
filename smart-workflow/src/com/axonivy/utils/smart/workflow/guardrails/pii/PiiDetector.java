package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiDetector {

  public record MaskingResult(String maskedText, Map<String, String> placeholderToOriginal) {
    public boolean hasPii() {
      return !placeholderToOriginal.isEmpty();
    }
  }

  private enum PiiType {
    URL("https?://[^\\s]+"),
    // BEARER_TOKEN before JWT: "Bearer eyJ..." must match as a whole before the JWT
    // portion is extracted.
    BEARER_TOKEN("(?i)\\bBearer\\s+[A-Za-z0-9\\-._~+/]{20,}={0,2}"),
    JWT("eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*"),
    API_KEY(
        "(?i)\\b(?:api[-_]?key|api[-_]?token|access[-_]?key|secret[-_]?key|auth[-_]?token)\\b\\s*[=:]\\s*[A-Za-z0-9\\-_.]{16,}"),
    PASSWORD("(?i)\\b(?:password|passwd|pwd|pass)\\b\\s*[=:]\\s*\\S{8,}"),
    AWS_ACCESS_KEY("\\b(AKIA|ASIA|AROA|ANPA|ANVA|APKA)[A-Z0-9]{16}\\b"),
    IP_ADDRESS(
        "\\b(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"),
    MAC_ADDRESS("\\b([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}\\b"),
    EMAIL("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"),
    // + or 00 must not be preceded by alphanumeric to avoid matching within IBANs
    // or other digit sequences.
    // Known limitation: long numeric reference codes with a +/00 prefix can still false-positive.
    // Reliable phone detection requires NLP (e.g. libphonenumber).
    PHONE("(?<![A-Za-z0-9])(?:\\+|00)[1-9]\\d{0,2}[\\s.\\-]?(?:\\(?\\d{1,4}\\)?[\\s.\\-]?){1,4}\\d{2,4}"),
    // Known limitation: Luhn validation rejects most random sequences but any 13–19 digit value
    // that passes Luhn by coincidence (invoice numbers, serial numbers) will be masked.
    // Context-aware detection requires NLP.
    CREDIT_DEBIT_CARD_NUMBER("\\b(?:\\d[ \\-]?){13,19}\\b"),
    SSN("\\b(?!000|666|9\\d{2})\\d{3}[\\- ]\\d{2}[\\- ]\\d{4}\\b"),
    DATE_OF_BIRTH("\\b(0?[1-9]|[12]\\d|3[01])[/\\-.](0?[1-9]|1[0-2])[/\\-.](19|20)\\d{2}\\b");

    final Pattern pattern;

    PiiType(String regex) {
      this.pattern = Pattern.compile(regex);
    }
  }

  public static MaskingResult detectAndMask(String text) {
    if (text == null || text.isEmpty()) {
      return new MaskingResult(text, Map.of());
    }

    Map<String, String> placeholderToOriginal = new LinkedHashMap<>();
    String masked = text;

    for (PiiType type : PiiType.values()) {
      Matcher matcher = type.pattern.matcher(masked);
      StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
        String piiValue = matcher.group();

        if (type == PiiType.CREDIT_DEBIT_CARD_NUMBER && !isValidCreditCard(piiValue)) {
          matcher.appendReplacement(sb, Matcher.quoteReplacement(piiValue));
          continue;
        }

        String placeholder = "<" + type.name() + "_" + sha256Prefix(piiValue) + ">";
        placeholderToOriginal.put(placeholder, piiValue);
        matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
      }
      matcher.appendTail(sb);
      masked = sb.toString();
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
