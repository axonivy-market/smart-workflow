package com.axonivy.utils.smart.workflow.model.azureopenai.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.axonivy.utils.smart.workflow.model.azureopenai.internal.utils.VariableUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

public class AzureOpenAiServiceConnector {
  private static final int DEFAULT_TEMPERATURE = 0;

  // Temporary model name and temperature for GPT-5. Will remove once LangChain4j
  // fully support GPT-5
  private static final String GPT_5 = "gpt-5";
  private static final int DEFAULT_TEMPERATURE_GPT_5 = 1;

  public static AzureOpenAiChatModel.Builder buildOpenAiModel(String deploymentName) {
    return initBuilder(StringUtils.defaultIfBlank(deploymentName, Ivy.var().get(AzureOpenAiConf.DEFAULT_DEPLOYMENT)));
  }

  private static AzureOpenAiChatModel.Builder initBuilder(String deploymentName) {
    String endpoint = Ivy.var().get(AzureOpenAiConf.ENDPOINT);
    var builder = AzureOpenAiChatModel.builder()
      .endpoint(endpoint)
      .logRequestsAndResponses(true);

    // TODO as pure test variable
    if (StringUtils.isBlank(deploymentName)) {
      return builder
        .customHeaders(Map.of("X-Requested-By", "ivy"))
        .deploymentName("test")
        .apiKey("test");
    }

    var deployment = VariableUtils.getDeploymentByName(deploymentName);
    if (Objects.isNull(deployment) || StringUtils.isBlank(endpoint)) {
      return null;
    }

    builder
      .deploymentName(deployment.getName())
      .apiKey(deployment.getApiKey());
    var request = ChatRequestParameters.builder()
      .modelName(deployment.getModel());
    temperature(deployment.getModel())
      .ifPresent(request::temperature);
    builder.defaultRequestParameters(request.build()); 

    return builder;
  }

  private static Optional<Double> temperature(String modelName) {
    if (modelName.startsWith("o")) {
      // Only set temperature if not using the "o" series
      return Optional.empty();
    }
    if (Strings.CI.startsWith(modelName, GPT_5)) {
      return Optional.of(Double.valueOf(DEFAULT_TEMPERATURE_GPT_5));
    }
    return Optional.of(Double.valueOf(DEFAULT_TEMPERATURE));
  }
}