package com.axonivy.utils.smart.workflow.extraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.apache.commons.io.FilenameUtils;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

public class FileExtractor {

  private static final SystemMessage SYSTEM_PROMPT = SystemMessage.from("""
      Please analyze the attached file and extract relevant information.
      If the file is a PDF, extract the text content from all pages.
      The format should be in Markdown, and if the file contains tabular data, please represent it in a markdown table for better readability.
  """);

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
        return model.chat(SYSTEM_PROMPT, UserMessage.from(content)).aiMessage().text();
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
