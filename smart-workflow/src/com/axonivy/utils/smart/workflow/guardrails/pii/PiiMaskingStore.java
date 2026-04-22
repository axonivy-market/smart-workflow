package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PiiMaskingStore {

  private static final long TTL_MS = 5 * 60 * 1000L;
  private static final int MAX_ENTRIES = 10_000;
  private static final int EVICTION_FREQUENCY = 20;

  private static final ConcurrentHashMap<String, Entry> STORE = new ConcurrentHashMap<>();
  private static int putCount = 0;

  record Entry(Map<String, String> placeholderToOriginal, long createdMs) {}

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

  static void evictExpired() {
    long now = System.currentTimeMillis();
    STORE.entrySet().removeIf(e -> now - e.getValue().createdMs() > TTL_MS);
  }

  static int size() {
    return STORE.size();
  }
}
