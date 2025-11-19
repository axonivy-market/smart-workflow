package com.axonivy.utils.smart.workflow.model.azureopenai.internal;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.azureopenai.internal.entity.AzureAiDeployment;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.utils.VariableUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;

public class AzureOpenAiServiceConnector {
  private static final int DEFAULT_TEMPERATURE = 0;

  // Temporary model name and temperature for GPT-5. Will remove once LangChain4j
  // fully support GPT-5
  private static final String GPT_5 = "gpt-5";
  private static final int DEFAULT_TEMPERATURE_GPT_5 = 1;

  public interface AzureOpenAiConf {
    String PREFIX = "AI.Providers.AzureOpenAI.";
    String ENDPOINT = PREFIX + "Endpoint";
    String DEPLOYMENTS = PREFIX + "Deployments";
    String DEFAULT_DEPLOYMENT = PREFIX + "DefaultDeployment";
  }

  public static AzureOpenAiChatModel.Builder buildOpenAiModel(String deploymentName) {
    return initBuilder(StringUtils.defaultIfBlank(deploymentName, Ivy.var().get(AzureOpenAiConf.DEFAULT_DEPLOYMENT)));
  }

  private static AzureOpenAiChatModel.Builder initBuilder(String deploymentName) {
    String endpoint = Ivy.var().get(AzureOpenAiConf.ENDPOINT);
    AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder().endpoint(endpoint)
        .logRequestsAndResponses(true);

    // TODO as pure test variable
    if (StringUtils.isBlank(deploymentName)) {
      return builder.customHeaders(Map.of("X-Requested-By", "ivy")).deploymentName("test").apiKey("test");
    }

    AzureAiDeployment deployment = VariableUtils.getDeploymentByName(deploymentName);
    if (Objects.isNull(deployment) || StringUtils.isBlank(endpoint)) {
      return null;
    }

    builder.deploymentName(deployment.getName()).apiKey(deployment.getApiKey());

    // Only set temperature if not using the "o" series
    if (!deployment.getModel().startsWith("o")) {
      Double temperature = Double
          .valueOf(GPT_5.equalsIgnoreCase(deployment.getModel()) ? DEFAULT_TEMPERATURE_GPT_5 : DEFAULT_TEMPERATURE);
      builder.temperature(temperature);
    }

    return builder;
  }
}