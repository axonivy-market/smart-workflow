package com.axonivy.utils.smart.workflow.rag;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.rag.TestEasyRag.Assistant;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.markdown.MarkdownDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;

/**
 * https://docs.langchain4j.dev/integrations/embedding-stores/opensearch/
 */
@IvyTest
class TestOpenSearchIngest {

  @Test
  void ingestMarkdownToOpenSearch() {
    // 1. Load markdown files from a directory
    List<Document> documents = FileSystemDocumentLoader.loadDocuments("/home/rew/Documents/Blog",
        new MarkdownDocumentParser());
    enrichDateMeta(documents);

    // 2. (Optional) Split documents into smaller chunks if needed
    var splitter = DocumentSplitters.recursive(2000, 300);

    // 3. Create embedding model (replace with your actual embedding model)
    var store = composeStore();

    // 4. ingest
    var result = EmbeddingStoreIngestor.builder()
        .embeddingStore(store)
        .documentSplitter(splitter)
        .build()
        .ingest(documents);
    System.out.println("ingested: " + result.tokenUsage());
  }

  private void enrichDateMeta(List<Document> documents) {
    documents.forEach(doc -> {
      var meta = doc.metadata();
      var file = meta.getString("file_name");
      var base = StringUtils.substringBeforeLast(file, ".");
      meta.put("date", "2026-"+base);
    });
  }

  @Test
  void query() {
    var store = composeStore();

    ChatModel chatModel = OpenAiServiceConnector
        .buildOpenAiModel()
        .build();

    var memory = MessageWindowChatMemory.withMaxMessages(10);
    var retriever = EmbeddingStoreContentRetriever.builder()
      .filter(new IsEqualTo("date", "2026-w01")) // nada
      .embeddingStore(store)
      .build();
    Assistant assistant = AiServices.builder(Assistant.class)
        .chatModel(chatModel)
        .chatMemory(memory)
        .contentRetriever(retriever)
        .build();

    var response = assistant.chat("In which area did I work in week 1?");
    System.out.println(response);
  }

  private OpenSearchEmbeddingStore composeStore() {
    return OpenSearchEmbeddingStore.builder()
        .serverUrl("http://localhost:9200")
        .indexName("my-blog26")
        .userName("admin")
        .password("Str0ng!Passw0rd2026")
        .build();
  }

}