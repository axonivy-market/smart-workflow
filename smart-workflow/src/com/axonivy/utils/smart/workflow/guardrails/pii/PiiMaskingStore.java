package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.ivyteam.ivy.environment.Ivy;

public class PiiMaskingStore {

  public static final String TTL_VARIABLE = "AI.Guardrails.PiiMasking.MappingTtlMinutes";
  private static final long DEFAULT_TTL_MINUTES = 5;
  private static final int MAX_ENTRIES = 10_000;
  private static final int EVICTION_FREQUENCY = 20;

  private static final ConcurrentHashMap<String, Entry> STORE = new ConcurrentHashMap<>();
  private static int putCount = 0;

  record Entry(Map<String, String> placeholderToOriginal, long createdMs) {}

  static long ttlMs() {
    try {
      String value = Ivy.var().get(TTL_VARIABLE);
      if (value != null && !value.isBlank()) {
        return Long.parseLong(value.strip()) * 60 * 1000L;
      }
    } catch (Exception ignored) {}
    return DEFAULT_TTL_MINUTES * 60 * 1000L;
  }

  public static void put(String invocationId, Map<String, String> mapping) {
    if (STORE.size() >= MAX_ENTRIES) {
      evictExpired();
    }
    STORE.put(invocationId, new Entry(Map.copyOf(mapping), System.currentTimeMillis()));
    if (++putCount % EVICTION_FREQUENCY == 0) {
      evictExpired();
    }
  }

  public static Map<String, String> getAndRemove(String invocationId) {
    if (invocationId == null) {
      return Collections.emptyMap();
    }
    Entry entry = STORE.remove(invocationId);
    return entry != null ? entry.placeholderToOriginal() : Collections.emptyMap();
  }

  public static boolean containsKey(String invocationId) {
    return invocationId != null && STORE.containsKey(invocationId);
  }

  static void evictExpired() {
    long now = System.currentTimeMillis();
    long ttl = ttlMs();
    STORE.entrySet().removeIf(e -> now - e.getValue().createdMs() > ttl);
  }

  static int size() {
    return STORE.size();
  }
}
