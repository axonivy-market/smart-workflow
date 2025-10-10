package com.axonivy.utils.smart.workflow.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.connector.OpenAiServiceConnector;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

@IvyTest
public class TestEasyRag {

  /**
   * https://docs.langchain4j.dev/tutorials/rag/
   */
  @Test
  void easy() {
    List<Document> documents = FileSystemDocumentLoader.loadDocuments("/home/rew/Documents/Blog");
    InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
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

}
