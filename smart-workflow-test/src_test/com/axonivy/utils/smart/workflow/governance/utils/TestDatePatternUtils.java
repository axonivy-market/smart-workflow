package com.axonivy.utils.smart.workflow.governance.utils;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

public class TestDatePatternUtils {

  @Test
  void parseLastUpdated_validIsoDateTime_returnsLocalDateTime() {
    assertThat(DatePatternUtils.parseLastUpdated("2025-06-15T10:30:00"))
        .isEqualTo(LocalDateTime.of(2025, 6, 15, 10, 30, 0));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "null input,         NULL",
      "date-only string,   2025-06-15",
      "invalid string,     not-a-date",
      "empty string,       ''"
  }, nullValues = "NULL")
  void parseLastUpdated_invalidInput_returnsNull(String description, String input) {
    assertThat(DatePatternUtils.parseLastUpdated(input)).as(description).isNull();
  }
}
