package com.axonivy.utils.smart.workflow.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.rag.RagConf;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestEmbeddingModelFactory {

  @SuppressWarnings("unused")
  static Stream<String> providerNames() {
    var classLoader = TestEmbeddingModelFactory.class.getClassLoader();
    return SpiLoader.findImpl(ChatModelProvider.class, classLoader).stream()
        .filter(ChatModelProvider::supportsEmbedding)
        .map(ChatModelProvider::name);
  }

  @ParameterizedTest
  @MethodSource("providerNames")
  void supportsEmbedding(String providerName) {
    var provider = EmbeddingModelFactory.providers().stream()
        .filter(p -> p.name().equals(providerName))
        .findFirst().orElseThrow();
    assertThat(provider.supportsEmbedding())
        .as("provider '%s' must support embedding to be listed", providerName)
        .isTrue();
  }

  @Test
  void dummyProviderIsNotInEmbeddingProviders() {
    var names = EmbeddingModelFactory.providers().stream()
        .map(p -> p.name())
        .toList();

    assertThat(names).doesNotContain(DummyChatModelProvider.NAME);
  }

  @Test
  void createReturnsEmptyForUnknownProvider() {
    assertThat(EmbeddingModelFactory.create("no-such-provider")).isEmpty();
  }

  @Test
  void getProviderOrDefaultThrowsForNonEmbeddingProvider() {
    assertThatThrownBy(() -> EmbeddingModelFactory.getProviderOrDefault(DummyChatModelProvider.NAME))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(DummyChatModelProvider.NAME);
  }

  @Test
  void getProviderOrDefaultThrowsForUnknownProvider() {
    assertThatThrownBy(() -> EmbeddingModelFactory.getProviderOrDefault("no-such-provider"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no-such-provider");
  }

  @Test
  void createFromIvyVarsThrowsWhenNotConfigured(AppFixture fixture) {
    fixture.var(RagConf.EMBEDDING_PROVIDER, "no-such-provider");
    assertThatThrownBy(EmbeddingModelFactory::createFromIvyVars)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no-such-provider");
  }
}
