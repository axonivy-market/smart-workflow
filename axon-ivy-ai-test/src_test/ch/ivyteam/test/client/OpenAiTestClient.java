package ch.ivyteam.test.client;

import java.util.Map;

import org.glassfish.jersey.client.filter.CsrfProtectionFilter;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;

public class OpenAiTestClient {

  public static OpenAiChatModelBuilder mockBuilder() {
    var localApi = Ivy.rest().client("mockClient").getUri().toASCIIString();
    return new OpenAiServiceConnector()
        .buildOpenAiModel()
        .baseUrl(localApi)
        .customHeaders(Map.of(CsrfProtectionFilter.HEADER_NAME, "ivy"));
  }

  public static OpenAiChatModel aiMock() {
    return mockBuilder().build();
  }

}
