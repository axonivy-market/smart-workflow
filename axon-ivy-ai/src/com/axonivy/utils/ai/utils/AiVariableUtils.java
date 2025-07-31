package com.axonivy.utils.ai.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.IvyToolParameter;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.enums.InstructionType;
import com.axonivy.utils.ai.function.AiVariableExtractor;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

public final class AiVariableUtils {

  private static final String VARIABLE_FORMAT = """
      Variable %d
      ID: %s
      Name: %s
      Description: %s
      """;

  private static final String VARIABLE_JSON_FORMAT = """
      Variable %d
      %s
      """;

  /**
   * Adds the given AiVariable to the list or replaces the existing one if a
   * variable with the same name already exists.
   *
   * @param newVar  the variable to add or replace (must have non-blank name and
   *                className)
   * @param varList the list to modify (must not be null)
   */
  public static List<AiVariable> addOrReplaceVariable(AiVariable newVar, List<AiVariable> varList) {
    // If the input variable is null or has blank fields, do nothing
    if (StringUtils.isBlank(Optional.ofNullable(newVar).map(AiVariable::getParameter).map(IvyToolParameter::getName)
        .orElse(StringUtils.EMPTY))) {
      return varList;
    }

    // If the input list is null, create a new one and add the variable
    if (varList == null) {
      varList = new ArrayList<>();
      varList.add(newVar);
      return varList;
    }

    // Find if a variable with the same name exists; replace if found
    for (int i = 0; i < varList.size(); i++) {
      if (newVar.getParameter().getName().equals(varList.get(i).getParameter().getName())) {
        varList.set(i, newVar);
        return varList;
      }
    }

    // Not found — add new
    varList.add(newVar);
    return varList;
  }

  /**
   * Adds or replaces multiple AiVariables into the target list. Reuses
   * addOrReplaceVariable for each item to ensure consistent behavior. If varList
   * is null, a new list is created. Invalid AiVariable entries (null, blank name,
   * blank className) are ignored.
   *
   * @param newVars list of new variables to add or replace (can be null or empty)
   * @param varList existing list to update (can be null)
   * @return the updated list (never null if newVars is valid)
   */
  public static List<AiVariable> addOrReplaceVariables(List<AiVariable> newVars, List<AiVariable> varList) {
    // If there are no new variables, return the original list as-is
    if (CollectionUtils.isEmpty(newVars)) {
      return varList;
    }

    // Initialize the list if it's null
    if (varList == null) {
      varList = new ArrayList<>();
    }

    // Call the single-variable method for each entry in newVars
    for (AiVariable newVar : newVars) {
      varList = addOrReplaceVariable(newVar, varList);
    }

    return varList;
  }

  /**
   * Converts a Map<String, Object> into a list of AiVariable objects. Skips any
   * entry with a blank key or null value.
   *
   * @param map the input map to convert
   * @return a list of AiVariable objects representing the map entries
   */
  public static List<AiVariable> convertMapToVariableList(Map<String, Object> map) {
    // Initialize the result list
    List<AiVariable> varList = new ArrayList<>();

    // Return empty list if map is null or empty
    if (map == null || map.isEmpty()) {
      return varList;
    }

    // Iterate through each map entry
    for (Entry<String, Object> entry : map.entrySet()) {

      // Skip entries with null values or blank keys
      if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
        continue;
      }

      // Create a new AiVariable
      AiVariable variable = new AiVariable();
      variable.init();
      variable.getParameter().setName(entry.getKey());
      String val = (entry.getValue() instanceof String) ? (String) entry.getValue()
          : BusinessEntityConverter.entityToJsonValue(entry.getValue());
      variable.getParameter().setValue(val);

      // Add the variable to the result list
      varList.add(variable);
    }

    // Return the populated list
    return varList;
  }

  public static String convertAiVariablesToString(List<AiVariable> variables) {
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

  public static String convertAiVariablesToJsonString(List<AiVariable> variables) {
    String variablesStr = StringUtils.EMPTY;
    if (CollectionUtils.isNotEmpty(variables)) {
      for (int i = 0; i < variables.size(); i++) {
        variablesStr += String.format(VARIABLE_JSON_FORMAT, i + 1,
            BusinessEntityConverter.entityToJsonNode(variables.get(i)).toString());
      }
    }
    return variablesStr;
  }

  public static List<AiVariable> extractOutputAiVariables(List<Instruction> instructions, List<AiVariable> variables,
      AbstractAiServiceConnector connector) {
    AiVariableExtractor extractor = AiVariableExtractor.getBuilder()
        .addInstructions(
            instructions.stream().filter(instruction -> InstructionType.EXTRACT_OUTPUT == instruction.getType())
                .collect(Collectors.toList()))
        .addVariables(variables).useService(connector)
        .build();
    extractor.execute();
    return extractor.getExtracted();
  }

  public static List<AiVariable> extractInputAiVariables(List<Instruction> instructions, List<AiVariable> variables,
      AbstractAiServiceConnector connector) {
    AiVariableExtractor extractor = AiVariableExtractor.getBuilder()
        .addInstructions(instructions.stream()
            .filter(instruction -> InstructionType.EXTRACT_INPUT == instruction.getType()).collect(Collectors.toList()))
        .addVariables(variables).useService(connector).build();
    extractor.execute();
    return extractor.getExtracted();
  }

  public static List<AiVariable> extractExecutionAiVariables(List<Instruction> instructions, List<AiVariable> variables,
      AbstractAiServiceConnector connector) {
    AiVariableExtractor extractor = AiVariableExtractor.getBuilder()
        .addInstructions(instructions.stream()
            .filter(instruction -> InstructionType.EXECUTION == instruction.getType()).collect(Collectors.toList()))
        .addVariables(variables).useService(connector).build();
    extractor.execute();
    return extractor.getExtracted();
  }
}