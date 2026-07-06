package com.axonivy.utils.smart.workflow.demo.assistant;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class UploadedDocumentEntryFactory {

  private UploadedDocumentEntryFactory() {}

  public static UploadedDocumentEntry of(String fileName, byte[] data) {
    UploadedDocumentEntry entry = new UploadedDocumentEntry();
    entry.setFileName(fileName);
    entry.setData(data);
    return entry;
  }

  public static boolean isPdf(UploadedDocumentEntry entry) {
    String name = entry.getFileName();
    return name != null && name.toLowerCase(Locale.ROOT).endsWith(".pdf");
  }

  public static String getContent(UploadedDocumentEntry entry) {
    return new String(entry.getData(), StandardCharsets.UTF_8);
  }

  public static InputStream getInputStream(UploadedDocumentEntry entry) {
    return new ByteArrayInputStream(entry.getData());
  }
}
