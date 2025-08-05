package com.axonivy.utils.ai.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.core.log.ExecutionLogger;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.enums.log.LogLevel;
import com.axonivy.utils.ai.enums.log.LogPhase;
import com.axonivy.utils.ai.exception.AiException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.input.PromptTemplate;

@JsonInclude(value = Include.NON_EMPTY)
public class AiStep implements Serializable {

  /** Template for logging step metadata */
  private static final String LOG_HEADER_TEMPLATE = """
      Step No: {{stepNo}}
      Tool Name: {{toolName}}
      """;

  private static final long serialVersionUID = -7596426640750339316L;

  private Integer stepNo; // The order number of the step in the workflow
  private String toolSignature; // Signature of the tool configured for this step

  @JsonIgnore
  private IvyTool tool; // Tool to be executed during this step

  // Default constructor
  public AiStep() {
  }

  /**
   * Configures this step to use a specific AI tool. Tool name is automatically
   * copied from the tool instance.
   *
   * @param tool the {@link IvyTool} instance to use
   */
  public void useTool(IvyTool tool) {
    this.tool = tool;
    this.setToolSignature(tool.getSignature());
  }

  /**
   * Executes the current step by calling its configured tool.
   * <p>
   * In case of exceptions, logs the error and returns an error-state variable.
   *
   * @param aiVariables    the input variables for the tool
   * @param connector      the connector used to access LLM or external services
   * @param logger         the logger to record step execution
   * @param iterationCount current iteration count (for retry or looped flows)
   * @return a list of resulting {@link AiVariable}, or a single error variable
   */
  public List<AiVariable> run(List<AiVariable> aiVariables, AbstractAiServiceConnector connector,
      ExecutionLogger logger, int iterationCount) {

    // Log step metadata
    logger.log(LogLevel.STEP, LogPhase.INIT, generateLogEntryContent(), StringUtils.EMPTY, iterationCount);
    try {
      // Configure and run the tool
      tool.setConnector(connector);
      return tool.execute(aiVariables, logger, iterationCount);

    } catch (AiException | JsonProcessingException e) {
      // Log error to Ivy runtime logs
      Ivy.log().error(e);
      // Return variable in error state
      return Arrays.asList(generateErrorResult(e));
    }
  }

  /**
   * Adds variables to the current tool instance, if the list is not empty.
   *
   * @param variables list of {@link AiVariable}s to pass to the tool
   */
  public void addVariables(List<AiVariable> variables) {
    if (CollectionUtils.isEmpty(variables)) {
      return;
    }
    tool.setVariables(variables);
  }

  /**
   * Generates content for logging based on the configured template. Includes step
   * number and tool name.
   *
   * @return formatted log content
   */
  public String generateLogEntryContent() {
    Map<String, Object> params = new HashMap<>();
    params.put("stepNo", Optional.ofNullable(stepNo.toString()).orElse(StringUtils.EMPTY));
    params.put("toolName", Optional.ofNullable(tool).map(IvyTool::getName).orElse(StringUtils.EMPTY));

    return PromptTemplate.from(LOG_HEADER_TEMPLATE).apply(params).text();
  }

  /**
   * Generates a variable marked in error state with exception details.
   *
   * @param e the exception encountered during step execution
   * @return an {@link AiVariable} with error state and content
   */
  private AiVariable generateErrorResult(Exception e) {
    AiVariable result = new AiVariable();
    result.setState(AiVariableState.ERROR);
    result.setErrorContent(
        Optional.ofNullable(e).map(Exception::getCause).map(Throwable::toString).orElse(StringUtils.EMPTY));
    return result;
  }

  public Integer getStepNo() {
    return stepNo;
  }

  public void setStepNo(Integer stepNo) {
    this.stepNo = stepNo;
  }

  public IvyTool getTool() {
    return tool;
  }

  public void setTool(IvyTool tool) {
    this.tool = tool;
  }

  public String getToolSignature() {
    return toolSignature;
  }

  public void setToolSignature(String toolSignature) {
    this.toolSignature = toolSignature;
  }
}