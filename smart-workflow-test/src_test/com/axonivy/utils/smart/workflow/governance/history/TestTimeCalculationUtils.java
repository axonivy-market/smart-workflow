package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.utils.TimeCalculationUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestTimeCalculationUtils {

  @Test
  void formatProcessingTime_nullEnd_doesNotThrow() {
    assertThatCode(() -> TimeCalculationUtils.formatProcessingTime(new Date(), null))
        .doesNotThrowAnyException();
  }

  @Test
  void formatProcessingTime_nullStart_doesNotThrow() {
    assertThatCode(() -> TimeCalculationUtils.formatProcessingTime(null, new Date()))
        .doesNotThrowAnyException();
  }

  @Test
  void formatProcessingTime_bothNull_doesNotThrow() {
    assertThatCode(() -> TimeCalculationUtils.formatProcessingTime(null, null))
        .doesNotThrowAnyException();
  }

  @Test
  void formatProcessingTime_nullStart_returnsSameAsNullEnd() {
    var nullStart = TimeCalculationUtils.formatProcessingTime(null, new Date());
    var nullEnd = TimeCalculationUtils.formatProcessingTime(new Date(), null);
    assertThat(nullStart).isEqualTo(nullEnd);
  }

  @Test
  void formatDuration_negative_clampsToZero() {
    var negative = TimeCalculationUtils.formatDuration(-60);
    var zero = TimeCalculationUtils.formatDuration(0);
    assertThat(negative).isEqualTo(zero);
  }

  @Test
  void formatDuration_zero_doesNotThrow() {
    assertThatCode(() -> TimeCalculationUtils.formatDuration(0))
        .doesNotThrowAnyException();
  }

  @Test
  void formatDuration_exactOneHour_doesNotThrow() {
    assertThatCode(() -> TimeCalculationUtils.formatDuration(60))
        .doesNotThrowAnyException();
  }
}
