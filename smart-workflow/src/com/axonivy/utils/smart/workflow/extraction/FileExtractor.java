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

  // Signature bytes of PDF file: "%PDF-"
  private static final byte[] PDF_PREFIX = "%PDF-".getBytes(StandardCharsets.US_ASCII);

  private final ChatModel model;

  public FileExtractor(ChatModel model) {
    this.model = model;
  }

  /**
   * Extracts content from a stream.
   * When {@code fileName} is blank, the format is detected via magic bytes (PDF, PNG, JPEG).
   * Otherwise the file extension is used.
   */
  public String extract(InputStream stream, String fileName) {
    if (stream == null) {
      return "";
    }
    try {
      byte[] bytes = stream.readAllBytes();
      Content content = createContent(bytes, fileName);
      if (content != null) {
        return model.chat(UserMessage.from(content)).aiMessage().text();
      }
      return "";
    } catch (IOException e) {
      throw new RuntimeException("Failed to read: " + e.getMessage(), e);
    }
  }

  private static Content createContent(byte[] bytes, String fileName) {
    String base64 = Base64.getEncoder().encodeToString(bytes);
    String extension = Optional.ofNullable(fileName)
        .map(FilenameUtils::getExtension)
        .map(String::toLowerCase)
        .orElseGet(() -> detectExtension(bytes));

    return switch (extension) {
      case "png" -> ImageContent.from(base64, "image/png", ImageContent.DetailLevel.HIGH);
      case "jpg", "jpeg" -> ImageContent.from(base64, "image/jpeg", ImageContent.DetailLevel.HIGH);
      case "pdf" -> PdfFileContent.from(base64, "application/pdf");
      default -> null;
    };
  }

  /**
   * Detects file type based on magic bytes. Currently supports PDF, PNG, JPEG.
   */
  private static String detectExtension(byte[] data) {
    try {
      String mime = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(data));
      return switch (mime != null ? mime : "") {
        case "image/png"  -> "png";
        case "image/jpeg" -> "jpeg";
        default -> resolvePdfExtension(data);
      };
    } catch (IOException e) {
      return "";
    }
  }

  /**
   * PDF files cannot be reliably detected via URLConnection, so we check the prefix bytes for "%PDF-".
   */
  private static String resolvePdfExtension (byte[] data) {
    return data.length >= PDF_PREFIX.length
        && Arrays.equals(data, 0, PDF_PREFIX.length,
          PDF_PREFIX, 0, PDF_PREFIX.length) 
          ? "pdf" : "";
  }
}
