package com.axonivy.utils.smart.workflow.rag.pipeline.internal;

import com.axonivy.utils.smart.workflow.rag.RagConf;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagConnector;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagResult;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagRetriever;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class OpenSearchRetriever implements RagRetriever {

  private static final String ERR_SEARCH_FAILED = "OpenSearch RAG search failed";

  private final RagConnector connector;

  public OpenSearchRetriever() {
    this(new OpenSearchConnector());
  }

  public OpenSearchRetriever(RagConnector connector) {
    this.connector = connector;
  }

  @Override
  public RagResult search(String collection, String query, int maxResults, double minScore) {
    try {
      int effectiveMaxResults = maxResults > 0 ? maxResults : IvyVar.integer(RagConf.MAX_RESULTS, RagConf.FALLBACK_MAX_RESULTS);
      double effectiveMinScore = minScore > 0 ? minScore : IvyVar.decimal(RagConf.MIN_SCORE, RagConf.FALLBACK_MIN_SCORE);
      EmbeddingModel embeddingModel = connector.embeddingModel();
      return performSearch(connector, collection, query, effectiveMaxResults, effectiveMinScore, embeddingModel);
    } catch (Exception ex) {
      Ivy.log().error(ERR_SEARCH_FAILED, ex);
      return new RagResult(ex.getMessage());
    }
  }

}
