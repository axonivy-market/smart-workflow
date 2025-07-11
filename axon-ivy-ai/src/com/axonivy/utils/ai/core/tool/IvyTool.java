package com.axonivy.utils.ai.core.tool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.IvyToolParameter;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.exception.AiException;
import com.axonivy.utils.ai.function.DataMapping;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.axonivy.utils.ai.service.IvyAdapterService;
import com.axonivy.utils.ai.utils.AiVariableUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import dev.langchain4j.model.input.PromptTemplate;

/**
 * Represents a specific type of tool used for integrating with Ivy Workflow
 * system. This tool executes a process in Ivy and retrieves the result, using
 * input variables and process parameters.
 */
public class IvyTool implements Serializable {

  private static final long serialVersionUID = -4175428864013818110L;

  private static final String VARIABLES_TEMPLATE = """
      Provided variables list:

          {{variables}}
      """;

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

  @JsonIgnore
  private AbstractAiServiceConnector connector;

  /**
   * Executes the Ivy process using the provided signature and input variables.
   * The result of the process execution is set as the result of this tool.
   * 
   * @throws JsonProcessingException
   */
  public List<AiVariable> execute(List<AiVariable> inputVariables) throws JsonProcessingException {
    // convert AI variables to process parameters
    variables = AiVariableUtils.extractInputAiVariables(instructions, inputVariables, getConnector());

    // Use AI to fulfill parameter
    // fullfilIvyTool();

    // Use name to fulfill parameter
    fulfillIvyToolUsingName();

    Map<String, Object> processParams = new HashMap<>();
    for (var param : getParameters()) {
      processParams.put(param.getName(), param.getValue());
    }

    Map<String, Object> processResult = IvyAdapterService.startSubProcessInApplication(signature, processParams);
    List<AiVariable> results = new ArrayList<>();

    // If no result, assume there was an error during execution
    if (processResult == null || processResult.size() == 0) {
      // Set a default error result if no result is returned
      AiVariable error = new AiVariable();
      error.setState(AiVariableState.ERROR);
      error.setContent("Error happened when running the Ivy tool: " + getName());
      error.setName("Error " + getName());
      results.add(error);
      return results;
    }

    // Retrieve results and convert to AiVariable
    for (Entry<String, Object> r : processResult.entrySet()) {
      if (r.getValue() instanceof AiVariable) {
        results.add((AiVariable) r.getValue());
      } else {
        results.add(new AiVariable(r.getKey(), BusinessEntityConverter.entityToJsonValue(r.getValue())));
      }
    }

    return results;
  }

  private void fulfillIvyToolUsingName() {
    if (CollectionUtils.isNotEmpty(parameters)) {
      for (IvyToolParameter param : parameters) {
        param.setValue(variables.stream().filter(variable -> variable.getName().equals(param.getName()))
            .map(AiVariable::getContent).findFirst().orElseGet(() -> StringUtils.EMPTY));
      }
    }
  }

  @JsonIgnore
  public void fullfilIvyTool() throws JsonProcessingException {
    fulfillNormalAttributes();
    fulfillMandatoryParameters();
  }

  private void fulfillNormalAttributes() {
    if (CollectionUtils.isNotEmpty(getParameters())) {

      // Use AI to fulfill value of all mandatory parameters
      getParameters().stream().filter(param -> BooleanUtils.isNotTrue(BooleanUtils.toBoolean(param.getIsMandatory())))
          .forEach(param -> {
            IvyToolParameter newAttribute = fulfillIvyToolParameter(param);
            getParameters().stream().filter(attr -> attr.getName().contentEquals(param.getName())).findFirst().get()
                .setValue(newAttribute.getValue());
          });
    }
  }

  private void fulfillMandatoryParameters() throws JsonProcessingException {
    if (CollectionUtils.isNotEmpty(getParameters())) {
      // Use AI to fulfill value of all mandatory parameters
      getParameters().stream().filter(param -> BooleanUtils.isTrue(BooleanUtils.toBoolean(param.getIsMandatory())))
          .forEach(param -> {
            IvyToolParameter newAttribute = fulfillIvyToolParameter(param);
            getParameters().stream().filter(attr -> attr.getName().contentEquals(param.getName())).findFirst().get()
                .setValue(newAttribute.getValue());
          });
    }
  }

  private IvyToolParameter fulfillIvyToolParameter(IvyToolParameter param) {
    // Clear value of the parameter before fulfill
    param = Optional.ofNullable(param).orElseGet(() -> new IvyToolParameter());

    // Use AI to fulfill parameter
    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("variables", AiVariableUtils.convertAiVariablesToString(variables));
    AiVariable result = DataMapping.getBuilder().useService(getConnector())
        .withQuery(PromptTemplate.from(VARIABLES_TEMPLATE).apply(paramsMap).text()).withTargetObject(param)
        .addFieldExplanations(Arrays.asList(new FieldExplanation(param.getName(), param.getDescription()))).build()
        .execute();

    // If the data mapping process is failed, return the original parameter
    if (result.getState() != AiVariableState.SUCCESS) {
      return param;
    }

    // Otherwise convert the mapped object to an instance of IvyToolParameter
    try {
      return BusinessEntityConverter.jsonValueToEntity(result.getContent(), IvyToolParameter.class);
    } catch (AiException e) {
      // If the conversion is failed, return the original parameter
      return param;
    }
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
}