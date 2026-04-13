package com.axonivy.utils.smart.workflow.rag.pipeline;

public interface RagConnector {

  boolean indexExists(String collection);

  RagVectorStore connect(String collection);
}
