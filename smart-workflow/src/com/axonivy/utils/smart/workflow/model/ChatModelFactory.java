package com.axonivy.utils.smart.workflow.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.internal.SpiLoader;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.ProcessModelVersionRelation;
import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.ChatModel;

public class ChatModelFactory {

  public interface AiConf {
    String DEFAULT_PROVIDER = "AI.DefaultProvider";
  }

  public static ChatModel createModel(ModelOptions modelOptions) {
    String vendor = Optional.ofNullable(Ivy.var().get(AiConf.DEFAULT_PROVIDER))
        .filter(Predicate.not(String::isEmpty))
        .orElse("OpenAI");
    var provider = ChatModelFactory.create(vendor)
        .orElseThrow(() -> new IllegalArgumentException("Unknown model provider " + vendor));
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
    Predicate<IProcessModelVersion> smartWorkflow = pmv -> "smart-workflow".equals(pmv.getName());
    var current = IProcessModelVersion.current();
    if (smartWorkflow.test(current)) {
      return current;
    }
    return current.getAllRelatedProcessModelVersions(ProcessModelVersionRelation.REQUIRED)
        .stream().filter(smartWorkflow)
        .findAny().orElseThrow();
  }

}
