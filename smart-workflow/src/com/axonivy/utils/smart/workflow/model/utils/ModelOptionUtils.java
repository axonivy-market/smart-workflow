package com.axonivy.utils.smart.workflow.model.utils;

import java.util.Optional;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffortResolver.Message;

import ch.ivyteam.ivy.environment.Ivy;

public final class ModelOptionUtils {
  public static final Double DEFAULT_TEMPERATURE = 0.0;

  public static Double toTemperature(String temperatureStr) {
    if (temperatureStr == null || temperatureStr.isBlank()) {
      return DEFAULT_TEMPERATURE;
    }
    try {
      Double temperature = Double.valueOf(temperatureStr);
      return Double.isFinite(temperature) && temperature >= 0 ? temperature : DEFAULT_TEMPERATURE;
    } catch (NumberFormatException ex) {
      return DEFAULT_TEMPERATURE;
    }
  }

  public static Double resolveTemperature(Double temperature) {
    return Optional.ofNullable(temperature).orElse(DEFAULT_TEMPERATURE);
  }

  public static Integer resolveMaxOutputTokens(Integer maxTokens) {
    return Optional.ofNullable(maxTokens).map(val -> val > 0 ? val : null).orElse(null);
  }

  public static ReasoningEffort toReasoningEffort(String effortStr) {
    if (StringUtils.isBlank(effortStr)) {
      return null;
    }
    var effort = EnumUtils.getEnumIgnoreCase(ReasoningEffort.class, effortStr.trim());
    if (effort == null) {
      Ivy.log().warn("Ignoring unknown reasoning effort '" + effortStr + "'. Use one of "
          + EnumUtils.getEnumList(ReasoningEffort.class) + ".");
    }
    return effort;
  }

  public static void warnReasoningEffortUnsupported(String providerName, ReasoningEffort effort) {
    if (effort != null) {
      Ivy.log().warn(Message.UNSUPPORTED.formatted(effort, providerName));
    }
  }
}
