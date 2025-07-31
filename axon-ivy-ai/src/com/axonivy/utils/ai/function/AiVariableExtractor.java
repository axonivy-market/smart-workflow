package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.IvyToolParameter;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.Instruction;

import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

public class AiVariableExtractor extends AiFunction<AiVariable, List<AiVariable>> {

  private static final String VARIABLE_FORMAT = """
      Variable %d
      ID: %s
      Name: %s
      Description: %s
      """;

  private static final String TEMPLATE = """
      {{variables}}
      -------------------
      INSTRUCTIONS:
      - Extract ID of selected variables.
      - If there is no variable from the list matched, return an empty JSON array.
      -------------------
      CONDITION TO CHOOSE VARIABLES:
      {{instructions}}
      """;

  private static final String VARIABLE_IDS = "variableIds";

  private List<Instruction> instructions;
  private List<AiVariable> variables;

  public static Builder getBuilder() {
    return new Builder();
  }

  @Override
  protected boolean validateInputs() {
    return getConnector() != null && CollectionUtils.isNotEmpty(variables);
  }

  @Override
  protected Map<String, Object> buildParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("variables", convertAiVariablesToString());
    params.put("instructions", convertInstructionsToString());
    return params;
  }

  @Override
  protected JsonSchema generateJsonSchema() {
    JsonSchemaElement itemSchema = JsonStringSchema.builder().description("ID of selected variable").build();

    JsonSchemaElement arraySchema = JsonArraySchema.builder().description("List of ID of selected variable")
        .items(itemSchema).build();

    JsonSchemaElement rootSchema = JsonObjectSchema.builder().description("The ID list of selected variable")
        .addProperty(VARIABLE_IDS, arraySchema).build();

    return JsonSchema.builder().name("ExtractedVariables").rootElement(rootSchema).build();
  }

  @Override
  protected List<AiVariable> parseJsonResponse(String jsonResponse) {
    List<String> variableIds = parseJsonArray(jsonResponse, VARIABLE_IDS);

    if (CollectionUtils.isEmpty(variableIds)) {
      return new ArrayList<>();
    }

    List<AiVariable> extracted = new ArrayList<>();
    for (String variableId : variableIds) {
      Optional<AiVariable> found = variables.stream().filter(variable -> variable.getId().equals(variableId))
          .findFirst();
      if (found.isPresent()) {
        extracted.add(found.get());
      }
    }

    return extracted;
  }

  @Override
  protected List<AiVariable> processResult(List<AiVariable> parsedResult) {
    // If there are no instructions or variables to extract, return all variables
    if (CollectionUtils.isEmpty(instructions) || CollectionUtils.isEmpty(variables)) {
      return variables != null ? variables : new ArrayList<>();
    }

    return parsedResult != null ? parsedResult : new ArrayList<>();
  }

  @Override
  protected String getTemplate() {
    return TEMPLATE;
  }

  private String convertAiVariablesToString() {
    String variablesStr = StringUtils.EMPTY;
    if (CollectionUtils.isNotEmpty(variables)) {
      for (int i = 0; i < variables.size(); i++) {
        Optional<AiVariable> current = Optional.ofNullable(variables.get(i));
        variablesStr += String.format(VARIABLE_FORMAT, i + 1, current.map(AiVariable::getId).orElse(StringUtils.EMPTY),
            current.map(AiVariable::getParameter).map(IvyToolParameter::getName).orElse(StringUtils.EMPTY),
            current.map(AiVariable::getParameter).map(IvyToolParameter::getDescription).orElse(StringUtils.EMPTY));
      }
    }
    return variablesStr;
  }

  private String convertInstructionsToString() {
    StringBuilder builder = new StringBuilder();
    if (CollectionUtils.isNotEmpty(instructions)) {
      for (Instruction item : instructions) {
        builder.append(String.format("- %s", item.getContent())).append(System.lineSeparator());
      }
    }
    return builder.toString().strip();
  }

  // Getters and setters for domain-specific fields
  public List<Instruction> getInstructions() {
    return instructions;
  }

  public void setInstructions(List<Instruction> instructions) {
    this.instructions = instructions;
  }

  public List<AiVariable> getVariables() {
    return variables;
  }

  public void setVariables(List<AiVariable> variables) {
    this.variables = variables;
  }

  // Renamed for clarity - this is the extracted result
  public List<AiVariable> getExtracted() {
    return getResult();
  }

  public static class Builder extends AiFunction.Builder<AiVariable, List<AiVariable>, AiVariableExtractor> {
    private List<Instruction> instructions;
    private List<AiVariable> variables;

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
    public Builder addInputData(List<AiVariable> data) {
      return (Builder) super.addInputData(data);
    }

    public Builder addInstructions(List<Instruction> instructions) {
      if (CollectionUtils.isNotEmpty(instructions)) {
        if (this.instructions == null) {
          this.instructions = new ArrayList<>();
        }
        this.instructions.addAll(instructions);
      }
      return this;
    }

    public Builder addVariables(List<AiVariable> variables) {
      if (CollectionUtils.isNotEmpty(variables)) {
        if (this.variables == null) {
          this.variables = new ArrayList<>();
        }
        this.variables.addAll(variables);
      }
      return this;
    }

    @Override
    public AiVariableExtractor build() {
      AiVariableExtractor extractor = new AiVariableExtractor();
      extractor.setConnector(connector);
      extractor.setQuery(query);
      extractor.setCustomInstructions(customInstructions);
      extractor.setInstructions(instructions);
      extractor.setVariables(variables);
      return extractor;
    }
  }
}
