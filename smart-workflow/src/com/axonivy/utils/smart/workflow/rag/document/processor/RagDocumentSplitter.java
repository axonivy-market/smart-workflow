package com.axonivy.utils.smart.workflow.rag.document.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.rag.RagConf;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

public class RagDocumentSplitter {

  private final int chunkSize;
  private final int chunkOverlap;

  public RagDocumentSplitter(int chunkSize, int chunkOverlap) {
    this.chunkSize = chunkSize;
    this.chunkOverlap = chunkOverlap;
  }

  public List<TextSegment> split(List<String> sources) {
    List<Document> documents = new ArrayList<>();
    for (String source : sources) {
      if (StringUtils.isBlank(source)) {
        continue;
      }
      documents.add(Document.from(source, Metadata.from(RagConf.SOURCE_KEY, "inline")));
    }
    return DocumentSplitters.recursive(chunkSize, chunkOverlap).splitAll(documents);
  }

}
