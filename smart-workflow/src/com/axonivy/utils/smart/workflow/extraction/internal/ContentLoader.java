package com.axonivy.utils.smart.workflow.extraction.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.TextContent;

public class ContentLoader {

  private static final byte[] PDF_PREFIX = "%PDF-".getBytes(StandardCharsets.US_ASCII);
  private static final String FAILED_TO_READ_CMS_MSG = "Failed to read CMS file content for object: %s with extension: %s";
  private static final String FAILED_TO_READ_STREAM_MSG = "Failed to read file '%s'";
  private static final String UNSUPPORTED_CMS_TYPE_MSG = "Unsupported CMS file type for object: %s with extension: %s";

  public static Optional<Content> loadFromCms(String cmsPath) {
    return Ivy.cm().findObject(cmsPath)
        .flatMap(obj -> resolveCmsObject(obj, cmsPath));
  }

  private static Optional<Content> resolveCmsObject(ContentObject obj, String cmsPath) {
    return switch (obj.meta().contentObjectType()) {
      case STRING -> Optional.of(TextContent.from(Ivy.cms().co(cmsPath)));
      case FILE   -> extractFromCmsFile(obj);
      default     -> Optional.empty();
    };
  }

  private static Optional<Content> extractFromCmsFile(ContentObject obj) {
    String extension = Optional.ofNullable(obj.meta().fileExtension()).orElse("").toLowerCase();
    ContentObjectValue value = obj.values().getFirst();

    return switch (extension) {
      case "png", "jpg", "jpeg", "pdf" -> fromStream(value.read().inputStream(), obj.name() + "." + extension);
      case "txt", "md" -> {
        try (var stream = value.read().inputStream()) {
          yield Optional.of(TextContent.from(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
          throw new RuntimeException(String.format(FAILED_TO_READ_CMS_MSG, obj.name(), extension), e);
        }
      }
      default -> throw new RuntimeException(String.format(UNSUPPORTED_CMS_TYPE_MSG, obj.name(), extension));
    };
  }

  public static Optional<Content> fromStream(InputStream stream, String fileName) {
    if (stream == null) {
      return Optional.empty();
    }
    String resolvedFilename = Optional.ofNullable(fileName).orElse("unknown file name");
    try (stream) {
      byte[] bytes = stream.readAllBytes();
      return createContent(bytes, fileName);
    } catch (IOException e) {
      throw new RuntimeException(String.format(FAILED_TO_READ_STREAM_MSG, resolvedFilename), e);
    }
  }

  private static Optional<Content> createContent(byte[] bytes, String fileName) {
    String base64 = Base64.getEncoder().encodeToString(bytes);
    String extension = Optional.ofNullable(fileName)
        .map(FilenameUtils::getExtension)
        .map(String::toLowerCase)
        .or(() -> detectExtension(bytes))
        .orElse("");

    return switch (extension) {
      case "png" -> Optional.of(ImageContent.from(base64, "image/png", ImageContent.DetailLevel.HIGH));
      case "jpg", "jpeg" -> Optional.of(ImageContent.from(base64, "image/jpeg", ImageContent.DetailLevel.HIGH));
      case "pdf" -> Optional.of(PdfFileContent.from(base64, "application/pdf"));
      default -> Optional.empty();
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
