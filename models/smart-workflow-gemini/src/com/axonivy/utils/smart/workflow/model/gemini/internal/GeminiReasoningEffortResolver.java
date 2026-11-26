package com.axonivy.utils.smart.workflow.model.gemini.internal;

import java.util.Optional;
import java.util.Set;

import com.axonivy.utils.smart.workflow.model.gemini.internal.enums.GoogleAiGeminiChatModelName;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffortResolver;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;

public class GeminiReasoningEffortResolver implements ReasoningEffortResolver<GeminiThinkingConfig> {

  private static final String PRO = GoogleAiGeminiChatModelName.GEMINI_2_5_PRO.getName();
  private static final String FLASH = GoogleAiGeminiChatModelName.GEMINI_2_5_FLASH.getName();
  private static final Set<String> USES_BUDGET = Set.of(PRO, FLASH);

  // see https://ai.google.dev/gemini-api/docs/openai
  private interface Budget {
    int OFF = 0;
    int PRO_MINIMUM = 128;
    int LOW = 1024;
    int MEDIUM = 8192;
    int HIGH = 24576;
  }

  @Override
  public Optional<GeminiThinkingConfig> toOption(String modelName, ReasoningEffort effort) {
    if (!USES_BUDGET.contains(modelName)) {
      return Optional.empty();
    }
    int budget = switch (effort) {
      case NONE -> lowest(modelName);
      case LOW -> Budget.LOW;
      case MEDIUM -> Budget.MEDIUM;
      case HIGH -> Budget.HIGH;
    };
    return Optional.of(GeminiThinkingConfig.builder().thinkingBudget(budget).build());
  }

  private static int lowest(String modelName) {
    if (FLASH.equals(modelName)) {
      return Budget.OFF;
    }
    Ivy.log().warn(Message.FALLBACK.formatted(modelName, ReasoningEffort.NONE,
        "a budget of " + Budget.PRO_MINIMUM + " tokens"));
    return Budget.PRO_MINIMUM;
  }
}
