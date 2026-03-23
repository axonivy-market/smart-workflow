package com.axonivy.utils.smart.workflow.utils;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;

public final class DateParsingUtils {

  private DateParsingUtils() {}

  public static String now() {
    return LocalDateTime.now().toString();
  }

  public static LocalDateTime parse(String dateTimeString) {
    return StringUtils.isBlank(dateTimeString) ? null : LocalDateTime.parse(dateTimeString);
  }
}
