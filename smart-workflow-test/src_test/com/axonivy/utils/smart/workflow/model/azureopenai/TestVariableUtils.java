package com.axonivy.utils.smart.workflow.model.azureopenai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.azureopenai.AzureAiDeployment;
import com.axonivy.utils.smart.workflow.azureopenai.utlis.VariableUtils;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestVariableUtils {
  private static final String DEPLOYMENTS_PREFIX = "AI.Providers.AzureOpenAI.Deployments";
  private static final String TEST_DEPLOYMENT_1 = "test-gpt-4-1-mini";
  private static final String TEST_DEPLOYMENT_2 = "test-o4-mini";

  private static final String MODEL_1 = "gpt-4.1-mini";
  private static final String MODEL_2 = "o4-mini";

  private static final String API_KEY_1 = "${decrypt:test-key-1}";
  private static final String DECRYPTED_API_KEY_1 = "test-key-1";
  private static final String API_KEY_2 = "${decrypt:test-key-2}";

  @BeforeEach
  void setup(AppFixture fixture) {
    // Setup test deployments with Model and APIKey fields
    fixture.var(DEPLOYMENTS_PREFIX + "." + TEST_DEPLOYMENT_1 + ".Model", MODEL_1);
    fixture.var(DEPLOYMENTS_PREFIX + "." + TEST_DEPLOYMENT_1 + ".APIKey", API_KEY_1);

    fixture.var(DEPLOYMENTS_PREFIX + "." + TEST_DEPLOYMENT_2 + ".Model", MODEL_2);
    fixture.var(DEPLOYMENTS_PREFIX + "." + TEST_DEPLOYMENT_2 + ".APIKey", API_KEY_2);
  }

  @Test
  void getAllDeployments() {
    List<AzureAiDeployment> deployments = VariableUtils.getDeployments();

    assertThat(deployments).isNotNull();
    assertThat(deployments).hasSize(2);

    List<String> deploymentNames = deployments.stream().map(AzureAiDeployment::getName).toList();

    assertThat(deploymentNames).containsExactlyInAnyOrder(TEST_DEPLOYMENT_1, TEST_DEPLOYMENT_2);
  }

  @Test
  void containCorrectModelAndApiKey() {
    List<AzureAiDeployment> deployments = VariableUtils.getDeployments();

    AzureAiDeployment deployment1 = deployments.stream().filter(d -> TEST_DEPLOYMENT_1.equals(d.getName())).findFirst()
        .orElse(null);

    assertThat(deployment1).isNotNull();
    assertThat(deployment1.getName()).isEqualTo(TEST_DEPLOYMENT_1);
    assertThat(deployment1.getModel()).isEqualTo(MODEL_1);
    assertThat(deployment1.getApiKey()).isEqualTo(DECRYPTED_API_KEY_1);
  }

  @Test
  void nonExistentDeployment() {
    AzureAiDeployment deployment = VariableUtils.getDeploymentByName("non-existent-deployment");

    assertThat(deployment).isNull();
  }

  @Test
  void returnNullForBlankName() {
    assertThat(VariableUtils.getDeploymentByName("")).isNull();
    assertThat(VariableUtils.getDeploymentByName("   ")).isNull();
    assertThat(VariableUtils.getDeploymentByName(null)).isNull();
  }

  @Test
  void shouldFilterOutEmptyNameDeployments(AppFixture fixture) {
    // This test verifies that deployments with empty names are filtered out
    List<AzureAiDeployment> deployments = VariableUtils.getDeployments();

    assertThat(deployments).isNotNull();
    assertThat(deployments).allMatch(d -> d.getName() != null && !d.getName().isEmpty());
  }

  @Test
  void matchExactName() {
    // Test exact name matching
    AzureAiDeployment deployment1 = VariableUtils.getDeploymentByName(TEST_DEPLOYMENT_1);
    AzureAiDeployment deployment2 = VariableUtils.getDeploymentByName(TEST_DEPLOYMENT_2);

    assertThat(deployment1.getName()).isEqualTo(TEST_DEPLOYMENT_1);
    assertThat(deployment2.getName()).isEqualTo(TEST_DEPLOYMENT_2);

    // Verify each has unique model
    assertThat(deployment1.getModel()).isEqualTo(MODEL_1);
    assertThat(deployment2.getModel()).isEqualTo(MODEL_2);
  }
}
