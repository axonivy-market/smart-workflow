package com.axonivy.utils.smart.workflow.model.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;

@IvyTest
public class TestModelOptionUtils {

  private static final Double DEFAULT_TEMPERATURE = 0.0;

  @RegisterExtension
  LoggerAccess log = new LoggerAccess();

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
  void toTemperature_null_returnDefault() {
    Double result = ModelOptionUtils.toTemperature(null);
    assertThat(result).isEqualTo(DEFAULT_TEMPERATURE);
  }

  @ParameterizedTest
  @CsvSource({
      "0.5, 0.5",
      "0.0, 0.0",
      "2.0, 2.0",
      "1, 1.0",
      "1.5E-1, 0.15",
      "  0.75, 0.75",
      "0.75  , 0.75",
      ", 0.0"
  })
  void resolve_temperature(Double input, double expected) {
    Double result = ModelOptionUtils.resolveTemperature(input);
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"NONE, NONE", "LOW, LOW", "MEDIUM, MEDIUM", "HIGH, HIGH", "low, LOW", "' High ', HIGH"})
  void toReasoningEffort(String input, ReasoningEffort expected) {
    assertThat(ModelOptionUtils.toReasoningEffort(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "  "})
  void toReasoningEffort_notSet(String input) {
    assertThat(ModelOptionUtils.toReasoningEffort(input)).isNull();
    assertThat(log.warnings()).isEmpty();
  }

  @Test
  void toReasoningEffort_unknown() {
    assertThat(ModelOptionUtils.toReasoningEffort("extreme")).isNull();
    assertThat(log.warnings()).containsExactly(
        "Ignoring unknown reasoning effort 'extreme'. Use one of [NONE, LOW, MEDIUM, HIGH].");
  }
}
