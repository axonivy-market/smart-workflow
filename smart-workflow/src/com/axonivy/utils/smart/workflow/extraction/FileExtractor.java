package com.axonivy.utils.smart.workflow.extraction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

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

  private static final Map<String, String> MIME_TYPES = Map.of(
      "png", "image/png",
      "jpg", "image/jpeg",
      "jpeg", "image/jpeg",
      "pdf", "application/pdf"
  );

  private static final String UNSUPPORTED_FILE_TYPE_MSG = "Unsupported file type: %s. Supported types: %s.";
  private static final String FAILED_TO_READ_FILE_MSG = "Failed to read file: %s";

  private final ChatModel model;

  public FileExtractor(ChatModel model) {
    this.model = model;
  }

  public String extract(File file) {
    if (file == null) {
      return "";
    }

    UserMessage userMessage = UserMessage.from(createFileContent(file));
    return model.chat(SYSTEM_PROMPT, userMessage).aiMessage().text();
  }

  private Content createFileContent(File file) {
    String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
    return switch (extension) {
      case "png", "jpg", "jpeg"
        -> ImageContent.from(encodeFileToBase64(file), MIME_TYPES.get(extension), ImageContent.DetailLevel.HIGH);
      case "pdf"
        -> PdfFileContent.from(encodeFileToBase64(file), MIME_TYPES.get(extension));
      default
        -> throw new IllegalArgumentException(String.format(UNSUPPORTED_FILE_TYPE_MSG, file.getName(), MIME_TYPES.keySet()));
    };
  }

  private String encodeFileToBase64(File file) {
    try {
      byte[] fileBytes = Files.readAllBytes(file.toPath());
      return Base64.getEncoder().encodeToString(fileBytes);
    } catch (IOException e) {
      throw new RuntimeException(String.format(FAILED_TO_READ_FILE_MSG, file.getName()), e);
    }
  }
}
