package com.axonivy.utils.smart.workflow.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.rag.RagConf;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestEmbeddingModelFactory {

  @Test
  void providersContainsOnlyEmbeddingCapableProviders() {
    var providers = EmbeddingModelFactory.providers();

    providers.forEach(p ->
        assertThat(p.supportsEmbedding())
            .as("provider '%s' must support embedding to be listed", p.name())
            .isTrue());
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
