package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.LogLine;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;

public class LogLineBuilder {

  private LogLineBuilder() {
  }

  public static LogLine of(LogLineSeverity severity, String message) {
    LogLine line = new LogLine();
    line.setSeverity(severity);
    line.setMessage(message);
    return line;
  }

  public static LogLine of(LogLineSeverity severity, String message, boolean italic) {
    LogLine line = new LogLine();
    line.setSeverity(severity);
    line.setMessage(message);
    line.setItalic(italic);
    return line;
  }
}
