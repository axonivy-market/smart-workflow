package com.axonivy.utils.smart.workflow.model.spi;

import java.util.Optional;

import ch.ivyteam.ivy.environment.Ivy;

public interface ReasoningEffortResolver<T> {

  interface Message {
    String FALLBACK = "Model '%s' does not support reasoning effort %s. Using %s instead.";
    String UNSUPPORTED = "Ignoring reasoning effort %s, because it is not supported for %s.";
  }

  Optional<T> toOption(String modelName, ReasoningEffort effort);

  default Optional<T> resolve(String modelName, ReasoningEffort effort) {
    if (effort == null) {
      return Optional.empty();
    }
    var option = toOption(modelName, effort);
    if (option.isEmpty() && effort != ReasoningEffort.NONE) {
      Ivy.log().warn(Message.UNSUPPORTED.formatted(effort, "model '" + modelName + "'"));
    }
    return option;
  }
}
