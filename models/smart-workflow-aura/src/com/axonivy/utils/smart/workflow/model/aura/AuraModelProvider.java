package com.axonivy.utils.smart.workflow.model.aura;

import java.util.List;

import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector.AuraConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import dev.langchain4j.model.chat.ChatModel;

public class AuraModelProvider implements ChatModelProvider {

  public static final String NAME = "Aura";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    return AuraServiceConnector.buildChatModel(options);
  }

  @Override
  public List<String> models() {
    return List.of(); // The inference API does not expose model selection.
  }

  @Override
  public List<String> secretsVars() {
    return List.of(AuraConf.BEARER_TOKEN);
  }
}
