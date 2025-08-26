package com.axonivy.utils.smart.orchestrator.utils;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public final class IdGenerationUtils {

  public static String generateRandomId() {
    return UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY);
  }
}
