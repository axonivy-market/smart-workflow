package com.axonivy.utils.smart.workflow.output;

public interface DynamicAgent<T> {
  T chat(String query);
}
