package com.axonivy.utils.smart.workflow.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.core.internal.resources.MarkerTypeDefinitionCache;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.loader.github.GitHubDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.parser.markdown.MarkdownDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

@IvyTest
public class TestEasyRag {

  /**
   * https://docs.langchain4j.dev/tutorials/rag/
   */
  @Test
  void easy() {
    List<Document> documents = FileSystemDocumentLoader.loadDocuments("/home/rew/Documents/Blog", new MarkdownDocumentParser());
    EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    EmbeddingStoreIngestor.ingest(documents, embeddingStore);

    ChatModel chatModel = OpenAiServiceConnector.buildOpenAiModel()
        .build();

    var memory = MessageWindowChatMemory.withMaxMessages(10);
    Assistant assistant = AiServices.builder(Assistant.class)
        .chatModel(chatModel)
        .chatMemory(memory)
        .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
        .build();

    var response = assistant.chat("In which area did I work recently?");
    System.out.println(response);

    System.out.println(memory.messages());
    assertThat(memory.messages()).hasSize(2);
    assertThat(memory.messages().get(0).toString())
        .as("injects RAG matches into the User Prompt")
        .contains("Monday");

    var pattern = assistant.chat("Do you find a pattern in my Monday work?");
    assertThat(pattern).isNotNull();
  }

  interface Assistant {
    String chat(String userMessage);
  }

  @Test
  void markdown() {
    List<Document> documents = FileSystemDocumentLoader.loadDocuments("/home/rew/Documents/Blog",
        new MarkdownDocumentParser());
    var first = documents.get(0);
    System.out.println("meta; " + first.metadata().toMap());
  }

  @Test
  void github() {
    var ghLoader = new GitHubDocumentLoader(Ivy.var().get("AI.RAG.Github.Token"), "axonivy-market");
    var ghDocs = ghLoader.loadDocuments("axonivy-market", "smart-workflow", "master", new ApacheTikaDocumentParser());
  }

}
