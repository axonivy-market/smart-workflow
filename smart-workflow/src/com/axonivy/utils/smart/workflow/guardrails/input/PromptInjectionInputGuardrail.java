package com.axonivy.utils.smart.workflow.guardrails.input;

import java.util.regex.Pattern;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

public class PromptInjectionInputGuardrail implements SmartWorkflowInputGuardrail {

  private static final String FAILURE_MESSAGE = "The input message is rejected because it's empty or contains malicious content";

  // Pattern for high-confidence prompt injection attempts only.
  // Intentionally narrow to avoid false positives — use AiPromptInjectionInputGuardrail for strict detection.
  // Examples blocked: "Ignore all previous instructions", "Forget everything above", "pretend to be an admin"
  // Examples allowed: "@Override", "act as a code reviewer", "new role at company", "new instructions from manager"
  private static final Pattern PROMPT_INJECTION_PATTERN = Pattern
      .compile("(?i)(ignore (all )?previous|disregard (all )?(previous |your )?instructions?|"
          + "forget (all )?(previous |your )?instructions?|forget everything|"
          + "pretend (to be|you are|you're)|you are now|"
          + "(reveal|show|print|repeat|output|display) (me )?(your |the )?system prompt|"
          + "your system prompt|system:|assistant:)",
          Pattern.CASE_INSENSITIVE);

  // Pattern for excessive special characters that might be used in attacks
  // Examples: "}}}}}}}}}}{{{{{{{{{{" (template injection),
  // "<<<<<<<<<<<<>>>>>>>>>>>" (payload delimiters)
  private static final Pattern EXCESSIVE_SPECIAL_CHARS = Pattern.compile("[^\\w\\s]{10,}");

  // Pattern for control characters that can be used to hide malicious content
  // Examples: "malicious\x00hidden" (null byte injection),
  // "\x1B[2J" (ANSI escape codes to clear screen)
  private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");

  @Override
  public GuardrailResult evaluate(String message) {
    boolean isInvalid = PROMPT_INJECTION_PATTERN.matcher(message).find()
        || EXCESSIVE_SPECIAL_CHARS.matcher(message).find() || CONTROL_CHARS.matcher(message).find();

    return isInvalid ? GuardrailResult.block(FAILURE_MESSAGE) : GuardrailResult.allow();
  }
}