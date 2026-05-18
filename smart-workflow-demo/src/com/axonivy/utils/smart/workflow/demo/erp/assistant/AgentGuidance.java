package com.axonivy.utils.smart.workflow.demo.erp.assistant;

/**
 * A question-instruction pair that serves a dual purpose:
 * <ul>
 * <li><b>UI</b>: {@link #questionPattern()} is displayed as a clickable
 * suggestion chip in the AI assistant sidebar. Clicking it sends that question
 * to the agent.</li>
 * <li><b>System prompt</b>: {@link #instruction()} is compiled into the
 * agent's system prompt via {@link AssistantUploadSupport#compileGuidanceContext()},
 * telling the agent how to handle this type of question.</li>
 * </ul>
 *
 * <p>Example usage in a bean:
 * <pre>{@code
 * return List.of(
 *   new AgentGuidance("Is <supplier> already in the system?",
 *       "use tool findSimilarSuppliers to search and report matches"),
 *   new AgentGuidance("Can you fill out this form for me?",
 *       "answer 'no' — you can only assist, not submit on behalf of the user")
 * );
 * }</pre>
 *
 * @param questionPattern a natural-language description of the question type,
 *                        shown as a chip label in the assistant sidebar
 * @param instruction     the instruction for the agent on how to answer this
 *                        type of question, compiled into the system prompt
 */
public record AgentGuidance(String questionPattern, String instruction) {

  /** JSF EL accessor for {@link #questionPattern()}. */
  public String getQuestionPattern() {
    return questionPattern;
  }

  /** JSF EL accessor for {@link #instruction()}. */
  public String getInstruction() {
    return instruction;
  }

  /**
   * Formats this guidance as a single bullet line for inclusion in
   * the agent system prompt.
   */
  public String toPromptLine() {
    return "- When the user asks \"" + questionPattern + "\": " + instruction;
  }
}
