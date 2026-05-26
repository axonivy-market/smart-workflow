package com.axonivy.utils.smart.workflow.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.market.ProductJson;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestRagProviders {

  @ParameterizedTest
  @MethodSource("ragProviders")
  void installer_promotesProvider(SmartWorkflowToolsProvider provider) throws Exception {
    var artifactIds = ProductJson.installerArtifactIds();
    assertThat(artifactIds)
        .as("RAG provider is installable as extra market product")
        .contains(installerName(provider));
  }

  private static String installerName(SmartWorkflowToolsProvider provider) throws Exception {
    var vendor = StringUtils.substringBefore(provider.getClass().getSimpleName(), "RagToolProvider") ;
    return "smart-workflow-"+vendor.toLowerCase()+"-rag";
  }

  static Stream<SmartWorkflowToolsProvider> ragProviders() {
    var classLoader = TestRagProviders.class.getClassLoader();
    return SpiLoader.findImpl(SmartWorkflowToolsProvider.class, classLoader).stream()
        .filter(p -> p.getClass().getPackageName().contains(".rag."));
  }
}
