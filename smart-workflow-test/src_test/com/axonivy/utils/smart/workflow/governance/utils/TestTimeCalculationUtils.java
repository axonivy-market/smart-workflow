package com.axonivy.utils.smart.workflow.governance.utils;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Date;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestTimeCalculationUtils {

  @Test
  void formatProcessingTime_nullEnd_returnsInProgress() {
    assertThat(TimeCalculationUtils.formatProcessingTime(new Date(), null)).isEqualTo("In progress");
  }

  @Test
  void formatProcessingTime_withEnd_delegatesToFormatDuration() {
    assertThat(TimeCalculationUtils.formatProcessingTime(new Date(0), new Date(30 * 60 * 1000L))).isEqualTo("30 min");
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "zero_minutes,         0,   '0 min'",
      "45_minutes,           45,  '45 min'",
      "59_minutes,           59,  '59 min'",
      "exactly_one_hour,     60,  '1 hour'",
      "one_hour_one_minute,  61,  '1 hour 1 min'",
      "one_hour_30_minutes,  90,  '1 hour 30 min'",
      "exactly_two_hours,    120, '2 hours'",
      "two_hours_5_minutes,  125, '2 hours 5 min'"
  })
  void formatDuration(String testName, long minutes, String expected) {
    assertThat(TimeCalculationUtils.formatDuration(minutes)).as(testName).isEqualTo(expected);
  }
}
