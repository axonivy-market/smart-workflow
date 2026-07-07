package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.LogLine;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.LogLineSeverity;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestLogLineBuilder {

  @Test
  void of_twoArgs_setsSeverityMessageAndNullItalic() {
    LogLine ok = LogLineBuilder.of(LogLineSeverity.OK, "Step completed");
    assertThat(ok.getSeverity()).isEqualTo(LogLineSeverity.OK);
    assertThat(ok.getMessage()).isEqualTo("Step completed");
    assertThat(ok.getItalic()).isNull();

    LogLine error = LogLineBuilder.of(LogLineSeverity.ERROR, "Something failed");
    assertThat(error.getSeverity()).isEqualTo(LogLineSeverity.ERROR);
    assertThat(error.getItalic()).isNull();
  }

  @Test
  void of_threeArgs_setsItalicFlag() {
    assertThat(LogLineBuilder.of(LogLineSeverity.OK, "Detail line", true).getItalic()).isTrue();
    assertThat(LogLineBuilder.of(LogLineSeverity.ERROR, "Error detail", false).getItalic()).isFalse();
  }
}
