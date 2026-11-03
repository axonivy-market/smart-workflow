package com.axonivy.utils.smart.workflow.components.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.components.chatbot.bean.ChatbotBean;
import com.axonivy.utils.smart.workflow.components.chatbot.message.entity.SmartWorkflowMessage;
import com.axonivy.utils.smart.workflow.components.chatbot.message.enums.MessageRole;
import com.axonivy.utils.smart.workflow.components.chatbot.mock.ChatbotChat;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.json.JsonMapper;

@RestResourceTest
class TestChatbotBean {

  private static final int MAX_MESSAGE_LENGTH = 2000;

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private Boolean accepted;
  private ChatbotBean bean;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("chatbot-bean"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(GuardrailCollector.DEFAULT_INPUT_GUARDRAILS, "");
    fixture.var(GuardrailCollector.DEFAULT_OUTPUT_GUARDRAILS, "");
    fixture.var("AI.CircuitBreaker.Enabled", "false");

    // PrimeFaces needs a JSF request, so record the callback param instead
    bean = new ChatbotBean() {
      @Override
      protected void reportAccepted(boolean value) {
        accepted = value;
      }
    };
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  \r\n  "})
  void sendMessage_blank_isRejected(String message) {
    bean.setUserMessage(message);

    bean.sendMessage();

    verifyAccepted(false);
    assertThat(bean.getChatHistory()).isEmpty();
    assertThat(bean.getError()).isNull();
  }

  @Test
  void sendMessage_tooLong_isRejectedWithoutRetry() {
    var ai = ChatbotChat.answering("unused");
    String tooLong = "a".repeat(MAX_MESSAGE_LENGTH + 1);
    bean.setUserMessage(tooLong);

    bean.sendMessage();

    verifyAccepted(false);
    assertThat(bean.getChatHistory()).isEmpty();
    assertThat(bean.getUserMessage()).isEqualTo(tooLong);
    assertThat(bean.getError().isRetryAllowed()).isFalse();
    assertThat(bean.getError().getContent()).contains(String.valueOf(MAX_MESSAGE_LENGTH));

    bean.retry();
    bean.answer();
    assertThat(ai.requests()).isEmpty();

    ask("short");
    assertThat(bean.getError()).isNull();
  }

  @Test
  void sendMessage_countsWindowsLineBreakAsOneCharacter() {
    // the browser's maxlength counts \r\n as one character
    String half = "a".repeat(MAX_MESSAGE_LENGTH / 2);
    bean.setUserMessage(half + "\r\n" + half.substring(1));

    bean.sendMessage();

    verifyAccepted(true);
    assertThat(bean.getChatHistory().getLast().getContent())
        .hasSize(MAX_MESSAGE_LENGTH)
        .contains("\n")
        .doesNotContain("\r");
  }

  @Test
  void conversation_answersEachQuestionOnce() {
    var ai = ChatbotChat.answering("Hello!");

    bean.answer();
    assertThat(ai.requests()).as("nothing to answer yet").isEmpty();

    ask("  Hi  ");
    verifyAccepted(true);
    assertThat(bean.getUserMessage()).isEmpty();

    bean.answer();
    bean.retry();
    bean.answer();
    assertThat(ai.requests()).hasSize(1);
    assertThat(bean.getError()).isNull();
    assertThat(bean.getChatHistory())
        .extracting(SmartWorkflowMessage::getRole, SmartWorkflowMessage::getContent)
        .containsExactly(tuple(MessageRole.USER, "Hi"), tuple(MessageRole.AI, "Hello!"));

    ask("Tell me more");
    bean.answer();
    var query = JSON.readTree(ai.lastUserMessage());
    assertThat(query.get("latestUserMessage").asString()).isEqualTo("Tell me more");
    assertThat(query.get("chatHistory")).hasSize(2);
    assertThat(bean.getChatHistory()).hasSize(4);
  }

  @Test
  void answer_emptyAnswer_keepsQuestionForRetry() {
    ChatbotChat.answering("");
    ask("Are you there?");

    bean.answer();

    assertThat(bean.getError().isRetryAllowed()).isTrue();
    assertThat(bean.getChatHistory())
        .extracting(SmartWorkflowMessage::getRole, SmartWorkflowMessage::getContent)
        .containsExactly(tuple(MessageRole.USER, "Are you there?"));

    var ai = ChatbotChat.answering("Yes, I am.");
    bean.retry();
    assertThat(bean.getError()).isNull();
    bean.answer();

    assertThat(bean.getChatHistory())
        .extracting(SmartWorkflowMessage::getRole, SmartWorkflowMessage::getContent)
        .containsExactly(
            tuple(MessageRole.USER, "Are you there?"),
            tuple(MessageRole.AI, "Yes, I am."));
    assertThat(JSON.readTree(ai.lastUserMessage()).get("chatHistory")).isEmpty();
  }

  @Test
  void answer_aiFailure_setsRetryableError() {
    MockOpenAI.defineChat(_ -> Response.status(Response.Status.BAD_REQUEST).entity("{}").build());
    ask("Hi");

    bean.answer();

    assertThat(bean.getError().isRetryAllowed()).isTrue();
    assertThat(bean.getChatHistory()).hasSize(1);
  }

  @Test
  void answer_blocked_setsError(AppFixture fixture) {
    fixture.var("AI.CircuitBreaker.Enabled", "true");
    var ai = ChatbotChat.answering("unused");
    ask("Hi");

    bean.answer();

    assertThat(ai.requests()).isEmpty();
    assertThat(bean.getError()).isNotNull();
    assertThat(bean.getChatHistory()).hasSize(1);
  }

  @Test
  void newConversation_resetsState() {
    var ai = ChatbotChat.answering("unused");
    ask("Hi");
    bean.setUserMessage("draft");

    bean.newConversation();
    bean.answer();

    assertThat(bean.getChatHistory()).isEmpty();
    assertThat(bean.getUserMessage()).isEmpty();
    assertThat(bean.getError()).isNull();
    assertThat(ai.requests()).isEmpty();
  }

  private void ask(String question) {
    bean.setUserMessage(question);
    bean.sendMessage();
  }

  private void verifyAccepted(boolean expected) {
    assertThat(accepted).isEqualTo(expected);
  }
}
