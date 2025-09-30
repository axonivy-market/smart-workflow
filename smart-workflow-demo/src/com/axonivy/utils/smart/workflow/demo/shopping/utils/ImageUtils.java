package com.axonivy.utils.smart.workflow.demo.shopping.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import ch.ivyteam.ivy.environment.Ivy;

public final class ImageUtils {

  public static String inputStreamToBase64(InputStream inputStream, String fileName) {
    if (inputStream == null) {
      Ivy.log().warn("Input stream is null");
      return null;
    }

    try {
      byte[] imageBytes = IOUtils.toByteArray(inputStream);
      return createDataUrl(Base64.getEncoder().encodeToString(imageBytes), fileName);
    } catch (IOException e) {
      Ivy.log().error("Error converting input stream to base64: {0}", e);
      return null;
    }
  }

  public static String dataUrlToBase64(String base64DataUrl) {
    if (StringUtils.isBlank(base64DataUrl)) {
      return null;
    }

    return cleanBase64String(base64DataUrl);
  }

  public static String base64ToDataUrl(String base64String, String mimeType) {
    if (StringUtils.isBlank(base64String)) {
      return null;
    }

    if (StringUtils.isBlank(mimeType)) {
      mimeType = "image/jpeg"; // default
    }

    return String.format("data:%s;base64,%s", mimeType, base64String);
  }

  public static String base64ToDataUrl(String base64String) {
    return base64ToDataUrl(base64String, "image/jpeg");
  }

  public static StreamedContent base64ToStreamedContent(String base64String) {
    if (StringUtils.isBlank(base64String)) {
      Ivy.log().warn("Base64 string is null or empty");
      return null;
    }

    try {
      // Use JPG as the default MIME type and filename
      String mimeType = "image/jpeg";
      String fileName = "image.jpg";

      String cleanBase64 = cleanBase64String(base64String);

      byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

      return DefaultStreamedContent.builder().name(fileName).contentType(mimeType)
          .stream(() -> new ByteArrayInputStream(imageBytes)).build();

    } catch (IllegalArgumentException e) {
      Ivy.log().error("Error decoding base64 string to StreamedContent: {0}", e);
      return null;
    }
  }

  private static String getMimeType(String filename) {
    if (StringUtils.isBlank(filename)) {
      return "application/octet-stream";
    }

    String extension = getFileExtension(filename).toLowerCase();
    switch (extension) {
    case "jpg":
    case "jpeg":
      return "image/jpeg";
    case "png":
      return "image/png";
    case "gif":
      return "image/gif";
    case "bmp":
      return "image/bmp";
    case "webp":
      return "image/webp";
    default:
      return "application/octet-stream";
    }
  }

  public static String createDataUrl(String base64String, String filename) {
    if (StringUtils.isBlank(base64String)) {
      return null;
    }

    String mimeType = getMimeType(filename);
    String cleanBase64 = cleanBase64String(base64String);
    return String.format("data:%s;base64,%s", mimeType, cleanBase64);
  }

  private static String getFileExtension(String filename) {
    if (StringUtils.isBlank(filename)) {
      return "";
    }

    int lastDotIndex = filename.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
      return "";
    }

    return filename.substring(lastDotIndex + 1);
  }

  private static String cleanBase64String(String base64String) {
    if (StringUtils.isBlank(base64String)) {
      return base64String;
    }

    if (base64String.startsWith("data:")) {
      int commaIndex = base64String.indexOf(',');
      if (commaIndex != -1 && commaIndex < base64String.length() - 1) {
        return base64String.substring(commaIndex + 1);
      }
    }

    return base64String;
  }
}