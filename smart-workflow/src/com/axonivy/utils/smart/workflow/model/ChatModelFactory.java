package com.axonivy.utils.smart.workflow.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.internal.SpiLoader;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.ProcessModelVersionRelation;
import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.ChatModel;

public class ChatModelFactory {

  private static final String FALLBACK_PROVIDER = "OpenAI";
  private static final String SMART_WORKFLOW_PROJECT = "smart-workflow";

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
    var project = myPmv().project(); // TODO: caching?
    return new SpiLoader(project).load(ChatModelProvider.class);
  }

  private static IProcessModelVersion myPmv() {
    Predicate<IProcessModelVersion> smartWorkflow = pmv -> SMART_WORKFLOW_PROJECT.equals(pmv.getName());
    var current = IProcessModelVersion.current();
    if (smartWorkflow.test(current)) {
      return current;
    }
    return current.getAllRelatedProcessModelVersions(ProcessModelVersionRelation.REQUIRED)
        .stream().filter(smartWorkflow)
        .findAny().orElseThrow();
  }

}
