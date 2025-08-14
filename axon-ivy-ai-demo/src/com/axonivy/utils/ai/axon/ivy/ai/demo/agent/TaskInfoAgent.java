package com.axonivy.utils.ai.axon.ivy.ai.demo.agent;

import java.util.List;

import com.axonivy.utils.ai.axon.ivy.ai.demo.dto.TaskInfo;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public final class TaskInfoAgent {

  public static TaskInfo createTaskInfo(String message, List<String> instructions) {
    ITaskInfoAgent agent = AiServices.builder(ITaskInfoAgent.class)
        .chatModel(OpenAiServiceConnector.buildJsonOpenAiModel().build()).build();
    return agent.createTaskInfo(message, instructions);
  }

  private interface ITaskInfoAgent {
    @SystemMessage("""
        Instruction to follow:
        {{instructions}}
        """)
    @UserMessage("{{message}}")
    public TaskInfo createTaskInfo(@V("message") String message, @V("instructions") List<String> instructions);
  }
}
