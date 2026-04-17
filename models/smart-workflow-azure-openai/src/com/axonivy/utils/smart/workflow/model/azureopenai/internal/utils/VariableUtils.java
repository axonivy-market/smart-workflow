package com.axonivy.utils.smart.workflow.model.azureopenai.internal.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiConf;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.entity.AzureAiDeployment;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.vars.Variable;

public final class VariableUtils {

  public static List<AzureAiDeployment> getDeployments() {
    var deploymentVars = deploymentsVars();
    if (deploymentVars.isEmpty()) {
      return List.of();
    }
    Map<String, AzureAiDeployment> deploymentMap = new HashMap<>();
    for (Variable variable : deploymentVars) {
      String[] parts = variable.name().split("\\.");
      if (parts.length == 6) {
        String deploymentName = parts[4];
        String fieldName = parts[5]; // e.g., "Model" or "APIKey"
        deploymentMap.computeIfAbsent(deploymentName, AzureAiDeployment::new);
        AzureAiDeployment deployment = deploymentMap.get(deploymentName);
        if (AzureOpenAiConf.MODEL_FIELD.equals(fieldName)) {
          deployment.setModel(variable.value());
        } else if (AzureOpenAiConf.API_KEY_FIELD.equals(fieldName)) {
          deployment.setApiKey(variable.value());
        }
      }
    }

    return deploymentMap.values().stream()
        .filter(deployment -> deployment.getName() != null && !deployment.getName().isEmpty())
        .toList();
  }

  public static List<Variable> deploymentsVars(){
    return Ivy.var().all().stream()
        .filter(variable -> variable.name().startsWith(AzureOpenAiConf.DEPLOYMENTS))
        .filter(variable -> !variable.name().equals(AzureOpenAiConf.DEPLOYMENTS))
        .toList();
  }

  public static AzureAiDeployment getDeploymentByName(String deploymentName) {
    if (StringUtils.isBlank(deploymentName)) {
      return null;
    }

    return getDeployments().stream()
      .filter(deployment -> deploymentName.equals(deployment.getName()))
      .findFirst()
      .orElse(null);
  }
}
