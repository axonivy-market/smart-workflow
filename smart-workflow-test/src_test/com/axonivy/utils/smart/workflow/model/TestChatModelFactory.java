package com.axonivy.utils.smart.workflow.model;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import static com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider.ModelNames.GENIOUS;
import com.axonivy.utils.smart.workflow.model.openai.OpenAiModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;

@IvyTest
class TestChatModelFactory {

  @Test
  void resolveByName() {
    assertThat(ChatModelFactory.create(DummyChatModelProvider.NAME))
        .isPresent();
  }

  @Test
  void resolveBase() {
    assertThat(ChatModelFactory.create(OpenAiModelProvider.NAME))
        .isNotEmpty();
  }

  @Test
  void listModels() {
    assertThat(loadDummy().models())
        .contains(GENIOUS);
  }

  @Test
  void chat() {
    ChatModel model = loadDummy().setup(buildStructuredModelOptions());
    assertThat(model.chat("are you smart?"))
        .isEqualTo("Hey I'm Genious. My Smartness is under development.");
  }

  @Test
  void capabilities() {
    var provider = loadDummy();
    ChatModel normal = provider.setup(buildNormalModelOptions());
    assertThat(normal.supportedCapabilities()).isEmpty();

    ChatModel structured = provider.setup(buildStructuredModelOptions());
    assertThat(structured.supportedCapabilities())
        .contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
  }

  @Test
  void listenersAreInvoked() {
    var calls = new ArrayList<String>();
    ChatModelListener listener = new ChatModelListener() {
      @Override
      public void onRequest(ChatModelRequestContext ctx) {
        calls.add("request");
      }
      @Override
      public void onResponse(ChatModelResponseContext ctx) {
        calls.add("response");
      }
    };
    ChatModel model = loadDummy().setup(ModelOptions.builder()
        .modelName(GENIOUS)
        .structuredOutput(false)
        .hasTools(false)
        .listeners(List.of(listener))
        .build());
    model.chat(ChatRequest.builder().messages(UserMessage.from("ping")).build());
    assertThat(calls).containsExactly("request", "response");
  }

  private static ChatModelProvider loadDummy() {
    return ChatModelFactory.create(DummyChatModelProvider.NAME).get();
  }

  private ModelOptions buildNormalModelOptions() {
    return ModelOptions.builder()
        .modelName(GENIOUS)
        .structuredOutput(false)
        .hasTools(false)
        .listeners(List.of())
        .build();
  }

  private ModelOptions buildStructuredModelOptions() {
    return ModelOptions.builder()
        .modelName(GENIOUS)
        .structuredOutput(true)
        .hasTools(false)
        .listeners(List.of())
        .build();
  }
}
