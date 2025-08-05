package com.axonivy.utils.ai.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.IvyToolParameter;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.enums.InstructionType;
import com.axonivy.utils.ai.function.ListStringExtractor;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.process.model.value.scripting.VariableInfo;

public final class AiVariableUtils {

  private static final String VARIABLE_FORMAT = """
      Variable %d
        - ID: %s
        - Name: %s
        - Description: %s
      """;

  private static final String VARIABLE_JSON_FORMAT = """
      Variable %d
      %s
      """;

  private static List<AiVariable> extractAiVariables(InstructionType instructionType, List<Instruction> instructions,
      List<AiVariable> variables, AbstractAiServiceConnector connector) {
    if (CollectionUtils.isEmpty(instructions)) {
      return variables;
    }

    // Convert current variables to JSON string

    String variablesStr = convertAiVariablesToString(variables);

    // Prepare data mapping using AI service
    ListStringExtractor.Builder builder = ListStringExtractor.getBuilder().useService(connector).withQuery(variablesStr)
        .addCustomInstruction("ONLY extract ID of selected variables");

    // Add instructions to the AI service
    Optional.ofNullable(instructions).orElseGet(() -> new ArrayList<>()).stream()
        .filter(instruction -> instruction.getType() == instructionType).map(Instruction::getContent)
        .map(content -> String.format("Condition to choose variable: %s", content))
        .forEach(builder::addCustomInstruction);

    // Execute the AI function
    List<String> results = builder.build().execute();

    if (CollectionUtils.isNotEmpty(results)) {
      return variables.stream().filter(variable -> results.contains(variable.getId())).collect(Collectors.toList());
    }

    return new ArrayList<>();
  }

  @SuppressWarnings("restriction")
  public static String convertAiVariablesToString(List<AiVariable> variables) {
    String variablesStr = StringUtils.EMPTY;
    if (CollectionUtils.isNotEmpty(variables)) {
      for (int i = 0; i < variables.size(); i++) {
        Optional<AiVariable> current = Optional.ofNullable(variables.get(i));
        variablesStr += String.format(VARIABLE_FORMAT, i + 1, current.map(AiVariable::getId).orElse(StringUtils.EMPTY),
            current.map(AiVariable::getParameter).map(IvyToolParameter::getDefinition).map(VariableDesc::getName)
                .orElse(StringUtils.EMPTY),
            current.map(AiVariable::getParameter).map(IvyToolParameter::getDefinition).map(VariableDesc::getInfo)
                .map(VariableInfo::getDescription).orElse(StringUtils.EMPTY));
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
    return extractAiVariables(InstructionType.EXTRACT_OUTPUT, instructions, variables, connector);
  }

  public static List<AiVariable> extractInputAiVariables(List<Instruction> instructions, List<AiVariable> variables,
      AbstractAiServiceConnector connector) {
    return extractAiVariables(InstructionType.EXTRACT_INPUT, instructions, variables, connector);
  }

  public static List<AiVariable> extractExecutionAiVariables(List<Instruction> instructions, List<AiVariable> variables,
      AbstractAiServiceConnector connector) {
    return extractAiVariables(InstructionType.EXECUTION, instructions, variables, connector);
  }
}