package com.axonivy.utils.smart.workflow.demo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.Binary;

public class FileProcessingUtils {

  private static final String FAILED_TO_LOAD_CMS_FILE_MSG = "Failed to load file from CMS: %s";

  public static InputStream loadInputStreamFromCms(String cmsPath) {
    Optional<ContentObject> contentObject = Ivy.cm().findObject(cmsPath);
    if (!contentObject.map(ContentObject::exists).orElse(false)) {
      return null;
    }
    return contentObject.map(ContentObject::values)
                        .map(values -> values.getFirst().read().inputStream())
                        .orElse(null);
  }

  public static Binary loadBinaryFromCms(String cmsPath) {
    Optional<ContentObject> contentObject = Ivy.cm().findObject(cmsPath);
    try {
      if (!contentObject.map(ContentObject::exists).orElse(false)) {
        return null;
      }
      try (InputStream is = contentObject.map(ContentObject::values)
                                         .map(values -> values.getFirst().read().inputStream())
                                         .orElse(null)) {
        return new Binary(is.readAllBytes());
      }
    } catch (IOException e) {
      String message = String.format(FAILED_TO_LOAD_CMS_FILE_MSG, cmsPath);
      Ivy.log().error(message, e);
      throw new RuntimeException(message, e);
    }
  }
}
