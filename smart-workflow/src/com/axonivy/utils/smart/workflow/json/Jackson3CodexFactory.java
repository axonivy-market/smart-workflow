package com.axonivy.utils.smart.workflow.json;

import dev.langchain4j.internal.Json.JsonCodec;
import dev.langchain4j.spi.json.JsonCodecFactory;

public class Jackson3CodexFactory implements JsonCodecFactory {

  @Override
  public JsonCodec create() {
    return new Jackson3Codec();
  }

}
