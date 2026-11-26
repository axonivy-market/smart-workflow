package com.axonivy.utils.smart.workflow.model.ollama.internal;

import java.util.Optional;
import java.util.regex.Pattern;

import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffortResolver;

import ch.ivyteam.ivy.environment.Ivy;

public class OllamaReasoningEffortResolver implements ReasoningEffortResolver<Boolean> {

  // only well-known ones, since users can install any model
  // llama2, llama3, llama3.1, llama3.2, llama3.3, llama4, llava, gemma, gemma2, gemma3
  // with any tag, such as llama3.2:3b
  private static final Pattern NOT_SUPPORTED = Pattern.compile("(llama[234]|llava|gemma[23]?)([.:-].*)?");
  // gpt-oss:20b, gpt-oss:120b
  private static final String ALWAYS_THINKS = "gpt-oss";

  @Override
  public Optional<Boolean> toOption(String modelName, ReasoningEffort effort) {
    if (NOT_SUPPORTED.matcher(modelName).matches()) {
      return Optional.empty();
    }
    if (effort == ReasoningEffort.NONE && modelName.startsWith(ALWAYS_THINKS)) {
      Ivy.log().warn(Message.FALLBACK.formatted(modelName, effort, ReasoningEffort.LOW.getText()));
    }
    return Optional.of(effort != ReasoningEffort.NONE);
  }
}
