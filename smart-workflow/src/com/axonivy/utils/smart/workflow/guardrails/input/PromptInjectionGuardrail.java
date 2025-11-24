package com.axonivy.utils.smart.workflow.guardrails.input;

import java.util.regex.Pattern;

import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail.GuardrailType;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

/**
 * Input guardrail that sanitizes user messages before they are sent to LLM.
 * Protects against prompt injection attacks and removes potentially harmful content.
 */
@SmartWorkflowGuardrail(type = GuardrailType.INPUT)
public class PromptInjectionGuardrail implements InputGuardrail {
private static final int MAX_INPUT_LENGTH = 10000;
  
  // Patterns for detecting prompt injection attempts
  private static final Pattern PROMPT_INJECTION_PATTERN = Pattern.compile(
      "(?i)(ignore previous|ignore all previous|disregard previous|forget previous|" +
      "new instructions|system prompt|override|act as|pretend to be|" +
      "you are now|forget everything|new role|system:|assistant:)", 
      Pattern.CASE_INSENSITIVE
  );
  
  // Pattern for excessive special characters that might be used in attacks
  private static final Pattern EXCESSIVE_SPECIAL_CHARS = Pattern.compile("[^\\w\\s]{10,}");
  
  // Pattern for control characters
  private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    Ivy.log().error("Test");
    if (userMessage == null || userMessage.singleText() == null) {
      return InputGuardrailResult.successWith(userMessage.singleText());
    }

    String originalText = userMessage.singleText();
    String sanitizedText = sanitizeInput(originalText);

    if (sanitizedText.isBlank()) {
      return failure("Input was rejected: message is empty after sanitization");
    }

    // If content was modified, create new UserMessage with sanitized content
    if (!originalText.equals(sanitizedText)) {
      return InputGuardrailResult.successWith(sanitizedText);
    }

    return InputGuardrailResult.successWith(userMessage.singleText());
  }

  /**
   * Sanitizes the input text by applying multiple sanitization methods.
   * 
   * @param input the raw input text
   * @return sanitized text safe for LLM processing
   */
  private String sanitizeInput(String input) {
    if (input == null || input.isBlank()) {
      return "";
    }
    
    String sanitized = input;
    
    // 1. Remove control characters
    sanitized = removeControlCharacters(sanitized);
    
    // 2. Limit input length
    sanitized = limitLength(sanitized, MAX_INPUT_LENGTH);
    
    // 3. Remove potential prompt injection patterns
    sanitized = removePromptInjection(sanitized);
    
    // 4. Normalize whitespace
    sanitized = normalizeWhitespace(sanitized);
    
    // 5. Remove excessive special characters
    sanitized = removeExcessiveSpecialChars(sanitized);
    
    return sanitized.trim();
  }

  /**
   * Removes control characters that could interfere with processing.
   */
  private String removeControlCharacters(String input) {
    return CONTROL_CHARS.matcher(input).replaceAll("");
  }

  /**
   * Limits the input length to prevent resource exhaustion.
   */
  private String limitLength(String input, int maxLength) {
    if (input.length() > maxLength) {
      return input.substring(0, maxLength);
    }
    return input;
  }

  /**
   * Removes or neutralizes potential prompt injection patterns.
   * This method identifies common prompt injection attempts and removes them.
   */
  private String removePromptInjection(String input) {
    // Replace potential prompt injection patterns with safe alternatives
    return PROMPT_INJECTION_PATTERN.matcher(input).replaceAll("[FILTERED]");
  }

  /**
   * Normalizes whitespace by replacing multiple consecutive whitespace 
   * characters with a single space.
   */
  private String normalizeWhitespace(String input) {
    return input.replaceAll("\\s+", " ");
  }

  /**
   * Removes sequences of excessive special characters that might be used
   * in injection attacks or to confuse the model.
   */
  private String removeExcessiveSpecialChars(String input) {
    return EXCESSIVE_SPECIAL_CHARS.matcher(input).replaceAll("");
  }

  /**
   * Additional method to check if input contains suspicious patterns
   * without modifying it. Can be used for logging/monitoring.
   */
  public boolean containsSuspiciousPatterns(String input) {
    if (input == null || input.isBlank()) {
      return false;
    }

    return PROMPT_INJECTION_PATTERN.matcher(input).find() 
        || EXCESSIVE_SPECIAL_CHARS.matcher(input).find()
        || CONTROL_CHARS.matcher(input).find();
  }
}
