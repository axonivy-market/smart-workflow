package com.axonivy.utils.smart.workflow.model.xai.internal;

import java.util.Optional;

import org.apache.commons.lang3.Strings;

import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffortResolver;

import ch.ivyteam.ivy.environment.Ivy;

public class XAiReasoningEffortResolver implements ReasoningEffortResolver<String> {

  // grok-4.20-0309-non-reasoning
  private static final String NON_REASONING = "-non-reasoning";
  // grok-4.3, grok-4.3-latest
  private static final String SUPPORTS_NONE = "grok-4.3";

  @Override
  public Optional<String> toOption(String modelName, ReasoningEffort effort) {
    if (Strings.CI.contains(modelName, NON_REASONING)) {
      return Optional.empty();
    }
    return Optional.of(effort == ReasoningEffort.NONE ? none(modelName) : effort.getText());
  }

  private static String none(String modelName) {
    if (Strings.CI.startsWith(modelName, SUPPORTS_NONE)) {
      return ReasoningEffort.NONE.getText();
    }
    var low = ReasoningEffort.LOW.getText();
    Ivy.log().warn(Message.FALLBACK.formatted(modelName, ReasoningEffort.NONE, low));
    return low;
  }
}
