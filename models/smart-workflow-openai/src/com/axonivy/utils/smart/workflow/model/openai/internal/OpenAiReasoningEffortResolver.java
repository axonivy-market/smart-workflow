package com.axonivy.utils.smart.workflow.model.openai.internal;

import java.util.Optional;
import java.util.regex.Pattern;

import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffortResolver;

import ch.ivyteam.ivy.environment.Ivy;

public class OpenAiReasoningEffortResolver implements ReasoningEffortResolver<String> {

  private static final String MINIMAL = "minimal";

  // gpt-3.5-turbo, gpt-4, gpt-4-turbo, gpt-4o, gpt-4o-mini, gpt-4.1, gpt-4.1-mini, gpt-4.1-nano
  private static final Pattern NOT_SUPPORTED = models("gpt-3\\.5.*|gpt-4.*");
  private static final Pattern ONLY_HIGH = models("gpt-5-pro");
  // gpt-5.1, gpt-5.2, gpt-5.4, gpt-5.5, gpt-5.6-sol, gpt-5.6-terra, gpt-5.6-luna,
  // gpt-6-sol, gpt-6-luna, gpt-6.1-terra
  private static final Pattern SUPPORTS_NONE = models(
      "gpt-5\\.[1245]|gpt-5\\.6-(sol|terra|luna)|gpt-6-(sol|luna)|gpt-6\\.1-terra");
  // gpt-5, gpt-5-mini, gpt-5-nano
  private static final Pattern SUPPORTS_MINIMAL = models("gpt-5(-mini|-nano)?");

  @Override
  public Optional<String> toOption(String modelName, ReasoningEffort effort) {
    if (NOT_SUPPORTED.matcher(modelName).matches()) {
      return Optional.empty();
    }
    if (ONLY_HIGH.matcher(modelName).matches()) {
      var high = ReasoningEffort.HIGH.getText();
      return Optional.of(effort == ReasoningEffort.HIGH ? high : fallback(modelName, effort, high));
    }
    return Optional.of(effort == ReasoningEffort.NONE ? none(modelName) : effort.getText());
  }

  private static String none(String modelName) {
    if (SUPPORTS_NONE.matcher(modelName).matches()) {
      return ReasoningEffort.NONE.getText();
    }
    boolean minimal = SUPPORTS_MINIMAL.matcher(modelName).matches();
    return fallback(modelName, ReasoningEffort.NONE, minimal ? MINIMAL : ReasoningEffort.LOW.getText());
  }

  private static String fallback(String modelName, ReasoningEffort effort, String value) {
    Ivy.log().warn(Message.FALLBACK.formatted(modelName, effort, value));
    return value;
  }

  // also matches dated versions, such as gpt-5.1-2025-11-13
  private static Pattern models(String names) {
    return Pattern.compile("(" + names + ")(-\\d{4}-\\d{2}-\\d{2})?", Pattern.CASE_INSENSITIVE);
  }
}
