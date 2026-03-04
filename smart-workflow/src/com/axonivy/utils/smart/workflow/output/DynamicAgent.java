package com.axonivy.utils.smart.workflow.output;

import dev.langchain4j.data.message.UserMessage;

public interface DynamicAgent<T> {
  T chat(UserMessage query);
}
