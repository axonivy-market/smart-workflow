package com.axonivy.utils.smart.workflow.model.aura;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import dev.langchain4j.model.chat.ChatModel;

public class AuraModelProvider implements ChatModelProvider {

  private static final String MODEL_SELECTION_UNSUPPORTED = "Aura does not support model selection.";
  private static final String STRUCTURED_OUTPUT_UNSUPPORTED = "Aura structured output is not implemented yet.";
  private static final String TOOLS_UNSUPPORTED = "Aura tool calls are not implemented yet.";

  public static final String NAME = "Aura";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    if (StringUtils.isNotBlank(options.modelName())) {
      throw new IllegalArgumentException(MODEL_SELECTION_UNSUPPORTED);
    }
    if (options.structuredOutput()) {
      throw new IllegalArgumentException(STRUCTURED_OUTPUT_UNSUPPORTED);
    }
    if (options.hasTools()) {
      throw new IllegalArgumentException(TOOLS_UNSUPPORTED);
    }
    return AuraServiceConnector.buildAuraModel(options);
  }

  @Override
  public List<String> models() {
    return List.of();
  }

  @Override
  public List<String> secretsVars() {
    return List.of();
  }
}
