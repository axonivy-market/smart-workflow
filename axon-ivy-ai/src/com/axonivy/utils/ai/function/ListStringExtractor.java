package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiOption;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

public class ListStringExtractor extends AiFunction<AiOption, List<String>> {

  private static final String TEMPLATE = """
      {{query}}
      -------------------------------
      INSTRUCTIONS:
      - Extract a list of String from the above information
      - Don't make up result, ONLY use information from the information above
      {{customInstructions}}
      """;

  private static final String RESULT = "result";

  public static Builder getBuilder() {
    return new Builder();
  }

  @Override
  protected Map<String, Object> buildParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("query", getQuery());
    params.put("customInstructions", formatCustomInstructions());
    return params;
  }

  @Override
  protected JsonSchema generateJsonSchema() {
    JsonSchemaElement selectionSchema = JsonStringSchema.builder().description("Extracted value").build();
    JsonSchemaElement selectionListSchema = JsonArraySchema.builder().items(selectionSchema)
        .description("List of extracted values").build();
    JsonSchemaElement rootSchema = JsonObjectSchema.builder().description("The result of the string extraction process")
        .addProperty(RESULT, selectionListSchema).required(RESULT).build();

    return JsonSchema.builder().name("ExtractionResult").rootElement(rootSchema).build();
  }

  @Override
  protected List<String> parseJsonResponse(String jsonResponse) {
    try {
      JsonNode rootNode = BusinessEntityConverter.getObjectMapper().readTree(jsonResponse);
      ArrayNode resultNode = (ArrayNode) rootNode.get(RESULT);

      List<String> result = new ArrayList<>();
      if (resultNode != null) {
        resultNode.iterator().forEachRemaining(node -> {
          result.add(node.asText());
        });
        return result;
      }
    } catch (Exception e) {
      Ivy.log().error(e);
      return null;
    }
    return null;
  }

  @Override
  protected String getTemplate() {
    return TEMPLATE;
  }

  // Builder class for ListStringExtractor
  public static class Builder extends AiFunction.Builder<AiOption, List<String>, ListStringExtractor> {
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
    public ListStringExtractor build() {
      ListStringExtractor extractor = new ListStringExtractor();
      extractor.setConnector(connector);
      extractor.setQuery(query);
      extractor.setCustomInstructions(customInstructions);
      return extractor;
    }
  }
}
