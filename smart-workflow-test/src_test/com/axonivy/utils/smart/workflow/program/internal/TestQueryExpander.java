package com.axonivy.utils.smart.workflow.program.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.utils.smart.workflow.extraction.internal.ContentLoader;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

@IvyTest
class TestQueryExpander {

  private static final byte[] DUMMY_CONTENT = new byte[]{0x00};

  @TempDir
  File tempDir;

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
    UserMessage result = QueryExpander.expandFileExpressions(
        "Hello <%=in.name%>",
        _ -> Optional.of("John"),
        _ -> Optional.empty());

    assertThat(textOf(result)).isEqualTo("Hello John");
  }

  @Test
  void templateWithNoExpressionsIsReturnedAsIs() {
    UserMessage result = QueryExpander.expandFileExpressions(
        "Plain text",
        _ -> Optional.empty(),
        _ -> Optional.empty());

    assertThat(textOf(result)).isEqualTo("Plain text");
  }

  @Test
  void emptyResolverResultLeavesExpressionInPlace() {
    UserMessage result = QueryExpander.expandFileExpressions(
        "Value: <%=in.missing%>",
        _ -> Optional.empty(),
        _ -> Optional.empty());

    assertThat(textOf(result)).isEqualTo("Value: <%=in.missing%>");
  }

  @Test
  void cmsTextIsReturnedAsString() {
    UserMessage result = QueryExpander.expandFileExpressions(
        "Prompt: <%=ivy.cms.co(\"/Texts/SystemPrompt\")%>",
        _ -> { throw new AssertionError("resolver must not be called for CMS expressions"); },
        _ -> Optional.of(TextContent.from("You are Smart Workflow.")));
    assertThat(textOf(result)).isEqualTo("Prompt: You are Smart Workflow.");
  }

  @Test
  void cmsSingleQuoteIsSupported() {
    UserMessage result = QueryExpander.expandFileExpressions(
        "<%=ivy.cms.co('/Texts/Note')%>",
        _ -> { throw new AssertionError("resolver must not be called for CMS expressions"); },
        _ -> Optional.of(TextContent.from("Note text")));
    assertThat(textOf(result)).isEqualTo("Note text");
  }

  @Test
  void cmsBinaryIsExtractedToContent() {
    byte[] pdfBytes = "%PDF-1.4 binary".getBytes(StandardCharsets.US_ASCII);
    UserMessage result = QueryExpander.expandFileExpressions(
        "Invoice: <%=ivy.cms.co(\"/Docs/Invoice\")%>",
        _ -> { throw new AssertionError("resolver must not be called for CMS expressions"); },
        _ -> ContentLoader.fromStream(new ByteArrayInputStream(pdfBytes), null));
    assertThat(result.contents()).hasSize(2); // TextContent("Invoice: ") + PdfFileContent
  }

  @Test
  void cmsPathNotFoundReturnsEmptyString() {
    UserMessage result = QueryExpander.expandFileExpressions(
        "Value: <%=ivy.cms.co(\"/Missing/Path\")%>",
        _ -> Optional.empty(),
        _ -> Optional.empty());
    assertThat(textOf(result)).isEqualTo("Value: ");
  }

  private void assertFileTypeResolved(Object fileArg) {
    UserMessage result = QueryExpander.expandFileExpressions(
        "Summary: <%=in.file%>",
        _ -> Optional.of(fileArg),
        _ -> Optional.empty());
    assertThat(result.contents()).hasSize(2); // TextContent("Summary: ") + ImageContent/PdfFileContent
  }

  private static String textOf(UserMessage msg) {
    return msg.contents().stream()
        .filter(TextContent.class::isInstance)
        .map(TextContent.class::cast)
        .map(TextContent::text)
        .collect(Collectors.joining());
  }

  private File createTempFile(String name) throws IOException {
    File file = new File(tempDir, name);
    Files.write(file.toPath(), DUMMY_CONTENT);
    return file;
  }
}
