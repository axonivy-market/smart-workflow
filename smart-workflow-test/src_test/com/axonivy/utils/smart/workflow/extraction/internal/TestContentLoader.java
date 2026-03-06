package com.axonivy.utils.smart.workflow.extraction.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestContentLoader {

  private static final byte[] DUMMY_CONTENT = new byte[]{0x01, 0x02, 0x03, 0x04};
  private static final byte[] PDF_PREFIX = "%PDF-".getBytes(StandardCharsets.US_ASCII); // PDF signature bytes
  private static final byte[] PNG_PREFIX = HexFormat.of().parseHex("89504E470D0A1A0A"); // PNG signature bytes
  private static final byte[] JPEG_PREFIX = HexFormat.of().parseHex("FFD8FFE0"); // JPEG signature bytes

  @TempDir
  File tempDir;

  @Test
  void extractSupportedFileTypes() throws IOException {
    for (String extension : new String[]{"png", "jpg", "jpeg", "pdf"}) {
      var file = createTempFile("test." + extension, DUMMY_CONTENT);
      InputStream stream = Files.newInputStream(file.toPath());
      assertThat(ContentLoader.fromStream(stream, file.getName())).isPresent();
    }
  }

  @Test
  void nullStreamReturnsEmpty() {
    assertThat(ContentLoader.fromStream(null, "test.png")).isEmpty();
  }

  @Test
  void unsupportedFileTypeReturnsEmpty() throws IOException {
    var file = createTempFile("test.txt", DUMMY_CONTENT);
    InputStream stream = Files.newInputStream(file.toPath());
    assertThat(ContentLoader.fromStream(stream, file.getName())).isEmpty();
  }

  @Test
  void detectsBinaryFiles() {
    assertThat(ContentLoader.fromStream(new ByteArrayInputStream(PDF_PREFIX), null)).isPresent();
    assertThat(ContentLoader.fromStream(new ByteArrayInputStream(PNG_PREFIX), null)).isPresent();
    assertThat(ContentLoader.fromStream(new ByteArrayInputStream(JPEG_PREFIX), null)).isPresent();
    assertThat(ContentLoader.fromStream(new ByteArrayInputStream(DUMMY_CONTENT), null)).isEmpty();
  }

  private File createTempFile(String name, byte[] content) throws IOException {
    File file = new File(tempDir, name);
    Files.write(file.toPath(), content);
    return file;
  }
}
