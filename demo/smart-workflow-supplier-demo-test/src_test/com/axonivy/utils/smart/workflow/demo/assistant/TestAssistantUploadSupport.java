package com.axonivy.utils.smart.workflow.demo.assistant;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestAssistantUploadSupport {

  private TestBean bean;

  @BeforeEach
  void setUp() {
    bean = new TestBean();
  }

  @Test
  void buildCombinedContent_noDocsNoContent_returnsNull() {
    assertThat(bean.buildCombinedContent()).isNull();
  }

  @Test
  void buildCombinedContent_noDocsFallsBackToUploadedContent() {
    bean.uploadedContent = "raw text content";

    assertThat(bean.buildCombinedContent()).isEqualTo("raw text content");
  }

  @Test
  void buildCombinedContent_withTextDocs_combinesWithSeparators() {
    bean.uploadedDocuments.add(doc("doc1.txt", "first"));
    bean.uploadedDocuments.add(doc("doc2.md", "second"));

    String result = bean.buildCombinedContent();

    assertThat(result).contains("--- DOCUMENT: doc1.txt ---", "first");
    assertThat(result).contains("--- DOCUMENT: doc2.md ---", "second");
  }

  @Test
  void buildCombinedContent_mixedDocs_skipsPdf() {
    bean.uploadedDocuments.add(doc("report.pdf", "ignored"));
    bean.uploadedDocuments.add(doc("notes.txt", "text content"));

    String result = bean.buildCombinedContent();

    assertThat(result).contains("notes.txt", "text content");
    assertThat(result).doesNotContain("report.pdf");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void addUploadedDocument_invalidName_setsFeedback(String fileName) {
    bean.addUploadedDocument(fileName, "content".getBytes(StandardCharsets.UTF_8));

    assertThat(bean.parseFeedback).isNotNull();
    assertThat(bean.uploadedDocuments).isEmpty();
  }

  @Test
  void addUploadedDocument_invalidExtension_setsFeedback() {
    bean.addUploadedDocument("report.docx", "content".getBytes(StandardCharsets.UTF_8));

    assertThat(bean.parseFeedback).isNotNull();
    assertThat(bean.uploadedDocuments).isEmpty();
  }

  @Test
  void addUploadedDocument_emptyContent_setsFeedback() {
    bean.addUploadedDocument("info.txt", new byte[0]);

    assertThat(bean.parseFeedback).isNotNull();
    assertThat(bean.uploadedDocuments).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"info.txt", "notes.md", "report.pdf"})
  void addUploadedDocument_validExtension_addsDocumentAndSetsState(String fileName) {
    bean.addUploadedDocument(fileName, "content".getBytes(StandardCharsets.UTF_8));

    assertThat(bean.uploadedDocuments).hasSize(1);
    assertThat(bean.awaitingConfirmation).isTrue();
  }

  @Test
  void addUploadedDocument_multipleFiles_accumulates() {
    bean.addUploadedDocument("a.txt", "alpha".getBytes(StandardCharsets.UTF_8));
    bean.addUploadedDocument("b.txt", "beta".getBytes(StandardCharsets.UTF_8));

    assertThat(bean.uploadedDocuments).hasSize(2);
  }

  @Test
  void removeUploadedDocument_lastDoc_resetsState() {
    bean.uploadedDocuments.add(doc("only.txt", "content"));
    bean.awaitingConfirmation = Boolean.TRUE;
    bean.parseFeedback = "1 file(s) ready";
    bean.uploadedFileName = "only.txt";

    bean.removeUploadedDocument("only.txt");

    assertThat(bean.uploadedDocuments).isEmpty();
    assertThat(bean.awaitingConfirmation).isFalse();
    assertThat(bean.parseFeedback).isNull();
    assertThat(bean.uploadedFileName).isNull();
  }

  @Test
  void removeUploadedDocument_oneOfMany_removesOnlyMatching() {
    bean.uploadedDocuments.add(doc("a.txt", "alpha"));
    bean.uploadedDocuments.add(doc("b.txt", "beta"));
    bean.uploadedDocuments.add(doc("c.txt", "gamma"));

    bean.removeUploadedDocument("b.txt");

    assertThat(bean.uploadedDocuments).hasSize(2);
    assertThat(bean.uploadedDocuments).noneMatch(d -> "b.txt".equals(d.getFileName()));
  }

  @Test
  void compileGuidanceContext_noGuidance_returnsEmpty() {
    assertThat(bean.compileGuidanceContext()).isEmpty();
  }

  @Test
  void compileGuidanceContext_withGuidance_formatsLines() {
    bean.guidance.add(guidance("How does it work?", "describe the flow"));
    bean.guidance.add(guidance("What is required?", "list required fields"));

    String result = bean.compileGuidanceContext();

    assertThat(result).startsWith("Question Handling Guidelines:");
    assertThat(result).contains("When the user asks \"How does it work?\": describe the flow");
    assertThat(result).contains("When the user asks \"What is required?\": list required fields");
  }

  @Test
  void resetAssistantState_clearsAllFields() {
    bean.uploadedFileName     = "file.txt";
    bean.uploadedContent      = "some content";
    bean.awaitingConfirmation = Boolean.TRUE;
    bean.parseFeedback        = "some feedback";
    bean.parsedDraft          = "draft value";
    bean.agentUserMessage     = "pending question";
    bean.uploadedDocuments.add(doc("file.txt", "data"));
    bean.agentChatHistory.add(new AssistantChatMessage());

    bean.resetAssistantState();

    assertThat(bean.uploadedFileName).isNull();
    assertThat(bean.uploadedContent).isNull();
    assertThat(bean.awaitingConfirmation).isFalse();
    assertThat(bean.parseFeedback).isNull();
    assertThat(bean.parsedDraft).isNull();
    assertThat(bean.agentUserMessage).isNull();
    assertThat(bean.uploadedDocuments).isEmpty();
    assertThat(bean.agentChatHistory).isEmpty();
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static UploadedDocumentEntry doc(String fileName, String content) {
    return UploadedDocumentEntryFactory.of(fileName, content.getBytes(StandardCharsets.UTF_8));
  }

  private static AgentGuidance guidance(String questionPattern, String instruction) {
    AgentGuidance g = new AgentGuidance();
    g.setQuestionPattern(questionPattern);
    g.setInstruction(instruction);
    return g;
  }

  // ── Test double ──────────────────────────────────────────────────────────

  static class TestBean implements AssistantUploadSupport<String> {

    String uploadedFileName;
    String uploadedContent;
    Boolean awaitingConfirmation = Boolean.FALSE;
    String parseFeedback;
    String parsedDraft;
    String agentUserMessage;
    List<UploadedDocumentEntry> uploadedDocuments = new ArrayList<>();
    List<AgentGuidance> guidance = new ArrayList<>();
    List<AssistantChatMessage> agentChatHistory = new ArrayList<>();

    @Override public String getFormData()                             { return null; }
    @Override public String getParseSubProcessSignature()             { return null; }
    @Override public String getParsedResultKey()                      { return null; }
    @Override public void applyParsedDraft(String draft)              {}
    @Override public void setAssistantParsedDraft(String draft)       { this.parsedDraft = draft; }
    @Override public String getAssistantUploadedContent()             { return uploadedContent; }
    @Override public void setAssistantUploadedFileName(String n)      { this.uploadedFileName = n; }
    @Override public void setAssistantUploadedContent(String c)       { this.uploadedContent = c; }
    @Override public void setAssistantAwaitingConfirmation(Boolean v)  { this.awaitingConfirmation = v; }
    @Override public void setAssistantParseFeedback(String f)         { this.parseFeedback = f; }
    @Override public List<UploadedDocumentEntry> getUploadedDocuments()             { return uploadedDocuments; }
    @Override public void setUploadedDocuments(List<UploadedDocumentEntry> d)       { this.uploadedDocuments = d; }
    @Override public List<AgentGuidance> getAgentGuidance()           { return guidance; }
    @Override public String getAgentUserMessage()                     { return agentUserMessage; }
    @Override public void setAgentUserMessage(String m)               { this.agentUserMessage = m; }
    @Override public List<AssistantChatMessage> getAgentChatHistory()             { return agentChatHistory; }
    @Override public void setAgentChatHistory(List<AssistantChatMessage> h)       { this.agentChatHistory = h; }
  }
}
