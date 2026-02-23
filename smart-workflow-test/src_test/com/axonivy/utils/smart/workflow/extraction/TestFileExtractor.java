package com.axonivy.utils.smart.workflow.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
  private static final byte[] DUMMY_CONTENT = new byte[]{0x00};

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
  void unsupportedFileTypeReturnsEmpty() throws IOException {
    var file = createTempFile("test.txt", DUMMY_CONTENT);
    InputStream stream = Files.newInputStream(file.toPath());
    assertThat(new FileExtractor(model).extract(stream, file.getName())).isEmpty();
  }

  private File createTempFile(String name, byte[] content) throws IOException {
    File file = new File(tempDir, name);
    Files.write(file.toPath(), content);
    return file;
  }
}
