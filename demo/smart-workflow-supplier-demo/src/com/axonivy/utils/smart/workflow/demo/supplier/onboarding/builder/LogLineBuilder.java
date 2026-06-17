package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.LogLine;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.LogLineSeverity;

public class LogLineBuilder {

  private LogLineBuilder() {
  }

  public static LogLine of(LogLineSeverity severity, String message) {
    return createBase(severity, message);
  }

  public static LogLine of(LogLineSeverity severity, String message, boolean italic) {
    LogLine line = createBase(severity, message);
    line.setItalic(italic);
    return line;
  }

  private static LogLine createBase(LogLineSeverity severity, String message) {
    LogLine line = new LogLine();
    line.setSeverity(severity);
    line.setMessage(message);
    return line;
  }
}
