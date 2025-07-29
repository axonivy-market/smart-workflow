package com.axonivy.utils.ai.tools;

import java.util.List;
import java.util.stream.Stream;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.engine.IRequestId;
import ch.ivyteam.ivy.process.extension.impl.AbstractUserProcessExtension;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

public class AgenticProcessCall extends AbstractUserProcessExtension {

  public interface Conf {
    String QUERY = "query";
  }

  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    String query = getConfig().get(Conf.QUERY); // execute scripted?

    UserMessage init = UserMessage.from(query);

    var model = new OpenAiServiceConnector()
        .buildOpenAiModel().build();

    List<ToolSpecification> ivyTools = IvyToolSpecs.find();
    ChatRequest request = ChatRequest.builder()
        .messages(init)
        .toolSpecifications(ivyTools)
        .build();
    ChatResponse response = model.chat(request);
    AiMessage aiMessage = response.aiMessage();

    var results = aiMessage.toolExecutionRequests().stream()
        .map(IvyToolExecutor::execute).toList();

    ChatRequest request2 = ChatRequest.builder()
        .messages(Stream.concat(Stream.of((ChatMessage) init, aiMessage), results.stream()).toList())
        .toolSpecifications(ivyTools)
        .build();
    ChatResponse response2 = model.chat(request2);

    Ivy.log().info(response2);
    return in;
  }

  public static class Editor extends UiEditorExtension {

    @Override
    public void initUiFields(ExtensionUiBuilder ui) {
      ui.label("How can I assist you today?").create();
      ui.scriptField(Conf.QUERY)
          .multiline()
          .requireType(String.class)
          .create();
    }
  }

}
