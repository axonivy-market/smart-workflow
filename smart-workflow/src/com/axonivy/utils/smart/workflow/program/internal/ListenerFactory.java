package com.axonivy.utils.smart.workflow.program.internal;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.observability.openinference.OpenInferenceTracing;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.listener.ChatModelListener;

class ListenerFactory {

  public static List<ChatModelListener> createListeners(String provider) {
    List<ChatModelListener> listeners = new ArrayList<>();
    if (eval(OpenInferenceTracing.Var.ENABLED)) {
      listeners.add(new OpenInferenceTracing(provider));
    }
    return listeners;
  }

  private static boolean eval(String name) {
    return "true".equals(Ivy.var().get(name));
  }

}
