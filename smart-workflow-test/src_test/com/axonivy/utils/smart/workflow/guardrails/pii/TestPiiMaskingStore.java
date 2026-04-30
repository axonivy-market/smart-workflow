package com.axonivy.utils.smart.workflow.guardrails.pii;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
public class TestPiiMaskingStore {

  @Test
  void putAndGetRemoveReturnsMapping() {
    var mapping = Map.of("<EMAIL_abc12345678901>", "user@example.com");
    PiiMaskingStore.put("store-test-1", mapping);
    var result = PiiMaskingStore.getAndRemove("store-test-1");
    assertThat(result).isEqualTo(mapping);
  }

  @Test
  void getAndRemoveConsumesEntry() {
    PiiMaskingStore.put("store-test-2", Map.of("<IP_ADDRESS_abc123456789>", "192.168.1.1"));
    PiiMaskingStore.getAndRemove("store-test-2");
    var second = PiiMaskingStore.getAndRemove("store-test-2");
    assertThat(second).isEmpty();
  }

  @Test
  void absentKeyReturnsEmpty() {
    var result = PiiMaskingStore.getAndRemove("nonexistent-key-store-test");
    assertThat(result).isEmpty();
  }

  @Test
  void nullKeyReturnsEmpty() {
    var result = PiiMaskingStore.getAndRemove(null);
    assertThat(result).isEmpty();
  }

  @Test
  void multipleInvocationsAreIsolated() {
    PiiMaskingStore.put("store-test-a", Map.of("<EMAIL_aaaaaaaaaaaa>", "a@example.com"));
    PiiMaskingStore.put("store-test-b", Map.of("<EMAIL_bbbbbbbbbbbb>", "b@example.com"));
    assertThat(PiiMaskingStore.getAndRemove("store-test-a")).containsValue("a@example.com");
    assertThat(PiiMaskingStore.getAndRemove("store-test-b")).containsValue("b@example.com");
  }

  @Test
  void freshEntriesSurviveEviction() {
    PiiMaskingStore.put("store-test-fresh", Map.of("<EMAIL_xxxxxxxxxxxx>", "x@example.com"));
    PiiMaskingStore.evictExpired();
    assertThat(PiiMaskingStore.getAndRemove("store-test-fresh")).isNotEmpty();
  }
}
