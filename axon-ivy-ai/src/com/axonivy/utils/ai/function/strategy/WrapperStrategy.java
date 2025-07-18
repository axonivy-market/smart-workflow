package com.axonivy.utils.ai.function.strategy;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.utils.StringProcessingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.model.input.PromptTemplate;

/**
 * Strategy implementation for wrapper-based extraction using << >> delimiters.
 */
public class WrapperStrategy implements InstructionBuilder, ResultProcessor {
    
    private static final String INSTRUCTION_TEMPLATE = """
        JSON object:
            {{object}}

            + Analyze the query, find corresponding attributes to fill
            + don't change the text from the query, don't generate your own text
            + The result MUST be the fulfilled JSON object, not the updated parts
            {{asListInstruction}}
        """;

    private static final String AS_LIST_INSTRUCTION = """
            + The result MUST be a list of JSON objects above
        """;
    
    private final Boolean asList;
    
    public WrapperStrategy(Boolean asList) {
        this.asList = asList;
    }
    
    @Override
    public String buildInstructions(Object context) {
        // Convert object to JSON
        String convertedJson = convertObjectToJson(context);

        // Build the function instructions
        Map<String, Object> params = new HashMap<>();
        params.put("object", convertedJson);
        params.put("asListInstruction", BooleanUtils.isTrue(asList) ? AS_LIST_INSTRUCTION : StringUtils.EMPTY);
        return PromptTemplate.from(INSTRUCTION_TEMPLATE).apply(params).text();
    }
    
    @Override
    public String processResult(String rawResult) {
        // Extract the JSON from the wrapper (<< >>)
        String standardResult = StringProcessingUtils.standardizeResult(rawResult, true);
        
        // Use StringProcessingUtils to check and fix JSON if needed
        return StringProcessingUtils.standardizeResult(standardResult, true);
    }
    
    /**
     * Convert the targetObject to JSON string using Jackson
     */
    private String convertObjectToJson(Object targetObject) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(targetObject);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
} 