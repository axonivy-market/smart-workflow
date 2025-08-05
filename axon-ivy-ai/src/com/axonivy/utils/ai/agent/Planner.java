package com.axonivy.utils.ai.agent;

import java.util.List;

import com.axonivy.utils.ai.core.AiStep;
import com.axonivy.utils.ai.core.ReActDecision;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface Planner {

  @UserMessage("{{planInput}}")
  public List<AiStep> createPlan(@V("planInput") String planInput);

  @UserMessage("{{observationInput}}")
  public ReActDecision makeDecision(@V("observationInput") String observationInput);
}
