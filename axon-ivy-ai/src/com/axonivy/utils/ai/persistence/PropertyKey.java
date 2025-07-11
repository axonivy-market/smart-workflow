package com.axonivy.utils.ai.persistence;

public final class PropertyKey {
  public static final String PROPERTY_START = "AxonIvyAi";
  public static final String ENTITY_PROPERTY_KEY_PREFIX = PROPERTY_START + ".%s";
  public static final String ENTITY_PROPERTY_KEY = ENTITY_PROPERTY_KEY_PREFIX + ".%d";
  public static final String ENTITY_INCREMENT_ID_KEY = PROPERTY_START + ".IncrementId";

  private PropertyKey() {
  }

}