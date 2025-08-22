package com.axonivy.utils.ai.axon.ivy.ai.demo.agent;

import java.util.List;

import com.axonivy.utils.ai.axon.ivy.ai.demo.dto.SupportTicket;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;

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
