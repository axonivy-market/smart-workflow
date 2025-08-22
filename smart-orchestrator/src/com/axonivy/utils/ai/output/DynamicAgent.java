package com.axonivy.utils.ai.output;

public interface DynamicAgent<T> {
  T chat(String query);
}
