package com.axonivy.utils.smart.workflow.rag.pipeline;

import com.axonivy.utils.smart.workflow.model.EmbeddingModelFactory;

import dev.langchain4j.model.embedding.EmbeddingModel;

public interface RagConnector {

  boolean indexExists(String collection);

  RagVectorStore vectorStore(String collection);

  default EmbeddingModel embeddingModel() {
    return EmbeddingModelFactory.createFromIvyVars();
  }
}
