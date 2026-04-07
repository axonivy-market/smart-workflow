package com.axonivy.utils.smart.workflow.rag.pipeline.internal;

public interface OpenSearchConf {
  public String PREFIX = "AI.RAG.OpenSearch.";
  public String URL = PREFIX + "Url";
  public String API_KEY = PREFIX + "ApiKey";
  public String USER_NAME = PREFIX + "UserName";
  public String PASSWORD = PREFIX + "Password";
  public String DEFAULT_COLLECTION = PREFIX + "DefaultCollection";
  public String FALLBACK_COLLECTION = "default-axon-ivy-vector-store";
}
