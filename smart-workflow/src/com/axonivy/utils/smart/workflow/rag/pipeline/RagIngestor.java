package com.axonivy.utils.smart.workflow.rag.pipeline;

import java.util.List;

import com.axonivy.utils.smart.workflow.rag.document.processor.RagDocumentSplitter;
import com.axonivy.utils.smart.workflow.rag.entity.RagResult;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

public interface RagIngestor {

  RagResult ingest(String collection, List<String> sources);

  default RagResult performIngest(RagConnector connector, String collection, List<String> sources, int chunkSize, int chunkOverlap, EmbeddingModel embeddingModel) {
    List<TextSegment> segments = new RagDocumentSplitter(chunkSize, chunkOverlap).split(sources);
    if (segments.isEmpty()) {
      return new RagResult("No content could be loaded from the provided sources.");
    }
    List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
    RagVectorStore store = connector.connect(collection);
    store.addAll(embeddings, segments);
    RagResult result = new RagResult();
    result.setAnswer("Indexed " + segments.size() + " segments.");
    return result;
  }

}
