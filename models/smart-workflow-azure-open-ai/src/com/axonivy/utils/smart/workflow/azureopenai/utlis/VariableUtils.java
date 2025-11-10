package com.axonivy.utils.smart.workflow.azureopenai.utlis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.azureopenai.AzureAiDeployment;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.vars.Variable;

public final class VariableUtils {
  private static final String AZURE_DEPLOYMENTS_PREFIX = "AI.AzureOpenAI.Deployments";
  private static final String MODEL_FIELD = "Model";
  private static final String API_KEY_FIELD = "APIKey";

  public static List<AzureAiDeployment> getDeployments() {
    List<Variable> deploymentVariables = Ivy.var().all().stream()
        .filter(variable -> variable.name().startsWith(AZURE_DEPLOYMENTS_PREFIX))
        .filter(variable -> !variable.name().equals(AZURE_DEPLOYMENTS_PREFIX)).collect(Collectors.toList());

    if (deploymentVariables.size() == 0) {
      return null;
    }

    Map<String, AzureAiDeployment> deploymentMap = new HashMap<>();
    for (Variable variable : deploymentVariables) {
      String[] parts = variable.name().split("\\.");
      if (parts.length == 5) {
        String deploymentName = parts[3];
        String fieldName = parts[4]; // e.g., "Model" or "APIKey"
        deploymentMap.computeIfAbsent(deploymentName, depployment -> new AzureAiDeployment(deploymentName));
        AzureAiDeployment deployment = deploymentMap.get(deploymentName);
        if (MODEL_FIELD.equals(fieldName)) {
          deployment.setModel(variable.value());
        } else if (API_KEY_FIELD.equals(fieldName)) {
          deployment.setApiKey(variable.value());
        }
      }
    }

    return deploymentMap.values().stream()
        .filter(deployment -> deployment.getName() != null && !deployment.getName().isEmpty())
        .collect(Collectors.toList());
  }

  public static AzureAiDeployment getDeploymentByName(String deploymentName) {
    if (StringUtils.isBlank(deploymentName)) {
      return null;
    }

    List<AzureAiDeployment> deployments = getDeployments();

    if (CollectionUtils.isEmpty(deployments)) {
      return null;
    }

    return deployments.stream().filter(deployment -> deploymentName.equals(deployment.getName())).findFirst()
        .orElse(null);
  }
}
