package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiDetector {

  public record MaskingResult(String maskedText, Map<String, String> placeholderToOriginal) {
    public boolean hasPii() {
      return !placeholderToOriginal.isEmpty();
    }
  }

  // Skipped (require NLP or too many false positives with regex alone):
  // NAME, ADDRESS, AGE, USERNAME, PASSWORD, DRIVER_ID, LICENSE_PLATE,
  // CREDIT_DEBIT_CARD_CVV (3-4 digits), PIN (4 digits), AWS_SECRET_KEY (40-char base64)
  private enum PiiType {
    // IT
    URL("https?://[^\\s]+"),
    AWS_ACCESS_KEY("\\b(AKIA|ASIA|AROA|ANPA|ANVA|APKA)[A-Z0-9]{16}\\b"),
    IP_ADDRESS("\\b(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"),
    MAC_ADDRESS("\\b([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}\\b"),

    // General
    EMAIL("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"),
    PHONE("(?:(?:\\+|00)[1-9]\\d{0,2}[\\s.\\-]?)?(?:\\(?\\d{2,4}\\)?[\\s.\\-]?){2,4}\\d{4}"),

    // Finance
    INTERNATIONAL_BANK_ACCOUNT_NUMBER("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{4,30}\\b"),
    SWIFT_CODE("\\b[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}(?:[A-Z0-9]{3})?\\b"),
    CREDIT_DEBIT_CARD_NUMBER("\\b(?:\\d[ \\-]?){13,19}\\b"),
    CREDIT_DEBIT_CARD_EXPIRY("\\b(0[1-9]|1[0-2])[/\\-](20\\d{2}|\\d{2})\\b"),

    // Documents / IDs
    VEHICLE_IDENTIFICATION_NUMBER("\\b[A-HJ-NPR-Z0-9]{17}\\b"),
    SSN("\\b(?!000|666|9\\d{2})\\d{3}[\\- ]\\d{2}[\\- ]\\d{4}\\b"),
    DATE_OF_BIRTH("\\b(0?[1-9]|[12]\\d|3[01])[/\\-.](0?[1-9]|1[0-2])[/\\-.](19|20)\\d{2}\\b");

    final Pattern pattern;

    PiiType(String regex) {
      this.pattern = Pattern.compile(regex);
    }
  }

  // Detection order matters: more specific patterns first to avoid partial matches.
  // URL before EMAIL (avoid matching email inside mailto: URLs).
  // AWS_ACCESS_KEY before CREDIT_DEBIT_CARD_NUMBER (to avoid digit overlap).
  // IBAN before CREDIT_DEBIT_CARD_NUMBER (IBAN starts with country code, very specific).
  // SWIFT_CODE before IBAN (shorter, must match first to avoid IBAN consuming it).
  // CREDIT_DEBIT_CARD_EXPIRY before CREDIT_DEBIT_CARD_NUMBER (avoid date being matched as CC digits).
  private static final PiiType[] DETECTION_ORDER = {
      PiiType.URL,
      PiiType.AWS_ACCESS_KEY,
      PiiType.MAC_ADDRESS,
      PiiType.IP_ADDRESS,
      PiiType.EMAIL,
      PiiType.PHONE,
      PiiType.SWIFT_CODE,
      PiiType.INTERNATIONAL_BANK_ACCOUNT_NUMBER,
      PiiType.CREDIT_DEBIT_CARD_EXPIRY,
      PiiType.CREDIT_DEBIT_CARD_NUMBER,
      PiiType.VEHICLE_IDENTIFICATION_NUMBER,
      PiiType.SSN,
      PiiType.DATE_OF_BIRTH
  };

  public static MaskingResult detectAndMask(String text) {
    if (text == null || text.isEmpty()) {
      return new MaskingResult(text, Map.of());
    }

    Map<String, String> placeholderToOriginal = new LinkedHashMap<>();
    String masked = text;

    for (PiiType type : DETECTION_ORDER) {
      Matcher matcher = type.pattern.matcher(masked);
      StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
        String original = matcher.group();
        if (type == PiiType.CREDIT_DEBIT_CARD_NUMBER && !isValidCreditCard(original)) {
          matcher.appendReplacement(sb, Matcher.quoteReplacement(original));
          continue;
        }
        String placeholder = "[" + type.name() + "_" + sha256Prefix(original) + "]";
        placeholderToOriginal.put(placeholder, original);
        matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
      }
      matcher.appendTail(sb);
      masked = sb.toString();
    }

    return new MaskingResult(masked, placeholderToOriginal);
  }

  public static String restore(String text, Map<String, String> placeholderToOriginal) {
    if (text == null || placeholderToOriginal == null || placeholderToOriginal.isEmpty()) {
      return text;
    }
    String restored = text;
    for (Map.Entry<String, String> entry : placeholderToOriginal.entrySet()) {
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
      return hex.substring(0, 8);
    } catch (NoSuchAlgorithmException e) {
      return Integer.toHexString(value.hashCode());
    }
  }
}
