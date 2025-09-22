package com.axonivy.utils.smart.workflow.demo.shopping.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.primefaces.model.file.UploadedFile;

public final class UploadedFileUtils {

  private static final String MD_EXTENSION = ".md";

  /**
   * Main method to process uploaded file and convert to string Detects file
   * extension and delegates to appropriate handler
   * 
   * @param uploadedFile PrimeFaces UploadedFile object
   * @return String content of the file, or empty string if unsupported type
   */
  public static String processUploadedFile(UploadedFile uploadedFile) {
    if (uploadedFile == null || uploadedFile.getFileName() == null) {
      return "";
    }

    String fileName = uploadedFile.getFileName().toLowerCase();
    String fileExtension = detectFileExtension(fileName);

    try {
      switch (fileExtension) {
      case MD_EXTENSION:
        return handleMdFile(uploadedFile);
      default:
        return "";
      }
    } catch (Exception e) {
      // Log error in production environment
      System.err.println("Error processing uploaded file: " + e.getMessage());
      return "";
    }
  }

  /**
   * Detects file extension from filename
   * 
   * @param fileName name of the file
   * @return file extension including dot (e.g., ".pdf", ".md"), or empty string
   *         if no extension
   */
  public static String detectFileExtension(String fileName) {
    if (fileName == null || fileName.trim().isEmpty()) {
      return "";
    }

    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
      return "";
    }

    return fileName.substring(lastDotIndex).toLowerCase();
  }

  /**
   * Handles Markdown (.md) files by reading content as UTF-8 text
   * 
   * @param uploadedFile the uploaded MD file
   * @return string content of the markdown file
   * @throws IOException if error reading file
   */
  public static String handleMdFile(UploadedFile uploadedFile) throws IOException {
    if (uploadedFile == null) {
      return "";
    }

    try (InputStream inputStream = uploadedFile.getInputStream()) {
      byte[] bytes = inputStream.readAllBytes();
      return new String(bytes, StandardCharsets.UTF_8);
    }
  }
}