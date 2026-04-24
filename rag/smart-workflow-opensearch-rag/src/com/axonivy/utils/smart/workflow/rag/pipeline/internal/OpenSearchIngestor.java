package com.axonivy.utils.smart.workflow.rag.pipeline.internal;

import com.axonivy.utils.smart.workflow.rag.pipeline.RagConnector;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagIngestor;

public class OpenSearchIngestor implements RagIngestor {

  private final RagConnector connector;

  public OpenSearchIngestor() {
    this(new OpenSearchConnector());
  }

  public OpenSearchIngestor(RagConnector connector) {
    this.connector = connector;
  }

  @Override
  public RagConnector connector() {
    return connector;
  }
}
