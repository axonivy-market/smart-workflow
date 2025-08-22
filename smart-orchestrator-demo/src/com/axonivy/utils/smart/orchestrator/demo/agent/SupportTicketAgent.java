package com.axonivy.utils.smart.orchestrator.demo.agent;

import java.util.List;

import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector;
import com.axonivy.utils.smart.orchestrator.demo.dto.SupportTicket;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public final class SupportTicketAgent {
  public static SupportTicket createSupportTicketWithoutApprovalInfo(String message, List<String> instructions) {
    ISupportTicketAgent agent = AiServices.builder(ISupportTicketAgent.class)
        .chatModel(OpenAiServiceConnector.buildJsonOpenAiModel().build()).build();
    return agent.createSupportTicketWithoutApprovalInfo(message, instructions);
  }

  private interface ISupportTicketAgent {
    @SystemMessage("""
        Instruction to follow:
        - Don't fill information related to approval
        {{instructions}}
        """)
    @UserMessage("{{message}}")
    public SupportTicket createSupportTicketWithoutApprovalInfo(@V("message") String message,
        @V("instructions") List<String> instructions);
  }
}
