package com.axonivy.utils.ai.core.tool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.core.log.ExecutionLogger;
import com.axonivy.utils.ai.dto.IvyToolParameter;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.enums.InstructionType;
import com.axonivy.utils.ai.enums.log.LogLevel;
import com.axonivy.utils.ai.enums.log.LogPhase;
import com.axonivy.utils.ai.service.IvyAdapterService;
import com.axonivy.utils.ai.utils.AiVariableUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;

/**
 * Represents a specific type of tool used for integrating with Ivy Workflow
 * system. This tool executes a process in Ivy and retrieves the result, using
 * input variables and process parameters.
 */
public class IvyTool implements Serializable {

  private static final long serialVersionUID = -4175428864013818110L;

  // Unique identifier for this tool instance
  private String id;

  // Human-readable name for the tool
  private String name;

  // Description of the tool's purpose or usage
  private String usage;

  // A list of step-specific instructions used to guide the tool's behavior.
  // This list is send by other agents during runtime.
  private List<Instruction> instructions;

  @JsonIgnore
  private List<AiVariable> variables;

  // The Axon Ivy process ID
  private String signature;

// Process parameter definitions
  private List<IvyToolParameter> parameters;

  // Process result definitions
  private List<IvyToolParameter> resultDefinitions;

  // The connector to AI service
  @JsonIgnore
  private AbstractAiServiceConnector connector;

  // Result after run the tool. This string will be used as the input for adaptive
  // planning
  @JsonIgnore
  private String aiResult;

  // Missing parameters
  @JsonIgnore
  private List<AiVariable> missingParameters;

  @SuppressWarnings("restriction")
  @JsonIgnore
  private CallSubStart ivyProcess;

  /**
   * Executes the Ivy process using the provided signature and input variables.
   * The result of the process execution is set as the result of this tool.
   * 
   * @throws JsonProcessingException
   */
  @SuppressWarnings("restriction")
  public List<AiVariable> execute(List<AiVariable> inputVariables, ExecutionLogger logger, int iterationCount)
      throws JsonProcessingException {

    logInit(logger, iterationCount);
    
    List<Instruction> inputInstructions = new ArrayList<>();

    for(var paramDesc : ivyProcess.getSignature().getInputParameters()) {
      Instruction extractInstruction = new Instruction();
      extractInstruction.setType(InstructionType.EXTRACT_INPUT);
      extractInstruction.setToolName(name);
      extractInstruction.setContent(paramDesc.getInfo().getDescription());
      inputInstructions.add(extractInstruction);
    }

    // convert AI variables to process parameters
    variables = AiVariableUtils.extractInputAiVariables(inputInstructions, inputVariables, getConnector());

    // Use AI to fulfill parameter
    // fullfilIvyTool();

    // Use name to fulfill parameter
    fulfillIvyToolUsingName();

    // check missing inputs
    missingParameters = new ArrayList<>();
    for (var param : parameters) {
      if (param.getValue() == null) {
        AiVariable missingVariable = new AiVariable(param.getDefinition());
        missingVariable.setState(AiVariableState.ERROR);
        missingParameters.add(missingVariable);
      }
    }

    Map<String, Object> processParams = new HashMap<>();
    for (var param : getParameters()) {
      processParams.put(param.getDefinition().getName(), param.getValue());
    }

    // Handle missing parameters
    if (CollectionUtils.isNotEmpty(missingParameters)) {
      // Log missing parameters
      logMissingInputVariables(logger, iterationCount);

      // Set the result to null since the tool isn't run
      return null;
    }

    // Log extracted variables
    logInputVariables(logger, iterationCount);

    // Call the callable subprocess (the tool)
    String signatureToStart = signature.replaceAll("java.lang.String", "String");
    Map<String, Object> processResult = IvyAdapterService.startSubProcessInApplication(signatureToStart, processParams);

    // The tool will return 2 kind of results:
    // "aiResult" : A string in natural language that will be use to evaluate next step
    // Other objects: result variables of the tool

    // Get AI result
    aiResult = Optional.ofNullable(processResult.get("aiResult")).map(obj -> (String) obj).orElse(null);

    // Extracting result variables
    // If there is no predefined result definition, skip extracting result, return
    // an empty list
    List<AiVariable> results = new ArrayList<>();
    if (CollectionUtils.isEmpty(resultDefinitions)) {
      return results;
    }

    // If no result, assume there was an error during execution
    if (processResult == null || processResult.size() == 0) {
      // Set a default error result if no result is returned
      AiVariable error = new AiVariable();
      error.setState(AiVariableState.ERROR);
      error.setErrorContent("Error happened when running the Ivy tool: " + getName());
      results.add(error);
      logError(logger, error, iterationCount);
      return results;
    }

    // Retrieve results and convert to AiVariable
    for (var resultDefinition : resultDefinitions) {
      Object resultObj = processResult.entrySet().stream()
          .filter(r -> r.getKey().equals(resultDefinition.getDefinition().getName()))
          .map(Entry::getValue).findFirst().orElse(null);

      if (Objects.nonNull(resultObj)) {
        AiVariable newVar = new AiVariable(resultDefinition.getDefinition());
        newVar.getParameter().setValue(resultObj);
        newVar.setState(AiVariableState.SUCCESS);
        results.add(newVar);
      }
    }
    logOutputVariables(logger, results, iterationCount);
    return results;
  }

