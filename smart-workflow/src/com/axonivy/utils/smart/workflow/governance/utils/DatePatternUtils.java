package com.axonivy.utils.smart.workflow.governance.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import ch.ivyteam.ivy.environment.Ivy;

public final class DatePatternUtils {

  public static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMM dd");

  public static DateTimeFormatter dateTimeFormatter() {
    return DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Ivy.session().getFormattingLocale());
  }
}