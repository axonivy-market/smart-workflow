package com.axonivy.utils.smart.workflow.governance.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import ch.ivyteam.ivy.environment.Ivy;

public final class DatePatternUtils {

  public static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMM dd");
  public static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

  public static LocalDateTime parseLastUpdated(String s) {
    if (s == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(s);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public static DateTimeFormatter dateTimeFormatter() {
    return DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Ivy.session().getFormattingLocale());
  }
}