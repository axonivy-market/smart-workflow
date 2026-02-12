package com.axonivy.utils.smart.workflow.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
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
      assertThat(extractor.extract(file)).isEqualTo(EXTRACTED_TEXT);
    }
  }

  @Test
  void invalidFiles() throws IOException {
    File unsupported = createTempFile("test.txt", DUMMY_CONTENT);
    assertThatThrownBy(() -> new FileExtractor(model).extract(unsupported))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported file type");

    File missing = new File(tempDir, "missing.png");
    assertThatThrownBy(() -> new FileExtractor(model).extract(missing))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to read file");
  }

  private File createTempFile(String name, byte[] content) throws IOException {
    File file = new File(tempDir, name);
    Files.write(file.toPath(), content);
    return file;
  }
}
