package ch.ivyteam.test.client;

import java.util.Map;

import org.glassfish.jersey.client.filter.CsrfProtectionFilter;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.mock.MockOpenAI;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;

public class OpenAiTestClient {

  public static OpenAiChatModelBuilder mockBuilder() {
    var localApi = EngineUrl.createRestUrl(MockOpenAI.PATH_SUFFIX);
    return new OpenAiServiceConnector()
        .buildOpenAiModel()
        .baseUrl(localApi)
        .customHeaders(Map.of(CsrfProtectionFilter.HEADER_NAME, "ivy"));
  }

  public static OpenAiChatModel aiMock() {
    return mockBuilder().build();
  }

}
