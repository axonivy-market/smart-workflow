package com.axonivy.utils.smart.workflow.model.dummy;

import java.util.List;
import java.util.Set;

import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;

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

  }

}
