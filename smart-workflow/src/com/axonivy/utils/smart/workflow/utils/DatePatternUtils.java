package com.axonivy.utils.smart.workflow.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import ch.ivyteam.ivy.environment.Ivy;

public final class DatePatternUtils {

  public static DateTimeFormatter dateTimeFormatter() {
    return DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Ivy.session().getFormattingLocale());
  }
}