  /**
   * Fulfills each {@link IvyToolParameter} in the current step by assigning a
   * matching value from the list of input {@link AiVariable}s.
   * <p>
   * A match is determined by comparing both the parameter name and its fully
   * qualified type. If a matching variable is found, its value is assigned to the
   * tool parameter.
   */
  @SuppressWarnings("restriction")
  private void fulfillIvyToolUsingName() {
    if (CollectionUtils.isNotEmpty(parameters)) {
      for (IvyToolParameter param : parameters) {
        // Define matching condition: name and type must be equal
        Predicate<AiVariable> matchByNameAndType = variable -> 
          variable.getParameter().getDefinition().getName().equals(param.getDefinition().getName())
            && variable.getParameter().getDefinition().getType().fullQualifiedName()
                .equals(param.getDefinition().getType().fullQualifiedName());

          // Search for the first variable that matches the parameter by name and type
          AiVariable matchedValue = variables.stream().filter(matchByNameAndType).findFirst().orElse(null);

          // Set the matched value to the parameter
          param.setValue(Optional.ofNullable(matchedValue).map(AiVariable::getParameter).map(IvyToolParameter::getValue)
              .orElse(null));
      }
    }
  }

  private String generateLogHeaderString() {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("Tool Id: %s", id));
    builder.append(System.lineSeparator());
    builder.append(String.format("Tool Name: %s", name));
    return builder.toString();
  }

  private void logInit(ExecutionLogger logger, int iterationCount) {
    logger.log(LogLevel.TOOL, LogPhase.INIT, generateLogHeaderString(), StringUtils.EMPTY, iterationCount);
  }

  private void logInputVariables(ExecutionLogger logger, int iterationCount) {
    StringBuilder builder = new StringBuilder();
    builder.append("Input variables:");
    builder.append(System.lineSeparator());
    if (CollectionUtils.isEmpty(variables)) {
      builder.append("None");
    } else {
      for (AiVariable variable : variables) {
        builder.append(variable.toPrettyString());
        builder.append(System.lineSeparator());
      }
    }

    logger.log(LogLevel.TOOL, LogPhase.RUNNING, generateLogHeaderString(), builder.toString(), iterationCount);
  }

  private void logMissingInputVariables(ExecutionLogger logger, int iterationCount) {
    StringBuilder builder = new StringBuilder();
    builder.append("Missing parameters:");
    builder.append(System.lineSeparator());
    for (AiVariable missingParam : missingParameters) {
      builder.append(missingParam.toPrettyString());
      builder.append(System.lineSeparator());
    }

    logger.log(LogLevel.TOOL, LogPhase.ERROR, generateLogHeaderString(), builder.toString(), iterationCount);
  }

  private void logOutputVariables(ExecutionLogger logger, List<AiVariable> results, int iterationCount) {
    StringBuilder builder = new StringBuilder();
    builder.append("Result:");
    builder.append(System.lineSeparator());
    if (CollectionUtils.isEmpty(results)) {
      builder.append("None");
    } else {
      for (AiVariable variable : results) {
        builder.append(variable.toPrettyString());
        builder.append(System.lineSeparator());
      }
    }

    logger.log(LogLevel.TOOL, LogPhase.COMPLETE, generateLogHeaderString(), builder.toString(), iterationCount);
  }

  private void logError(ExecutionLogger logger, AiVariable error, int iterationCount) {
    StringBuilder builder = new StringBuilder();
    builder.append("Error:");
    builder.append(System.lineSeparator());
    builder.append(error.toPrettyString());
    logger.log(LogLevel.TOOL, LogPhase.ERROR, generateLogHeaderString(), builder.toString(), iterationCount);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsage() {
    return usage;
  }

  public void setUsage(String usage) {
    this.usage = usage;
  }

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

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public List<IvyToolParameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<IvyToolParameter> parameters) {
    this.parameters = parameters;
  }

  public AbstractAiServiceConnector getConnector() {
    return connector;
  }

  public void setConnector(AbstractAiServiceConnector connector) {
    this.connector = connector;
  }

  public List<IvyToolParameter> getResultDefinitions() {
    return resultDefinitions;
  }

  public void setResultDefinitions(List<IvyToolParameter> results) {
    this.resultDefinitions = results;
  }

  public String getAiResult() {
    return aiResult;
  }

  public void setAiResult(String aiResult) {
    this.aiResult = aiResult;
  }

  public List<AiVariable> getMissingParameters() {
    return missingParameters;
  }

  public void setMissingParameters(List<AiVariable> missingParameters) {
    this.missingParameters = missingParameters;
  }

  @SuppressWarnings("restriction")
  public CallSubStart getIvyProcess() {
    return ivyProcess;
  }

  @SuppressWarnings("restriction")
  public void setIvyProcess(CallSubStart ivyProcess) {
    this.ivyProcess = ivyProcess;
  }
}