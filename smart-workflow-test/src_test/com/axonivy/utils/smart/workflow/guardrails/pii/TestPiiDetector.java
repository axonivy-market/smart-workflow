package com.axonivy.utils.smart.workflow.guardrails.pii;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.pii.PiiDetector.MaskingResult;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
public class TestPiiDetector {

  // The following fake credentials and PII data are used for testing purposes only.
  // They do not correspond to any real persons, accounts, or systems.

  @Test
  void detectEmail() {
    var result = PiiDetector.detectAndMask("Contact us at user@example.com for more info.");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).doesNotContain("user@example.com");
    assertThat(result.maskedText()).contains("<EMAIL_");
    assertThat(result.placeholderToOriginal()).containsValue("user@example.com");
  }

  @Test
  void detectUrl() {
    var result = PiiDetector.detectAndMask("Visit https://example.com/path for details.");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<URL_");
    assertThat(result.placeholderToOriginal()).containsValue("https://example.com/path");
  }

  @Test
  void detectIpAddress() {
    var result = PiiDetector.detectAndMask("Server is at 192.168.1.1.");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<IP_ADDRESS_");
    assertThat(result.placeholderToOriginal()).containsValue("192.168.1.1");
  }

  @Test
  void detectMacAddress() {
    var result = PiiDetector.detectAndMask("Device MAC: 00:1A:2B:3C:4D:5E");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<MAC_ADDRESS_");
    assertThat(result.placeholderToOriginal()).containsValue("00:1A:2B:3C:4D:5E");
  }

  @Test
  void detectAwsAccessKey() {
    // AKIAIOSFODNN7EXAMPLE is the canonical AWS documentation example key
    var result = PiiDetector.detectAndMask("Key: " + "AKIA" + "IOSFODNN7EXAMPLE");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<AWS_ACCESS_KEY_");
    assertThat(result.placeholderToOriginal()).containsValue("AKIAIOSFODNN7EXAMPLE");
  }

  @Test
  void detectPhoneWithPlusPrefix() {
    var result = PiiDetector.detectAndMask("Call me at +1 555 123 4567.");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<PHONE_");
  }

  @Test
  void detectPhoneWithDoubleZeroPrefix() {
    var result = PiiDetector.detectAndMask("International: 0049 30 12345678");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<PHONE_");
  }

  @Test
  void creditCardNumberNotMatchedAsPhone() {
    // Bare 16-digit number without +/00 prefix must not trigger the PHONE pattern
    var result = PiiDetector.detectAndMask("Card: 4532015112830366");
    assertThat(result.maskedText()).doesNotContain("<PHONE_");
    assertThat(result.maskedText()).contains("<CREDIT_DEBIT_CARD_NUMBER_");
  }

  @Test
  void detectValidCreditCard() {
    var result = PiiDetector.detectAndMask("Pay with 4532015112830366");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<CREDIT_DEBIT_CARD_NUMBER_");
    assertThat(result.placeholderToOriginal()).containsValue("4532015112830366");
  }

  @Test
  void creditCardFailingLuhnIsNotDetected() {
    // 1234567890123456 has Luhn sum 64 (mod 10 ≠ 0) — not a valid card number
    var result = PiiDetector.detectAndMask("Number: 1234567890123456");
    assertThat(result.hasPii()).isFalse();
  }

  @Test
  void detectCreditCardExpiry() {
    var result = PiiDetector.detectAndMask("Expires: 12/2025");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<CREDIT_DEBIT_CARD_EXPIRY_");
    assertThat(result.placeholderToOriginal()).containsValue("12/2025");
  }

  @Test
  void detectIban() {
    // DE89370400440532013000 is the canonical German IBAN example from ISO 13616
    var result = PiiDetector.detectAndMask("IBAN: DE89370400440532013000");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<INTERNATIONAL_BANK_ACCOUNT_NUMBER_");
    assertThat(result.placeholderToOriginal()).containsValue("DE89370400440532013000");
  }

  @Test
  void detectSwiftCode() {
    var result = PiiDetector.detectAndMask("BIC: DEUTDEDB");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<SWIFT_CODE_");
    assertThat(result.placeholderToOriginal()).containsValue("DEUTDEDB");
  }

  @Test
  void detectVin() {
    // 1HGBH41JXMN109186 is the NHTSA documentation example VIN
    var result = PiiDetector.detectAndMask("VIN: 1HGBH41JXMN109186");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<VEHICLE_IDENTIFICATION_NUMBER_");
    assertThat(result.placeholderToOriginal()).containsValue("1HGBH41JXMN109186");
  }

  @Test
  void detectSsn() {
    var result = PiiDetector.detectAndMask("SSN: 123-45-6789");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<SSN_");
    assertThat(result.placeholderToOriginal()).containsValue("123-45-6789");
  }

  @Test
  void detectDateOfBirth() {
    var result = PiiDetector.detectAndMask("Born: 15/06/1990");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<DATE_OF_BIRTH_");
    assertThat(result.placeholderToOriginal()).containsValue("15/06/1990");
  }

  @Test
  void cleanTextPassesThrough() {
    var result = PiiDetector.detectAndMask("Hello, how are you today?");
    assertThat(result.hasPii()).isFalse();
    assertThat(result.maskedText()).isEqualTo("Hello, how are you today?");
  }

  @Test
  void nullReturnedUnchanged() {
    var result = PiiDetector.detectAndMask(null);
    assertThat(result.maskedText()).isNull();
    assertThat(result.hasPii()).isFalse();
  }

  @Test
  void emptyTextReturnedUnchanged() {
    var result = PiiDetector.detectAndMask("");
    assertThat(result.maskedText()).isEmpty();
    assertThat(result.hasPii()).isFalse();
  }

  @Test
  void restoreRoundTrip() {
    String original = "Email user@example.com from IP 192.168.1.1";
    MaskingResult masked = PiiDetector.detectAndMask(original);
    assertThat(masked.hasPii()).isTrue();
    String restored = PiiDetector.restore(masked.maskedText(), masked.placeholderToOriginal());
    assertThat(restored).isEqualTo(original);
  }

  @Test
  void placeholderIsDeterministic() {
    var first = PiiDetector.detectAndMask("user@example.com");
    var second = PiiDetector.detectAndMask("user@example.com");
    assertThat(first.maskedText()).isEqualTo(second.maskedText());
  }

  @Test
  void isValidCreditCard_luhnPass() {
    assertThat(PiiDetector.isValidCreditCard("4532015112830366")).isTrue();
  }

  @Test
  void isValidCreditCard_luhnFail() {
    assertThat(PiiDetector.isValidCreditCard("1234567890123456")).isFalse();
  }

  @Test
  void sha256PrefixIsTwelveHexChars() {
    String prefix = PiiDetector.sha256Prefix("test");
    assertThat(prefix).hasSize(12).matches("[0-9a-f]+");
  }

  @Test
  void detectBearerToken() {
    var result = PiiDetector.detectAndMask("Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<BEARER_TOKEN_");
    assertThat(result.maskedText()).doesNotContain("<JWT_");
  }

  @Test
  void detectJwt() {
    var result = PiiDetector.detectAndMask("Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<JWT_");
  }

  @Test
  void detectApiKey() {
    var result = PiiDetector.detectAndMask("api_key=sk-test-1234567890abcdef");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<API_KEY_");
    assertThat(result.maskedText()).doesNotContain("sk-test-1234567890abcdef");
    assertThat(result.placeholderToOriginal()).containsValue("api_key=sk-test-1234567890abcdef");
  }

  @Test
  void maskPassword() {
    var result = PiiDetector.detectAndMask("password=MySecretP4ss!");
    assertThat(result.hasPii()).isTrue();
    assertThat(result.maskedText()).contains("<PASSWORD_");
    assertThat(result.maskedText()).doesNotContain("MySecretP4ss!");
    assertThat(result.placeholderToOriginal()).containsValue("password=MySecretP4ss!");
  }

  @Test
  void countByTypeTracksDetections() {
    var result = PiiDetector.detectAndMask("user@example.com and admin@example.org");
    assertThat(result.countByType()).containsEntry("EMAIL", 2);
  }
}
