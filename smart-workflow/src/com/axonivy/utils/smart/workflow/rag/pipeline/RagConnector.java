package com.axonivy.utils.smart.workflow.rag.pipeline;

import com.axonivy.utils.smart.workflow.rag.entity.RagResult;

import org.apache.commons.lang3.StringUtils;

public interface RagConnector {
  boolean indexExists(String collection);
  Connection connect(String collection);

  default RagVectorStore connectStore(String collection) {
    Connection connection = connect(collection);
    if (connection.hasError()) {
      throw new IllegalStateException(connection.error());
    }
    return connection.store();
  }

  record Connection(RagVectorStore store, String error) {
    public boolean hasError() {
      return StringUtils.isNotBlank(error);
    }
  }
}
