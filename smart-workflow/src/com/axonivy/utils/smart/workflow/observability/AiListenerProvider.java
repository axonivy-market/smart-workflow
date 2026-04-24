package com.axonivy.utils.smart.workflow.observability;

import java.util.List;

import dev.langchain4j.observability.api.listener.AiServiceListener;

public interface AiListenerProvider {
  /**
   * @return listeners to track AI service execution.
   */
  List<AiServiceListener<?>> provide(); 
}
