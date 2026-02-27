package com.axonivy.utils.smart.workflow.extraction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.environment.Ivy;

public class CmsLoader {

  private static final String UNSUPPORTED_FILE_TYPE_MSG = "Unsupported CMS file type for object: %s with extension: %s";
  private static final String FAILED_TO_READ_FILE_MSG = "Failed to read CMS file content for object: %s with extension: %s";

  public static Object load(String cmsPath) {
    return Ivy.cm().findObject(cmsPath)
        .map(obj -> resolve(obj, cmsPath))
        .orElse(null);
  }

  private static Object resolve(ContentObject obj, String cmsPath) {
    return switch (obj.meta().contentObjectType()) {
      case STRING -> Ivy.cms().co(cmsPath);
      case FILE   -> extractFromFile(obj);
      default     -> "";
    };
  }

  private static Object extractFromFile(ContentObject obj) {
    String extension = Optional.ofNullable(obj.meta().fileExtension()).orElse("").toLowerCase();
    ContentObjectValue value = obj.values().getFirst();

    return switch(extension) {
      case "png", "jpg", "jpeg", "pdf" -> value.read().inputStream();
      case "txt", "md" -> {
        try (var stream = value.read().inputStream()) {
          yield new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new RuntimeException(String.format(FAILED_TO_READ_FILE_MSG, obj.name(), extension), e);
        }
      }
      default -> throw new RuntimeException(String.format(UNSUPPORTED_FILE_TYPE_MSG, obj.name(), extension));
    };
  }
}
