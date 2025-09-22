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

/**
 * Utility class for image processing and base64 conversion operations. Provides
 * methods to convert images to/from base64 strings, resize images, and validate
 * image formats.
 */
public final class ImageUtils {

  /**
   * Converts an InputStream containing image data to a base64 string.
   *
   * @param inputStream the input stream containing image data
   * @param fileName    the name of the file
   * @return base64 data URL string ready for HTML img src, or null if conversion
   *         fails
   */
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

  /**
   * Converts a base64 data URL string to clean base64 string for storage. Removes
   * the "data:image/type;base64," prefix.
   *
   * @param base64DataUrl the base64 data URL string (from inputStreamToBase64)
   * @return clean base64 string without data URL prefix, or null if conversion
   *         fails
   */
  public static String dataUrlToBase64(String base64DataUrl) {
    if (StringUtils.isBlank(base64DataUrl)) {
      return null;
    }

    return cleanBase64String(base64DataUrl);
  }

  /**
   * Converts a clean base64 string back to data URL for HTML display.
   *
   * @param base64String the clean base64 string (stored in database)
   * @param mimeType     the MIME type (e.g., "image/png")
   * @return base64 data URL string ready for HTML img src
   */
  public static String base64ToDataUrl(String base64String, String mimeType) {
    if (StringUtils.isBlank(base64String)) {
      return null;
    }

    if (StringUtils.isBlank(mimeType)) {
      mimeType = "image/png"; // default
    }

    return String.format("data:%s;base64,%s", mimeType, base64String);
  }

  /**
   * Converts a clean base64 string back to data URL with PNG format.
   *
   * @param base64String the clean base64 string (stored in database)
   * @return base64 data URL string ready for HTML img src
   */
  public static String base64ToDataUrl(String base64String) {
    return base64ToDataUrl(base64String, "image/png");
  }

  /**
   * Converts a base64 string to StreamedContent. Assumes PNG format for all
   * images.
   *
   * @param base64String the base64 encoded image with data URL prefix (from
   *                     inputStreamToBase64)
   * @return StreamedContent object for the image, or null if conversion fails
   */
  public static StreamedContent base64ToStreamedContent(String base64String) {
    if (StringUtils.isBlank(base64String)) {
      Ivy.log().warn("Base64 string is null or empty");
      return null;
    }

    try {
      // Use PNG as the default MIME type and filename
      String mimeType = "image/png";
      String fileName = "image.png";

      // Clean the base64 string (remove data URL prefix if present)
      String cleanBase64 = cleanBase64String(base64String);

      // Decode base64 to bytes
      byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

      // Build and return StreamedContent
      return DefaultStreamedContent.builder().name(fileName).contentType(mimeType)
          .stream(() -> new ByteArrayInputStream(imageBytes)).build();

    } catch (IllegalArgumentException e) {
      Ivy.log().error("Error decoding base64 string to StreamedContent: {0}", e);
      return null;
    }
  }

  /**
   * Gets the MIME type for an image based on its filename.
   *
   * @param filename the filename
   * @return MIME type string, or "application/octet-stream" if unknown
   */
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

  /**
   * Creates a data URL from base64 string and filename.
   *
   * @param base64String the base64 encoded image
   * @param filename     the original filename (used to determine MIME type)
   * @return data URL string (e.g., "data:image/png;base64,...")
   */
  public static String createDataUrl(String base64String, String filename) {
    if (StringUtils.isBlank(base64String)) {
      return null;
    }

    String mimeType = getMimeType(filename);
    String cleanBase64 = cleanBase64String(base64String);
    return String.format("data:%s;base64,%s", mimeType, cleanBase64);
  }

  /**
   * Extracts the file extension from a filename.
   *
   * @param filename the filename
   * @return file extension without the dot, or empty string if no extension
   */
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

  /**
   * Removes data URL prefix from base64 string if present.
   *
   * @param base64String the base64 string that might contain data URL prefix
   * @return clean base64 string without prefix
   */
  private static String cleanBase64String(String base64String) {
    if (StringUtils.isBlank(base64String)) {
      return base64String;
    }

    // Remove data URL prefix if present (e.g., "data:image/png;base64,")
    if (base64String.startsWith("data:")) {
      int commaIndex = base64String.indexOf(',');
      if (commaIndex != -1 && commaIndex < base64String.length() - 1) {
        return base64String.substring(commaIndex + 1);
      }
    }

    return base64String;
  }
}