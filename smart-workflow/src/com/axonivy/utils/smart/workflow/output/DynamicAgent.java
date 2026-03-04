package com.axonivy.utils.smart.workflow.output;

import java.util.List;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.UserMessage;

public interface DynamicAgent<T> {
  T chat(@UserMessage List<Content> query);
}
