package com.axonivy.utils.ai.axon.ivy.ai.demo.agent;

import java.util.List;

import com.axonivy.utils.ai.axon.ivy.ai.demo.dto.AiApprovalDecision;
import com.axonivy.utils.ai.axon.ivy.ai.demo.dto.Employee;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class HrTicketApprovalAgent {
  public static AiApprovalDecision createSupportTicketWithoutApprovalInfo(String message, Employee employee,
      List<String> instructions) {
    IHrTicketApprovalAgent agent = AiServices.builder(IHrTicketApprovalAgent.class)
        .chatModel(OpenAiServiceConnector.buildJsonOpenAiModel().build()).build();
    return agent.approve(message, instructions, employee);
  }

  private interface IHrTicketApprovalAgent {
    @SystemMessage("""
        You are acting as the approving manager for a support ticket request.
        Your goal is to make an approval decision based solely on the provided
        user information and additional instructions. Be concise and decisive.

        ---
        User Information:
        {{employee}}

        ---
        Additional Instructions:
        {{instructions}}
        """)
    @UserMessage("{{message}}")
    public AiApprovalDecision approve(@V("message") String message, @V("instructions") List<String> instructions,
        @V("employee") Employee employee);
  }
}
