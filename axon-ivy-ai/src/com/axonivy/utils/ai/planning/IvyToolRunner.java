package com.axonivy.utils.ai.planning;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.tools.IvySubProcessToolsProvider;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class IvyToolRunner {
  private ChatModel model;
  private ChatMemoryProvider memoryProvider;
  private String memoryId;
  private IvySubProcessToolsProvider toolProvider;

  public IvyToolRunner(ChatModel model, ChatMemoryProvider memoryProvider, String memoryId,
      IvySubProcessToolsProvider toolProvider) {
    this.model = model;
    this.memoryProvider = memoryProvider;
    this.memoryId = memoryId;
    this.toolProvider = toolProvider;
  }

  public IvyToolRunner(ChatMemoryProvider memoryProvider, String memoryId,
      IvySubProcessToolsProvider toolProvider) {
    this.memoryProvider = memoryProvider;
    this.memoryId = memoryId;
    this.toolProvider = toolProvider;
  }

  public void run(String message, int maxIteration) {
    if (model == null) {
      buildDefaultModel();
    }

    IvyToolExecutor runner = AiServices.builder(IvyToolExecutor.class).chatModel(model)
        .chatMemoryProvider(memoryProvider)
        .toolProvider(toolProvider).maxSequentialToolsInvocations(maxIteration).build();

    runner.run(memoryId, message);
  }

  private void buildDefaultModel() {
    this.model = OpenAiServiceConnector.buildJsonOpenAiModel().build();
  }

  public interface IvyToolExecutor {

    @UserMessage("{{message}}")
    public String run(@MemoryId String memoryId, @V("message") String message);
  }
}
