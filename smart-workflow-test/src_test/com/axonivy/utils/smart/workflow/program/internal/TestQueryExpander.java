package com.axonivy.utils.smart.workflow.program.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider.ModelNames;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.ChatModel;

@IvyTest
class TestQueryExpander {

  private static final String EXTRACTED = "Extracted content";
  private static final byte[] DUMMY_CONTENT = new byte[]{0x00};

  @TempDir
  File tempDir;

  private ChatModel model;

  @BeforeEach
  void setup() {
    DummyChatModelProvider.defineChatText(request -> EXTRACTED);
    model = new DummyChatModelProvider().setup(new ModelOptions(ModelNames.GENIOUS, false));
  }

  @Test
  void javaFileIsResolved() throws IOException {
    assertFileTypeResolved(createTempFile("invoice.pdf"));
  }

  @Test
  void nioPathIsResolved() throws IOException {
    assertFileTypeResolved(createTempFile("invoice.pdf").toPath());
  }

  @Test
  void ivyFileIsResolved() throws IOException {
    var ivyFile = new ch.ivyteam.ivy.scripting.objects.File("test/invoice.pdf");
    var javaFile = ivyFile.getJavaFile();
    Files.createDirectories(javaFile.toPath().getParent());
    Files.write(javaFile.toPath(), DUMMY_CONTENT);
    try {
      assertFileTypeResolved(ivyFile);
    } finally {
      Files.deleteIfExists(javaFile.toPath());
    }
  }

  @Test
  void binaryIsResolved() {
    var pdfBytes = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);
    var binary = new ch.ivyteam.ivy.scripting.objects.Binary(pdfBytes);
    assertFileTypeResolved(binary);
  }

  @Test
  void inputStreamIsResolved() {
    InputStream stream = new ByteArrayInputStream("%PDF-1.4".getBytes(StandardCharsets.US_ASCII));
    assertFileTypeResolved(stream);
  }

  @Test
  void nonFileExpressionIsExpandedAsString() {
    String result = QueryExpander.expandFileExpressions(
        "Hello <%=in.name%>",
        model,
        expr -> Optional.of("John"),
        cmsExpr -> null);

    assertThat(result).isEqualTo("Hello John");
  }

  @Test
  void templateWithNoExpressionsIsReturnedAsIs() {
    String result = QueryExpander.expandFileExpressions(
        "Plain text",
        model,
        expr -> Optional.empty(),
        cmsExpr -> null);

    assertThat(result).isEqualTo("Plain text");
  }

  @Test
  void emptyResolverResultLeavesExpressionInPlace() {
    String result = QueryExpander.expandFileExpressions(
        "Value: <%=in.missing%>",
        model,
        expr -> Optional.empty(),
        cmsExpr -> null);

    assertThat(result).isEqualTo("Value: <%=in.missing%>");
  }

  @Test
  void cmsTextIsReturnedAsString() {
    String result = QueryExpander.expandFileExpressions(
        "Prompt: <%=ivy.cms.co(\"/Texts/SystemPrompt\")%>",
        model,
        expr -> { throw new AssertionError("resolver must not be called for CMS expressions"); },
        cmsExpr -> "You are Smart Workflow.");
    assertThat(result).isEqualTo("Prompt: You are Smart Workflow.");
  }

  @Test
  void cmsSingleQuoteIsSupported() {
    String result = QueryExpander.expandFileExpressions(
        "<%=ivy.cms.co('/Texts/Note')%>",
        model,
        expr -> { throw new AssertionError("resolver must not be called for CMS expressions"); },
        cmsExpr -> "Note text");
    assertThat(result).isEqualTo("Note text");
  }

  @Test
  void cmsBinaryIsExtractedViaFileExtractor() {
    byte[] pdfBytes = "%PDF-1.4 binary".getBytes(StandardCharsets.US_ASCII);
    String result = QueryExpander.expandFileExpressions(
        "Invoice: <%=ivy.cms.co(\"/Docs/Invoice\")%>",
        model,
        expr -> { throw new AssertionError("resolver must not be called for CMS expressions"); },
        cmsExpr -> new ByteArrayInputStream(pdfBytes));
    assertThat(result).isEqualTo("Invoice: " + EXTRACTED);
  }

  @Test
  void cmsPathNotFoundReturnsEmptyString() {
    String result = QueryExpander.expandFileExpressions(
        "Value: <%=ivy.cms.co(\"/Missing/Path\")%>",
        model,
        expr -> Optional.empty(),
        cmsExpr -> null);
    assertThat(result).isEqualTo("Value: ");
  }

  private void assertFileTypeResolved(Object fileArg) {
    String result = QueryExpander.expandFileExpressions(
        "Summary: <%=in.file%>",
        model,
        expr -> Optional.of(fileArg),
        cmsExpr -> null);
    assertThat(result).isEqualTo("Summary: " + EXTRACTED);
  }

  private File createTempFile(String name) throws IOException {
    File file = new File(tempDir, name);
    Files.write(file.toPath(), DUMMY_CONTENT);
    return file;
  }
}
