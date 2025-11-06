package com.axonivy.utils.smart.workflow.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.model.openai.OpenAiModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.ChatModel;

public class ChatModelFactory {

  public static ChatModel createModel(ModelOptions modelOptions) {
    String vendor = Optional.ofNullable(Ivy.var().get("AI.defaultProvider"))
        .filter(Predicate.not(String::isEmpty))
        .orElse(OpenAiModelProvider.NAME);
    var provider = ChatModelFactory.create(vendor)
        .orElseThrow(() -> new IllegalArgumentException("Unknown model provider " + vendor));
    return provider.setup(modelOptions);
  }

  public static Optional<ChatModelProvider> create(String provider) {
    return providers() // TODO: stick to naming in dev.langchain4j.model.ModelProvider ?
        .filter(impl -> Objects.equals(impl.name(), provider))
        .findFirst();
  }

  public static Stream<ChatModelProvider> providers() {
    return Stream.of(new OpenAiModelProvider()); // TODO: load dynamic!
  }

}
