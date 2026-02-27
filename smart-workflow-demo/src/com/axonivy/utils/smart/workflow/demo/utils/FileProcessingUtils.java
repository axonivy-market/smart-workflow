package com.axonivy.utils.smart.workflow.demo.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.Binary;

import com.axonivy.utils.smart.workflow.exception.SmartWorkflowException;

public class FileProcessingUtils {

  private static final String FAILED_TO_LOAD_CMS_FILE_MSG = "Failed to load file from CMS: %s/%s%s";
  private static final String TEMP_FILE_PREFIX = "cmsfile_";

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
      throw new SmartWorkflowException(message, e);
    }
  }

  public static File loadFileFromCms(String cmsPath, String filename, String fileExtension) {
    ContentObject contentObject = Ivy.cm().findObject(cmsPath).get();
    try {
      if (!contentObject.exists()) {
        return null;
      }

      byte[] bytes;
      try (InputStream is = contentObject.values().getFirst().read().inputStream()) {
        bytes = is.readAllBytes();
      }
      if (bytes != null) {
        File tempFile = Files.createTempFile(TEMP_FILE_PREFIX, "." + fileExtension).toFile();
        Files.write(tempFile.toPath(), bytes);
        return tempFile;
      }
    } catch (IOException e) {
      String message = String.format(FAILED_TO_LOAD_CMS_FILE_MSG, cmsPath, filename, fileExtension);
      Ivy.log().error(message, e);
      throw new SmartWorkflowException(message, e);
    }
    return null;
  }
}
