package com.axonivy.utils.smart.workflow.extraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.apache.commons.io.FilenameUtils;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

public class FileExtractor {

  private final ChatModel model;

  public FileExtractor(ChatModel model) {
    this.model = model;
  }

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
    String extension = FilenameUtils.getExtension(fileName).toLowerCase();
    String base64 = Base64.getEncoder().encodeToString(bytes);
    return switch (extension) {
      case "png" -> ImageContent.from(base64, "image/png", ImageContent.DetailLevel.HIGH);
      case "jpg", "jpeg" -> ImageContent.from(base64, "image/jpeg", ImageContent.DetailLevel.HIGH);
      case "pdf" -> PdfFileContent.from(base64, "application/pdf");
      default -> null;
    };
  }
}
