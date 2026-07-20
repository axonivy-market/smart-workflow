package com.axonivy.utils.smart.workflow.governance.utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.ivyteam.ivy.environment.Ivy;

public final class TimeCalculationUtils {

  private static final long   MS_PER_MIN             = TimeUnit.MINUTES.toMillis(1);
  private static final String CMS_BASE          = "/Labels/Common/Duration/";
  private static final String CMS_IN_PROGRESS   = CMS_BASE + "InProgress";
  private static final String CMS_MINUTES       = CMS_BASE + "Minutes";
  private static final String CMS_HOUR          = CMS_BASE + "Hour";
  private static final String CMS_HOURS         = CMS_BASE + "Hours";
  private static final String CMS_HOUR_MINUTES  = CMS_BASE + "HourMinutes";
  private static final String CMS_HOURS_MINUTES = CMS_BASE + "HoursMinutes";

  private TimeCalculationUtils() {}

  public static String formatProcessingTime(Date start, Date end) {
    if (end == null) {
      return Ivy.cms().co(CMS_IN_PROGRESS);
    }
    long durationInMinutes = (end.getTime() - start.getTime()) / MS_PER_MIN;
    return formatDuration(durationInMinutes);
  }

  public static String formatDuration(long minutes) {
    if (minutes < 60) {
      return Ivy.cms().co(CMS_MINUTES, List.of(minutes));
    }
    long hours = minutes / 60;
    long remainingMinutes = minutes % 60;
    boolean singular = hours == 1;
    if (remainingMinutes > 0) {
      String key = singular ? CMS_HOUR_MINUTES : CMS_HOURS_MINUTES;
      return Ivy.cms().co(key, List.of(hours, remainingMinutes));
    }
    String key = singular ? CMS_HOUR : CMS_HOURS;
    return Ivy.cms().co(key, List.of(hours));
  }
}
