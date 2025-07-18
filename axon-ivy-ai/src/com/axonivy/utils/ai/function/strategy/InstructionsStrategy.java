package com.axonivy.utils.ai.function.strategy;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.axonivy.utils.ai.utils.StringProcessingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.input.PromptTemplate;

/**
 * Strategy implementation for object-based extraction using JSON object
 * templates.
 */
public class InstructionsStrategy implements InstructionBuilder, ResultProcessor {
    
    private static final String INSTRUCTION_TEMPLATE = """
        Understand the content and update the fields in the following JSON object based on the information you extract:

        {{contextObject}}

        Return exactly this JSON object with its fields updated/filled based on the content you analyzed. Do not change the structure, only update the field values.
        {{asListInstruction}}
        """;

    private static final String AS_LIST_INSTRUCTION = """
        The result MUST be a JSON array where each element is the above object structure with updated field values.
        """;
    
    private final Boolean asList;
    
    public InstructionsStrategy(Boolean asList) {
        this.asList = asList;
    }
    
    @Override
    public String buildInstructions(Object context) {
      // Serialize the target object to JSON as a template to be updated
        ObjectMapper mapper = BusinessEntityConverter.getObjectMapper();
        try {
          String jsonTemplate = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
          Ivy.log().info("Using JSON object template to be updated:");
          Ivy.log().info(jsonTemplate);

          // Build the function instructions
          Map<String, Object> params = new HashMap<>();
          params.put("contextObject", jsonTemplate);
          params.put("asListInstruction", BooleanUtils.isTrue(asList) ? AS_LIST_INSTRUCTION : StringUtils.EMPTY);
          return PromptTemplate.from(INSTRUCTION_TEMPLATE).apply(params).text();
        } catch (JsonProcessingException e) {
          Ivy.log().error("Error when serializing context object to JSON template", e);
          return StringUtils.EMPTY;
        }
    }
    
    @Override
    public String processResult(String rawResult) {
        // For instructions strategy, we expect clean JSON without wrappers
        // Just trim whitespace and do minimal processing
        String cleanResult = rawResult.trim();
        
        // Use StringProcessingUtils to handle any remaining JSON formatting issues
        return StringProcessingUtils.standardizeResult(cleanResult, false);
    }
} 