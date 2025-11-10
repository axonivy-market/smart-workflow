package com.axonivy.utils.smart.workflow.azureopenai.internal;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.azureopenai.AzureAiDeployment;
import com.axonivy.utils.smart.workflow.azureopenai.utlis.VariableUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.Capability;

public class AzureOpenAiServiceConnector {
  private static final int DEFAULT_TEMPERATURE = 0;

  // Temporary model name and temperature for GPT-5. Will remove once LangChain4j
  // fully support GPT-5
  private static final String GPT_5 = "gpt-5";
  private static final int DEFAULT_TEMPERATURE_GPT_5 = 1;

  public interface AzureOpenAiConf {
    String PREFIX = "Ai.AzureOpenAI.";
    String ENDPOINT = PREFIX + "Endpoint";
    String API_KEY = PREFIX + "APIKey";
    String DEPLOYMENTS = PREFIX + "Deployments";
  }

  public static AzureOpenAiChatModel.Builder buildOpenAiModel(String deploymentName) {
    return initBuilder(deploymentName);
  }

  public static AzureOpenAiChatModel.Builder buildJsonOpenAiModel(String deploymentName) {
    return initBuilder(deploymentName).supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  private static AzureOpenAiChatModel.Builder initBuilder(String deploymentName) {

    AzureAiDeployment deployment = VariableUtils.getDeploymentByName(deploymentName);
    String endpoint = Ivy.var().get(AzureOpenAiConf.ENDPOINT);

    if (Objects.isNull(deployment) || StringUtils.isBlank(endpoint)) {
      return null;
    }

    AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
        .endpoint(endpoint).deploymentName(deployment.getName())
        .logRequestsAndResponses(true);

 // Only set temperature if not using the "o" series
 if (!deployment.getModel().startsWith("o")) {
   Double temperature = Double
       .valueOf(GPT_5.equalsIgnoreCase(deployment.getModel()) ? DEFAULT_TEMPERATURE_GPT_5 : DEFAULT_TEMPERATURE);
      builder.temperature(temperature);
    }

    String key = deployment.getApiKey();
    if (!key.isBlank()) {
      builder.apiKey(key);
    } else {
      builder.customHeaders(Map.of("X-Requested-By", "ivy")); // TODO as pure test variable
    }

    return builder;
  }
}