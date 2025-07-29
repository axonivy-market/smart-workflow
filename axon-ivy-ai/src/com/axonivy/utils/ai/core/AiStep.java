package com.axonivy.utils.ai.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.log.LogLevel;
import com.axonivy.utils.ai.enums.log.LogPhase;
import com.axonivy.utils.ai.exception.AiException;
import com.axonivy.utils.ai.memory.AgentMessageLogger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.input.PromptTemplate;

/**
 * Represents a single step in an AI workflow. A step can either use a tool
 * (e.g., AI function) or an agent (another set of steps), and is responsible
 * for executing that logic and producing a result.
 */
@JsonInclude(value = Include.NON_EMPTY)
public class AiStep implements Serializable {

  private static final String LOG_HEADER_TEMPLATE = """
      Step No: {{stepNo}}
      Next Step: {{next}}
      Previous Step: {{prev}}
      Tool Id: {{toolId}}
      """;

  // Constant representing the initial step index in the agent workflow
  public static final int INITIAL_STEP = 0;

  // Constant indicating a finalize step, typically used for cleanup or final
  // actions
  public static final int FINALIZE_STEP = -1;

  // Constant for a special step where variables are extracted from results or
  // context
  public static final int EXTRACT_VARIABLES_STEP = -2;

  private static final long serialVersionUID = -7596426640750339316L;

  // Step metadata
  private Integer stepNo;
  private Integer previous;
  private Integer next;
  private String name;
  private String analysis;
  private String toolId;

  @JsonIgnore
  private String runId;

  // Execution targets
  @JsonIgnore
  private IvyTool tool;

  // Default constructor
  public AiStep() {
  }

  /**
   * Configures this step to use a specific AI tool. The step will copy input
   * variables from the tool and register the tool ID.
   */
  public void useTool(IvyTool tool) {
    this.tool = tool;
    this.setToolId(tool.getId());
  }

  /**
   * Executes the step. Exceptions will move the flow to the final step and be
   * logged.
   */
  public List<AiVariable> run(List<AiVariable> aiVariables, AbstractAiServiceConnector connector,
      AgentMessageLogger logger, int iterationCount) {

    // Log the step
    logger.log(LogLevel.STEP, LogPhase.INIT, generateLogEntryContent(), StringUtils.EMPTY, iterationCount);
    try {
      tool.setConnector(connector);
      return tool.execute(aiVariables, logger, iterationCount);

    } catch (AiException | JsonProcessingException e) {
      // If any exception occurs, log and move to finalize step
      Ivy.log().error(e);
      setNext(FINALIZE_STEP);
      return null;
    }
  }

  public void addVariables(List<AiVariable> variables) {
    if (CollectionUtils.isEmpty(variables)) {
      return;
    }
    tool.setVariables(variables);
  }

  public Integer getStepNo() {
    return stepNo;
  }

  public void setStepNo(Integer stepNo) {
    this.stepNo = stepNo;
  }

  public Integer getPrevious() {
    return previous;
  }

  public void setPrevious(Integer previous) {
    this.previous = previous;
  }

  public Integer getNext() {
    return next;
  }

  public void setNext(Integer next) {
    this.next = next;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAnalysis() {
    return analysis;
  }

  public void setAnalysis(String analysis) {
    this.analysis = analysis;
  }

  public IvyTool getTool() {
    return tool;
  }

  public void setTool(IvyTool tool) {
    this.tool = tool;
  }

  public String getToolId() {
    return toolId;
  }

  public void setToolId(String toolId) {
    this.toolId = toolId;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  public String generateLogEntryContent() {
    Map<String, Object> params = new HashMap<>();
    params.put("stepNo", Optional.ofNullable(stepNo.toString()).orElse(StringUtils.EMPTY));
    params.put("next", Optional.ofNullable(next.toString()).orElse(StringUtils.EMPTY));
    params.put("prev", Optional.ofNullable(previous.toString()).orElse(StringUtils.EMPTY));
    params.put("toolId", Optional.ofNullable(tool).map(IvyTool::getId).orElse(StringUtils.EMPTY));

    return PromptTemplate.from(LOG_HEADER_TEMPLATE).apply(params).text();
  }
}