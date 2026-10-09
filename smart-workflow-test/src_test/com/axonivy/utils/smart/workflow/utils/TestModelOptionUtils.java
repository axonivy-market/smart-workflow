package com.axonivy.utils.smart.workflow.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestModelOptionUtils {

  private static final Double DEFAULT_TEMPERATURE = 0.0;

  @ParameterizedTest
  @CsvSource({
      "0.5, 0.5",
      "0.0, 0.0",
      "2.0, 2.0",
      "1, 1.0",
      "1.5E-1, 0.15",
      "  0.75, 0.75",
      "0.75  , 0.75"
  })
  void toTemperature_validTemperature_returnsValue(String input, double expected) {
    Double result = ModelOptionUtils.toTemperature(input);
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.5", "Infinity", "-Infinity", "NaN", "", "  ", "abc", "12.34.56", "1.2.3"})
  void toTemperature_invalidTemperature_returnsDefault(String input) {
    Double result = ModelOptionUtils.toTemperature(input);
    assertThat(result).isEqualTo(DEFAULT_TEMPERATURE);
  }

  @Test
  void toTemperature_null_throwsNullPointerException() {
    assertThatThrownBy(() -> ModelOptionUtils.toTemperature(null))
        .isInstanceOf(NullPointerException.class);
  }
}
