package com.axonivy.utils.smart.workflow.observability;

import java.util.List;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.governance.history.listener.ChatHistoryListener;
import com.axonivy.utils.smart.workflow.observability.customfields.CustomFieldTrackingListener;
import com.axonivy.utils.smart.workflow.observability.openinference.OpenInferenceTracing;

import dev.langchain4j.observability.api.listener.AiServiceListener;

public class AiListeners {

  public static Stream<AiServiceListener<?>> create(ListenerCtxt ctxt) {
    return providers(ctxt).stream()
      .flatMap(provider -> provider.provide().stream());
  }

  private static List<AiListenerProvider> providers(ListenerCtxt ctxt) {
    return List.of(
      new CustomFieldTrackingListener(), 
      new ChatHistoryListener(), 
      new OpenInferenceTracing(ctxt.provider().name(), ctxt.provider().model())
    );
  }
  
  public record ListenerCtxt(AiProvider provider) {}

  public record AiProvider(String name, String model) {}
}
