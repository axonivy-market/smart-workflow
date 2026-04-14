package com.axonivy.utils.smart.workflow.rag.pipeline;

import java.util.List;

import com.axonivy.utils.smart.workflow.model.EmbeddingModelFactory;
import com.axonivy.utils.smart.workflow.rag.RagConf;
import com.axonivy.utils.smart.workflow.rag.document.processor.RagDocumentSplitter;
import com.axonivy.utils.smart.workflow.rag.entity.RagResult;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

public interface RagIngestor {

  String MSG_NO_CONTENT = "No content could be loaded from the provided sources.";
  String MSG_INDEXED_FORMAT = "Indexed %d segments.";

  RagConnector connector();

  default RagResult ingest(String collection, List<String> sources) {
    return ingest(collection, sources,
        IvyVar.integer(RagConf.CHUNK_SIZE, RagConf.FALLBACK_CHUNK_SIZE),
        IvyVar.integer(RagConf.CHUNK_OVERLAP, RagConf.FALLBACK_CHUNK_OVERLAP));
  }

  default RagResult ingest(String collection, List<String> sources, int chunkSize, int chunkOverlap) {
    try {
      List<TextSegment> segments = new RagDocumentSplitter(chunkSize, chunkOverlap).split(sources);
      if (segments.isEmpty()) {
        return new RagResult(MSG_NO_CONTENT);
      }
      List<Embedding> embeddings = EmbeddingModelFactory.createFromIvyVars().embedAll(segments).content();
      RagVectorStore store = connector().connect(collection);
      store.addAll(embeddings, segments);
      RagResult result = new RagResult();
      result.setAnswer(String.format(MSG_INDEXED_FORMAT, segments.size()));
      return result;
    } catch (RuntimeException ex) {
      Ivy.log().error("Ingest failed", ex);
      return new RagResult(ex.getMessage());
    }
  }
}
