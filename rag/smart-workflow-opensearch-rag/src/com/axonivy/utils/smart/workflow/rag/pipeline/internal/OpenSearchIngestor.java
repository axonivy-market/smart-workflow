package com.axonivy.utils.smart.workflow.rag.pipeline.internal;

import java.util.List;

import com.axonivy.utils.smart.workflow.model.EmbeddingModelFactory;
import com.axonivy.utils.smart.workflow.rag.RagConf;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagConnector;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagIngestor;
import com.axonivy.utils.smart.workflow.rag.entity.RagResult;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class OpenSearchIngestor implements RagIngestor {

  private static final String ERR_INGEST_FAILED = "Ingest failed";

  private final RagConnector connector;

  public OpenSearchIngestor() {
    this(new OpenSearchConnector());
  }

  public OpenSearchIngestor(RagConnector connector) {
    this.connector = connector;
  }

  @Override
  public RagResult ingest(String collection, List<String> sources) {
    try {
      int chunkSize = IvyVar.integer(RagConf.CHUNK_SIZE, RagConf.FALLBACK_CHUNK_SIZE);
      int chunkOverlap = IvyVar.integer(RagConf.CHUNK_OVERLAP, RagConf.FALLBACK_CHUNK_OVERLAP);
      EmbeddingModel embeddingModel = EmbeddingModelFactory.createFromIvyVars();
      return performIngest(connector, collection, sources, chunkSize, chunkOverlap, embeddingModel);
    } catch (Exception ex) {
      Ivy.log().error(ERR_INGEST_FAILED, ex);
      return new RagResult(ex.getMessage());
    }
  }

}
