package ch.ivyteam.test.client;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class OpenAiTestClient {

  public static String localMockApiUrl() {
    return Ivy.rest().client("mockClient").getUri().toASCIIString();
  }

  public static OpenAiChatModel aiMock() {
    return new OpenAiServiceConnector().buildOpenAiModel().build();
  }

  public static OpenAiChatModel structuredOutputAiMock() {
    return new OpenAiServiceConnector().buildJsonOpenAiModel().build();
  }
}
