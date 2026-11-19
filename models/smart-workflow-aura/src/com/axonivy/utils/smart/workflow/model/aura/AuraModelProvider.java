package com.axonivy.utils.smart.workflow.model.aura;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector.AuraConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;

public class AuraModelProvider implements ChatModelProvider {

  public static final String NAME = "Aura";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    var builder = AuraServiceConnector.buildOpenAiModel(options);
    if (options.structuredOutput()) {
      builder.supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
      builder.strictJsonSchema(true);
      builder.responseFormat("json_schema");
    }
    builder.listeners(options.listeners());
    return builder.build();
  }

  @Override
  public List<String> models() {
    var model = Ivy.var().get(AuraConf.DEFAULT_MODEL);
    return StringUtils.isBlank(model) ? List.of() : List.of(model);
  }

  @Override
  public List<String> secretsVars() {
    return List.of(AuraConf.API_KEY);
  }
}
