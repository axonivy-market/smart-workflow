package com.axonivy.utils.ai.agent;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.tools.IvySubProcessToolsProvider;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class IvyToolRunner {
  private OpenAiChatModel model;
  private ChatMemoryProvider memoryProvider;
  private String memoryId;
  private IvySubProcessToolsProvider toolProvider;

  public IvyToolRunner(OpenAiChatModel model, ChatMemoryProvider memoryProvider, String memoryId,
      IvySubProcessToolsProvider toolProvider) {
    this.model = model;
    this.memoryProvider = memoryProvider;
    this.memoryId = memoryId;
    this.toolProvider = toolProvider;
  }

  public IvyToolRunner(ChatMemoryProvider memoryProvider, String memoryId,
      IvySubProcessToolsProvider toolProvider) {
    buildDefaultModel();
    this.memoryProvider = memoryProvider;
    this.memoryId = memoryId;
    this.toolProvider = toolProvider;
  }

  public void run(String message, int maxIteration) {
    IIVyToolRunner runner = AiServices.builder(IIVyToolRunner.class).chatModel(model).chatMemoryProvider(memoryProvider)
        .toolProvider(toolProvider).maxSequentialToolsInvocations(maxIteration).build();

    runner.run(memoryId, message);
  }

  private void buildDefaultModel() {
    this.model = OpenAiServiceConnector.buildJsonOpenAiModel().build();
  }

  public interface IIVyToolRunner {

    @UserMessage("{{message}}")
    public String run(@MemoryId String memoryId, @V("message") String message);
  }
}
