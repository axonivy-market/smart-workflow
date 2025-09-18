package com.axonivy.utils.smart.workflow.demo.agent;

import java.util.List;

import com.axonivy.utils.smart.workflow.connector.OpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.demo.dto.AxonIvySupportTicket;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public final class AxonIvySupportTicketAgent {
  public static AxonIvySupportTicket createAxonIvySupportTicket(String message, List<String> instructions) {
    IAxonIvySupportTicketAgent agent = AiServices.builder(IAxonIvySupportTicketAgent.class)
        .chatModel(OpenAiServiceConnector.buildJsonOpenAiModel().build()).build();
    return agent.createSupportTicketWithoutApprovalInfo(message, instructions);
  }

  private interface IAxonIvySupportTicketAgent {
    @SystemMessage("""
        Instruction to follow:
        - Don't fill information related to approval
        {{instructions}}
        """)
    @UserMessage("{{message}}")
    public AxonIvySupportTicket createSupportTicketWithoutApprovalInfo(@V("message") String message,
        @V("instructions") List<String> instructions);
  }
}
