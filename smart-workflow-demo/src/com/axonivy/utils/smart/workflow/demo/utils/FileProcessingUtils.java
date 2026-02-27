package com.axonivy.utils.smart.workflow.demo.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.Binary;

public class FileProcessingUtils {

  private static final String FAILED_TO_LOAD_CMS_FILE_MSG = "Failed to load file from CMS: %s/%s%s";

  public static InputStream loadInputStreamFromCms(String cmsPath) {
    ContentObject contentObject = Ivy.cm().findObject(cmsPath).get();
    if (!contentObject.exists()) {
      return null;
    }
    return contentObject.values().getFirst().read().inputStream();
  }

  public static Binary loadBinaryFromCms(String cmsPath) {
    ContentObject contentObject = Ivy.cm().findObject(cmsPath).get();
    try {
      if (!contentObject.exists()) {
        return null;
      }
      try (InputStream is = contentObject.values().getFirst().read().inputStream()) {
        byte[] bytes = is.readAllBytes();
        return bytes != null ? new Binary(bytes) : null;
      }
    } catch (IOException e) {
      String message = String.format(FAILED_TO_LOAD_CMS_FILE_MSG, cmsPath, "", "");
      Ivy.log().error(message, e);
      throw new RuntimeException(message, e);
    }
  }
}
