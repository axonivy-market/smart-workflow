package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.util.List;

/**
 * Rules for Personally Identifiable Information (PII) — data that directly identifies
 * a person. Access secrets such as URLs, JWTs, bearer tokens, API keys, passwords, and
 * AWS keys are intentionally excluded: those are credentials, not identity data, and
 * will be handled by a dedicated CredentialMaskingGuardrail.
 */
public class PiiMaskingRuleRegistry {

  private static final List<MaskingRule> RULES = List.of(
      MaskingRule.of("IP_ADDRESS",
          "\\b(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"),
      MaskingRule.of("MAC_ADDRESS", "\\b([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}\\b"),
      MaskingRule.of("EMAIL", "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"),
      // + or 00 must not be preceded by alphanumeric to avoid matching within IBANs
      // or other digit sequences.
      // Known limitation: long numeric reference codes with a +/00 prefix can still false-positive.
      // Reliable phone detection requires NLP (e.g. libphonenumber).
      MaskingRule.of("PHONE",
          "(?<![A-Za-z0-9])(?:\\+|00)[1-9]\\d{0,2}[\\s.\\-]?(?:\\(?\\d{1,4}\\)?[\\s.\\-]?){1,4}\\d{2,4}"),
      // Known limitation: Luhn validation rejects most random sequences but any 13–19 digit value
      // that passes Luhn by coincidence (invoice numbers, serial numbers) will be masked.
      // Context-aware detection requires NLP.
      MaskingRule.of("CREDIT_DEBIT_CARD_NUMBER", "\\b(?:\\d[ \\-]?){13,19}\\b",
          PiiDetector::isValidCreditCard),
      MaskingRule.of("SSN", "\\b(?!000|666|9\\d{2})\\d{3}[\\- ]\\d{2}[\\- ]\\d{4}\\b"),
      MaskingRule.of("DATE_OF_BIRTH",
          "\\b(0?[1-9]|[12]\\d|3[01])[/\\-.](0?[1-9]|1[0-2])[/\\-.](19|20)\\d{2}\\b"));

  public static List<MaskingRule> getRules() {
    return RULES;
  }
}
