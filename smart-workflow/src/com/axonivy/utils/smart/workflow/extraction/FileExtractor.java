package com.axonivy.utils.smart.workflow.extraction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

public class FileExtractor {

  private static final byte[] PDF_PREFIX = "%PDF-".getBytes(StandardCharsets.US_ASCII);
  private static final String UNSUPPORTED_FILE_TYPE_MSG = "Unsupported file type for '%s'. Consider converting to PDF, PNG or JPEG, or open a support request.";
  private static final String FAILED_TO_READ_FILE_MSG = "Failed to read file '%s'";
  private final ChatModel model;

  public FileExtractor(ChatModel model) {
    this.model = model;
  }

  public String extract(InputStream stream, String fileName) {
    if (stream == null) {
      return "";
    }
    String resolvedFilename = Optional.ofNullable(fileName).orElse("unknown file name");
    try {
      byte[] bytes = stream.readAllBytes();
      Content content = createContent(bytes, resolvedFilename);
      if (content != null) {
        return model.chat(UserMessage.from(content)).aiMessage().text();
      }
      throw new RuntimeException(String.format(UNSUPPORTED_FILE_TYPE_MSG, resolvedFilename));
    } catch (IOException e) {
      throw new RuntimeException(String.format(FAILED_TO_READ_FILE_MSG, resolvedFilename), e);
    }
  }

  private static Content createContent(byte[] bytes, String fileName) {
    String base64 = Base64.getEncoder().encodeToString(bytes);
    String extension = Optional.ofNullable(fileName)
        .map(FilenameUtils::getExtension)
        .map(String::toLowerCase)
        .or(() -> detectExtension(bytes))
        .orElse("");

    return switch (extension) {
      case "png" -> ImageContent.from(base64, "image/png", ImageContent.DetailLevel.HIGH);
      case "jpg", "jpeg" -> ImageContent.from(base64, "image/jpeg", ImageContent.DetailLevel.HIGH);
      case "pdf" -> PdfFileContent.from(base64, "application/pdf");
      default -> null;
    };
  }

  private static Optional<String> detectExtension(byte[] data) {
    try {
      String mime = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(data));
      return switch (mime != null ? mime : "") {
        case "image/png"  -> Optional.of("png");
        case "image/jpeg" -> Optional.of("jpeg");
        default -> resolvePdfExtension(data);
      };
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /**
   * PDF files cannot be reliably detected via URLConnection, so we check the prefix bytes for "%PDF-".
   */
  private static Optional<String> resolvePdfExtension(byte[] data) {
    return data.length >= PDF_PREFIX.length
        && Arrays.equals(data, 0, PDF_PREFIX.length,
          PDF_PREFIX, 0, PDF_PREFIX.length)
        ? Optional.of("pdf") : Optional.empty();
  }
}
