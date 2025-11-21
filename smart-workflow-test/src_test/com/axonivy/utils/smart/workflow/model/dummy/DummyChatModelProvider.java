package com.axonivy.utils.smart.workflow.model.dummy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

public class DummyChatModelProvider implements ChatModelProvider {

  public static String NAME = "dummy";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public List<String> models() {
    return List.of(ModelNames.GENIOUS, ModelNames.SUPERMAN, ModelNames.CLASSIC);
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    return new DummyChatModel(options);
  }

  public interface ModelNames {
    String GENIOUS = "Genious";
    String SUPERMAN = "Superman";
    String CLASSIC = "Classic";
  }

  private static class DummyChatModel implements ChatModel {

    private final ModelOptions options;

    public DummyChatModel(ModelOptions options) {
      this.options = options;
    }

    @Override
    public String chat(String userMessage) {
      return "Hey I'm " + options.modelName() + ". My Smartness is under development.";
    }

    @Override
    public Set<Capability> supportedCapabilities() {
      if (options.structuredOutput()) {
        return Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
      }
      return Set.of();
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
      UserMessage lastMessage = (UserMessage) Optional.ofNullable(chatRequest).map(ChatRequest::messages)
          .map(List::getLast)
          .orElse(new UserMessage("Test"));
      AiMessage result = "One day Reto Weiss and Bruno Bütler had a gread idea.".equals(lastMessage.singleText())
          ? AiMessage.aiMessage("The Spark of Innovation")
          : AiMessage.aiMessage("Not implemented!");

      return ChatResponse.builder().aiMessage(result).build();
    }
  }

}
