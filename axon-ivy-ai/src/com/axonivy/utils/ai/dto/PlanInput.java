package com.axonivy.utils.ai.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.dto.ai.Instruction;

import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import dev.langchain4j.model.input.PromptTemplate;

public class PlanInput {

  private static final String TEMPLATE = """
      QUERY:
      {{query}}
      -------------------------------
      PROVIDED TOOLS:
      {{tools}}
      -------------------------------
      INSTRUCTION:
      - Each tool is capable of doing some tasks, analyze carefully before making decision
      - Only use the tools above, create a step-by-step plan to handle the query
      - Each step of the plan should have these required fields:
        + stepNo: An incremental number field, start with 1
        + name: A descriptive name for this step
        + toolSignature: Signature of the selected tool (REQUIRED)
      - Tool Signature is REQUIRED. Do not create a step without using tool Signature from above.
      - Review the plan you created: is it good enough? Are there any unnecessary steps? Any missing critical steps?
      {{instructions}}
      -------------------------------
      """;

  private static final String TOOL_LINE_FORMAT = """
        + Signature: %s
          Description: %s
      """;

  private String query;
  private List<Instruction> instructions;

  @SuppressWarnings("restriction")
  private List<CallSubStart> tools;

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public List<Instruction> getInstructions() {
    return instructions;
  }

  public void setInstructions(List<Instruction> instructions) {
    this.instructions = instructions;
  }

  @SuppressWarnings("restriction")
  public List<CallSubStart> getTools() {
    return tools;
  }

  @SuppressWarnings("restriction")
  public void setTools(List<CallSubStart> tools) {
    this.tools = tools;
  }

  @Override
  public String toString() {
    return PromptTemplate.from(TEMPLATE).apply(buildParameters()).text();
  }

  private Map<String, Object> buildParameters() {
    String toolsPrompt = CollectionUtils.isEmpty(tools) ? StringUtils.EMPTY : buildToolsPrompt();
    String instructionsPrompt = CollectionUtils.isEmpty(instructions) ? StringUtils.EMPTY : buildInstructionsPrompt();

    Map<String, Object> params = new HashMap<>();
    params.put("query", query == null ? StringUtils.EMPTY : query);
    params.put("tools", toolsPrompt);
    params.put("instructions", instructionsPrompt);

    return params;
  }

  @SuppressWarnings("restriction")
  private String buildToolsPrompt() {
    String result = StringUtils.EMPTY;
    for (var tool : tools) {
      String line = String.format(TOOL_LINE_FORMAT, tool.getSignature().toSignatureString(), tool.getDescription());
      result = result.concat(line);
    }
    return result;
  }

  /**
   * Formats instructions for inclusion in prompts.
   */
  protected final String buildInstructionsPrompt() {
    if (CollectionUtils.isEmpty(instructions)) {
      return StringUtils.EMPTY;
    }

    StringBuilder builder = new StringBuilder();
    for (Instruction instruction : instructions) {
      builder.append("- ").append(instruction.getContent()).append(System.lineSeparator());
    }

    return builder.toString().strip();
  }
}
