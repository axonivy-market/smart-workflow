package com.axonivy.utils.smart.workflow.rag;

public interface RagConf {

  String SOURCE_KEY = "source";

  String PREFIX = "AI.RAG.";
  String CHUNK_SIZE = PREFIX + "ChunkSize";
  String CHUNK_OVERLAP = PREFIX + "ChunkOverlap";
  String MAX_RESULTS = PREFIX + "MaxResults";
  String MIN_SCORE = PREFIX + "MinScore";
  String EMBEDDING_PROVIDER = PREFIX + "EmbeddingModel.Provider";
  String EMBEDDING_MODEL_NAME = PREFIX + "EmbeddingModel.Name";
  String EMBEDDING_API_KEY = PREFIX + "EmbeddingModel.ApiKey";

  int FALLBACK_CHUNK_SIZE = 300;
  int FALLBACK_CHUNK_OVERLAP = 20;
  int FALLBACK_MAX_RESULTS = 5;
  double FALLBACK_MIN_SCORE = 0.6;

}
