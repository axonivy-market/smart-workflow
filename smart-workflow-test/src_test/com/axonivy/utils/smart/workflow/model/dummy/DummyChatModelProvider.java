package com.axonivy.utils.smart.workflow.model.dummy;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import dev.langchain4j.data.message.AiMessage;
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

  public static void defineChat(Function<ChatRequest, ChatResponse> chat) {
    DummyChatModel.CHAT = chat;
  }

  public static void defineChatText(Function<ChatRequest, String> chat) {
    DummyChatModel.CHAT = r -> ChatResponse.builder().aiMessage(AiMessage.aiMessage(chat.apply(r))).build();
  }

  private static class DummyChatModel implements ChatModel {

    private static final String DEFAULT_CHAT_RESPONSE_TEMPLATE = "Hey I'm %s. My Smartness is under development.";
    private static final String NOT_IMPLEMENTED = "Not implemented!";
    private static Function<ChatRequest, ChatResponse> CHAT = request -> ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage(DummyChatModel.NOT_IMPLEMENTED)).build();

    private final ModelOptions options;

    public DummyChatModel(ModelOptions options) {
      this.options = options;
    }

    @Override
    public String chat(String userMessage) {
      return String.format(DEFAULT_CHAT_RESPONSE_TEMPLATE, options.modelName());
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
      return CHAT.apply(chatRequest);
    }
  }

}
