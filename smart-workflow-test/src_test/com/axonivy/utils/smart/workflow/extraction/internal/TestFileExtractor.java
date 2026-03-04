package com.axonivy.utils.smart.workflow.extraction.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider.ModelNames;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.ChatModel;

@IvyTest
class TestFileExtractor {

  private static final String EXTRACTED_TEXT = "Extracted file content";
  private static final byte[] DUMMY_CONTENT = new byte[]{0x01, 0x02, 0x03, 0x04};
  private static final byte[] PDF_PREFIX = "%PDF-".getBytes(StandardCharsets.US_ASCII); // PDF signature bytes
  private static final byte[] PNG_PREFIX = HexFormat.of().parseHex("89504E470D0A1A0A"); // PNG signature bytes
  private static final byte[] JPEG_PREFIX = HexFormat.of().parseHex("FFD8FFE0"); // JPEG signature bytes, JPEG has several valid signatures, this is one of the most common ones

  private ChatModel model;

  @TempDir
  File tempDir;

  @BeforeEach
  void setup() {
    DummyChatModelProvider.defineChatText(request -> EXTRACTED_TEXT);
    model = new DummyChatModelProvider().setup(new ModelOptions(ModelNames.GENIOUS, false));
  }

  @Test
  void extractSupportedFileTypes() throws IOException {
    var extractor = new FileExtractor(model);
    for (String extension : new String[]{"png", "jpg", "jpeg", "pdf"}) {
      var file = createTempFile("test." + extension, DUMMY_CONTENT);
      InputStream stream = Files.newInputStream(file.toPath());
      assertThat(extractor.extract(stream, file.getName())).isEqualTo(EXTRACTED_TEXT);
    }
  }

  @Test
  void nullStreamReturnsEmpty() {
    assertThat(new FileExtractor(model).extract(null, "test.png")).isEmpty();
  }

  @Test
  void unsupportedFileTypeThrowsException() throws IOException {
    var file = createTempFile("test.txt", DUMMY_CONTENT);
    InputStream stream = Files.newInputStream(file.toPath());
    assertThatThrownBy(() -> new FileExtractor(model).extract(stream, file.getName()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("test.txt");
  }

  @Test
  void detectsBinaryFiles() {
    assertThat(new FileExtractor(model).extract(new ByteArrayInputStream(PDF_PREFIX), null)).isEqualTo(EXTRACTED_TEXT);
    assertThat(new FileExtractor(model).extract(new ByteArrayInputStream(PNG_PREFIX), null)).isEqualTo(EXTRACTED_TEXT);
    assertThat(new FileExtractor(model).extract(new ByteArrayInputStream(JPEG_PREFIX), null)).isEqualTo(EXTRACTED_TEXT);
    assertThatThrownBy(() -> new FileExtractor(model).extract(new ByteArrayInputStream(DUMMY_CONTENT), null))
        .isInstanceOf(RuntimeException.class);
  }

  private File createTempFile(String name, byte[] content) throws IOException {
    File file = new File(tempDir, name);
    Files.write(file.toPath(), content);
    return file;
  }
}
