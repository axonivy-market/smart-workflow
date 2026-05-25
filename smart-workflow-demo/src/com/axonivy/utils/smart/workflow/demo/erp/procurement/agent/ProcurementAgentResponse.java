package com.axonivy.utils.smart.workflow.demo.erp.procurement.agent;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.agent.feedback.AgentFeedback;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.ProcurementRequest;

public class ProcurementAgentResponse {

  @Description("The response from the agent")
  private String result;

  @Description("List of feedbacks from the agent, such as success or error details.")
  private List<AgentFeedback> feedbackList;

  @Description("The updated procurement request, populated when the request was modified.")
  private ProcurementRequest request;

  public String getResult() { return result; }
  public void setResult(String result) { this.result = result; }

  public List<AgentFeedback> getFeedbackList() { return feedbackList; }
  public void setFeedbackList(List<AgentFeedback> feedbackList) { this.feedbackList = feedbackList; }

  public ProcurementRequest getRequest() { return request; }
  public void setRequest(ProcurementRequest request) { this.request = request; }
}
