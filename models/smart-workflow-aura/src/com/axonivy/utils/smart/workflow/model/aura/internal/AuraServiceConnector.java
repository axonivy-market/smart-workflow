package com.axonivy.utils.smart.workflow.model.aura.internal;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import jakarta.ws.rs.client.WebTarget;

public class AuraServiceConnector {

  private static final String AURA_INFERENCE_CLIENT = "auraInferenceEndpoint";

  public interface AuraConf {
    String PREFIX = "AI.Providers.Aura.";
    String ORG_ID = PREFIX + "OrgId";
    String HOST = PREFIX + "Host";
    String APP_ID = PREFIX + "AppId";
  }

  public static AuraChatModel buildAuraModel(ModelOptions options) {
    requireConfigured(AuraConf.ORG_ID);
    requireConfigured(AuraConf.HOST);
    requireConfigured(AuraConf.APP_ID);
    var target = Ivy.rest().client(AURA_INFERENCE_CLIENT)
        .resolveTemplate("host", Ivy.var().get(AuraConf.HOST))
        .resolveTemplate("orgId", Ivy.var().get(AuraConf.ORG_ID));
    return buildAuraModel(options, target);
  }

  static AuraChatModel buildAuraModel(ModelOptions options, WebTarget target) {
    return new AuraChatModel(options, target);
  }

  private static void requireConfigured(String variable) {
    if (StringUtils.isBlank(Ivy.var().get(variable))) {
      throw new IllegalArgumentException("Configure " + variable + " before calling Aura");
    }
  }
}
