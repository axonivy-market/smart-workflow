package com.axonivy.utils.smart.workflow.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;
import com.axonivy.utils.smart.workflow.utils.IvyUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.ChatModel;

public class ChatModelFactory {

  private static final String FALLBACK_PROVIDER = "OpenAI";

  public interface AiConf {
    String DEFAULT_PROVIDER = "AI.DefaultProvider";
  }

  public static ChatModel createModel(ModelOptions modelOptions, String providerName) {
    String resolvedProviderName = StringUtils.defaultIfBlank(providerName,
        StringUtils.defaultIfBlank(Ivy.var().get(AiConf.DEFAULT_PROVIDER), FALLBACK_PROVIDER));

    var provider = ChatModelFactory.create(resolvedProviderName)
        .orElseThrow(() -> new IllegalArgumentException("Unknown model provider " + resolvedProviderName));
    return provider.setup(modelOptions);
  }

  public static Optional<ChatModelProvider> create(String provider) {
    return providers().stream() // TODO: stick to naming in dev.langchain4j.model.ModelProvider ?
        .filter(impl -> Objects.equals(impl.name(), provider))
        .findFirst();
  }

  public static Set<ChatModelProvider> providers() {
    var project = IvyUtils.getSmartWorlflowPmv().project(); // TODO: caching?
    return new SpiLoader(project).load(ChatModelProvider.class);
  }
}
