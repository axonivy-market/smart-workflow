package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.core.AiStep;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.input.PromptTemplate;

public class AgentPlanner extends AiFunction<IvyTool, List<AiStep>> {

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
        + analysis: explanation why use this tool
        + toolId: ID of the selected tool (REQUIRED)
        + next: Number of next step, -1 if final step
        + previous: Number of the previous step, 0 if first step
      - Tool ID is REQUIRED. Do not create a step without using tool ID from above.
      - Review the plan you created: is it good enough? Are there any unnecessary steps? Any missing critical steps?
      {{customInstructions}}
      -------------------------------
      """;

  private static final String AGENT_PART_TEMPLATE = """
      Available tools:
      {{tools}}
      """;

  private static final String TOOL_LINE_FORMAT = """
        + ID: %s
          Usage: %s
      """;

  private static final String STEPS = "steps";

  private List<IvyTool> tools;
  private String generatedPlan;

  public static Builder getBuilder() {
    return new Builder();
  }

  @Override
  protected boolean validateInputs() {
    return super.validateInputs() && CollectionUtils.isNotEmpty(tools);
  }

  @Override
  protected Map<String, Object> buildParameters() {
    String toolsPrompt = CollectionUtils.isEmpty(tools) ? StringUtils.EMPTY : buildToolsPrompt();

    Map<String, Object> params = new HashMap<>();
    params.put("tools", toolsPrompt);
    params.put("query", getQuery());
    params.put("customInstructions", formatCustomInstructions());

    return params;
  }

  @Override
  protected JsonSchema generateJsonSchema() {
    // Generate schema for AiStep class using JsonSchemaElementUtils
    JsonSchemaElement aiStepSchema = JsonSchemaElementUtils.jsonSchemaElementFrom(AiStep.class);

    // Create array schema for List<AiStep>
    JsonSchemaElement stepsArraySchema = JsonArraySchema.builder().description("List of steps in the execution plan")
        .items(aiStepSchema).build();

    // Create root object schema
    JsonSchemaElement rootSchema = JsonObjectSchema.builder().description("The execution plan with a list of steps")
        .addProperty(STEPS, stepsArraySchema).required(STEPS).build();

    return JsonSchema.builder().name("ExecutionPlan").rootElement(rootSchema).build();
  }

  @Override
  protected List<AiStep> parseJsonResponse(String jsonResponse) {
    try {
      // Parse the JSON response directly to get the steps array
      var jsonNode = BusinessEntityConverter.getObjectMapper().readTree(jsonResponse);
      var stepsNode = jsonNode.get(STEPS);

      if (stepsNode != null && stepsNode.isArray()) {
        // Convert JSON array directly to List<AiStep>
        List<AiStep> steps = BusinessEntityConverter.jsonValueToEntities(stepsNode.toString(), AiStep.class);

        // Generate the plan string for logging/debugging
        generatePlanString(steps);

        return steps;
      }

      return new ArrayList<>();

    } catch (Exception e) {
      System.err.println("Failed to parse agent plan response: " + e.getMessage());
      return new ArrayList<>();
    }
  }

  @Override
  protected String getTemplate() {
    return TEMPLATE;
  }

  private void generatePlanString(List<AiStep> steps) {
    StringBuilder planStrBuilder = new StringBuilder();
    planStrBuilder.append("Generated execution plan:").append(System.lineSeparator());

    for (AiStep step : steps) {
      planStrBuilder.append("Step ").append(step.getStepNo()).append(": ").append(step.getName()).append(" (Tool: ")
          .append(step.getToolId()).append(")").append(System.lineSeparator()).append("  Analysis: ")
          .append(step.getAnalysis()).append(System.lineSeparator()).append("  Previous: ").append(step.getPrevious())
          .append(", Next: ").append(step.getNext()).append(System.lineSeparator());
    }

    generatedPlan = planStrBuilder.toString().strip();
  }

  private String buildToolsPrompt() {
    String result = StringUtils.EMPTY;
    for (var tool : tools) {
      String line = String.format(TOOL_LINE_FORMAT, tool.getId(), tool.getUsage());
      result = result.concat(line);
    }

    Map<String, Object> params = new HashMap<>();
    params.put("tools", result);
    return PromptTemplate.from(AGENT_PART_TEMPLATE).apply(params).text();
  }

  // Getters and setters for domain-specific fields
  public List<IvyTool> getTools() {
    return tools;
  }

  public void setTools(List<IvyTool> tools) {
    this.tools = tools;
  }

  public String getGeneratedPlan() {
    return generatedPlan;
  }

  public List<AiStep> getSteps() {
    return getResult();
  }

  // Builder class for AgentPlanner
  public static class Builder extends AiFunction.Builder<IvyTool, List<AiStep>, AgentPlanner> {
    private List<IvyTool> tools;

    @Override
    public Builder useService(AbstractAiServiceConnector connector) {
      return (Builder) super.useService(connector);
    }

    @Override
    public Builder withQuery(String query) {
      return (Builder) super.withQuery(query);
    }

    @Override
    public Builder addCustomInstructions(List<String> instructions) {
      return (Builder) super.addCustomInstructions(instructions);
    }

    @Override
    public Builder addCustomInstruction(String instruction) {
      return (Builder) super.addCustomInstruction(instruction);
    }

    @Override
    public Builder addInputData(List<IvyTool> data) {
      return (Builder) super.addInputData(data);
    }

    public Builder addTool(IvyTool tool) {
      if (this.tools == null) {
        this.tools = new ArrayList<>();
      }
      this.tools.add(tool);
      return this;
    }

    public Builder addTools(List<IvyTool> tools) {
      if (this.tools == null) {
        this.tools = new ArrayList<>();
      }
      this.tools.addAll(tools);
      return this;
    }

    @Override
    public AgentPlanner build() {
      AgentPlanner planner = new AgentPlanner();
      planner.setConnector(connector);
      planner.setQuery(query);
      planner.setCustomInstructions(customInstructions);
      planner.setTools(tools);
      return planner;
    }
  }
}
