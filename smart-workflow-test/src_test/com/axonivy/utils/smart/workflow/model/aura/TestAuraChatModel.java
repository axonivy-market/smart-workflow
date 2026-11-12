package com.axonivy.utils.smart.workflow.model.aura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.ai.mock.MockAura;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraChatModel;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraInferenceClient;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraHttpClientFeature;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.resource.ResourceResponder;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.AiServices;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

/** Real local HTTP calls, synthetic Aura responses, no external service or credentials. */
@RestResourceTest
class TestAuraChatModel {

  private static final UUID ORGANIZATION = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  private static final String JOB = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
  private final AtomicInteger posts = new AtomicInteger();
  private final AtomicInteger polls = new AtomicInteger();

  @BeforeEach
  void setup() {
    MockAura.reset();
  }

  @AfterEach
  void cleanup() {
    MockAura.reset();
  }

  @Test
  void textOverIvyRest(ResourceResponder responder) {
    MockAura.define(request -> {
      posts.incrementAndGet();
      assertThat(request.method()).isEqualTo("POST");
      assertThat(request.organization()).isEqualTo(ORGANIZATION.toString());
      assertThat(request.body().at("/messages/0/content").asString()).isEqualTo("Hello");
      assertThat(request.body().path("temperature").asDouble()).isEqualTo(0.4);
      assertThat(request.body().path("max_completion_tokens").asInt()).isEqualTo(512);
      assertThat(request.body().has("model")).isFalse();
      return responder.send("response.json");
    });
    var options = ModelOptions.builder().temperature(0.4).maxOutputTokens(512).build();

    assertThat(model(options).chat("Hello")).isEqualTo("Hello from Aura");
    assertThat(posts).hasValue(1);
  }

  @Test
  void perCallOptionsOverrideDefaults(ResourceResponder responder) {
    MockAura.define(request -> {
      assertThat(request.body().path("temperature").asDouble()).isEqualTo(0.8);
      assertThat(request.body().path("max_completion_tokens").asInt()).isEqualTo(128);
      return responder.send("response.json");
    });
    var model = model(ModelOptions.builder().temperature(0.4).maxOutputTokens(512).build());
    var response = model.chat(ChatRequest.builder().messages(UserMessage.from("Hello"))
        .temperature(0.8).maxOutputTokens(128).build());
    assertThat(response.aiMessage().text()).isEqualTo("Hello from Aura");
  }

  @Test
  void typedOutputOverIvyRest(ResourceResponder responder) {
    MockAura.define(request -> {
      assertThat(request.body().at("/response_format/type").asString()).isEqualTo("json_schema");
      assertThat(request.body().at("/response_format/json_schema/schema/properties/name/type").asString()).isEqualTo("string");
      return responder.send("structured-response.json");
    });
    var agent = AiServices.create(PersonAgent.class, model(ModelOptions.builder().structuredOutput(true).build()));

    assertThat(agent.extract("The person's name is Alex").name()).isEqualTo("Alex");
  }

  interface PersonAgent {
    Person extract(String message);
  }

  record Person(String name) {
  }

  @Test
  void queuedThenInProgressThenCompleted(ResourceResponder responder) {
    MockAura.define(request -> {
      if (request.method().equals("POST")) {
        posts.incrementAndGet();
        return job(202, "queued").header("Retry-After", "0").build();
      }
      assertThat(request.jobId()).isEqualTo(JOB);
      assertThat(request.organization()).isEqualTo(ORGANIZATION.toString());
      if (polls.incrementAndGet() == 1) {
        return job(200, "in_progress").header("Retry-After", "0").build();
      }
      return completed(responder);
    });

    assertThat(model().chat("Hello")).isEqualTo("Hello from Aura");
    assertThat(posts).hasValue(1);
    assertThat(polls).hasValue(2);
  }

  @Test
  void buildsPollingPathInsteadOfFollowingLocation(ResourceResponder responder) {
    MockAura.define(request -> {
      if (request.method().equals("POST")) {
        return job(202, "queued")
            .header("Location", "https://example.invalid/do-not-send-credentials")
            .header("Retry-After", "0").build();
      }
      assertThat(request.method()).isEqualTo("GET");
      assertThat(request.jobId()).isEqualTo(JOB);
      polls.incrementAndGet();
      return completed(responder);
    });

    assertThat(model().chat("Hello")).isEqualTo("Hello from Aura");
    assertThat(polls).hasValue(1);
  }

  @Test
  void keepsConfiguredAuthenticationOnBothRequests(ResourceResponder responder) {
    MockAura.define(request -> {
      assertThat(request.authorization()).isEqualTo("Bearer synthetic-test-token");
      if (request.method().equals("POST")) {
        posts.incrementAndGet();
        return job(202, "queued").header("Retry-After", "0").build();
      }
      polls.incrementAndGet();
      return completed(responder);
    });
    var target = target().register((ClientRequestFilter) request ->
        request.getHeaders().putSingle("Authorization", "Bearer synthetic-test-token"));
    var model = new AuraChatModel(new AuraInferenceClient(target, ORGANIZATION, Duration.ofSeconds(5)),
        ModelOptions.builder().build());

    assertThat(model.chat("Hello")).isEqualTo("Hello from Aura");
    assertThat(posts).hasValue(1);
    assertThat(polls).hasValue(1);
  }

  @Test
  void doesNotChangeSharedClientConfiguration(ResourceResponder responder) {
    MockAura.define(_ -> responder.send("response.json"));
    model().chat("Hello");

    var configuration = Ivy.rest().client("mockClient").getConfiguration();
    assertThat(configuration.isRegistered(AuraHttpClientFeature.class)).isFalse();
    assertThat(configuration.getProperty("jersey.config.apache5.client.retryStrategy")).isNull();
  }

  @Test
  void doesNotFollowHttpRedirects() {
    MockAura.define(request -> {
      assertThat(request.method()).isEqualTo("POST");
      posts.incrementAndGet();
      return Response.status(302).header("Location", target().path("redirect-target").getUri()).build();
    });

    assertThatThrownBy(() -> model().chat("Hello"))
        .isInstanceOf(HttpException.class).hasMessageContaining("302");
    assertThat(posts).hasValue(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 404, 422, 429, 503})
  void httpFailuresAreSanitizedAndNotRetried(int status) {
    MockAura.define(_ -> {
      posts.incrementAndGet();
      return Response.status(status).entity("{\"message\":\"credential-do-not-echo\"}").build();
    });

    assertThatThrownBy(() -> model().chat("Hello"))
        .isInstanceOf(HttpException.class)
        .hasMessageContaining("Aura HTTP " + status)
        .hasMessageNotContaining("credential-do-not-echo")
        .hasNoCause();
    assertThat(posts).hasValue(1);
  }

  @Test
  void distinguishesCreditBudgetFromRateLimit() {
    MockAura.define(_ -> Response.status(429).entity("{\"error\":\"TOKEN_LIMIT_EXCEEDED\"}").build());
    assertThatThrownBy(() -> model().chat("Hello")).hasMessageContaining("credit budget exhausted");

    MockAura.define(_ -> Response.status(429).entity("{\"error\":\"RATE_LIMIT_EXCEEDED\"}").build());
    assertThatThrownBy(() -> model().chat("Hello")).hasMessageContaining("request rate limit exceeded");
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-json-credential-do-not-echo", "null", "[]", ""})
  void malformedResponsesDoNotLeakBodies(String body) {
    MockAura.define(_ -> Response.ok(body).build());
    assertThatThrownBy(() -> model().chat("Hello"))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("invalid JSON")
        .hasMessageNotContaining("credential-do-not-echo").hasNoCause();
  }

  @Test
  void jobFailureDoesNotLeakMessage() {
    MockAura.define(request -> {
      if (request.method().equals("POST")) {
        return job(202, "queued").header("Retry-After", "0").build();
      }
      polls.incrementAndGet();
      return Response.ok("{\"job_id\":\"" + JOB + "\",\"status\":\"failed\","
          + "\"error\":{\"code\":\"queue_timeout\",\"message\":\"credential-do-not-echo\"}}").build();
    });

    assertThatThrownBy(() -> model().chat("Hello"))
        .hasMessageContaining("timed out waiting for capacity")
        .hasMessageNotContaining("credential-do-not-echo");
    assertThat(polls).hasValue(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"job_id\":\"../../other-org\",\"status\":\"queued\"}",
      "{\"job_id\":\"" + JOB + "\",\"status\":\"unknown\"}",
      "{\"job_id\":\"" + JOB + "\",\"status\":\"completed\"}"})
  void rejectsMalformedJobEnvelopes(String body) {
    MockAura.define(_ -> {
      posts.incrementAndGet();
      return Response.status(202).entity(body).build();
    });
    assertThatThrownBy(() -> model().chat("Hello")).isInstanceOf(IllegalStateException.class);
    assertThat(posts).hasValue(1);
  }

  @Test
  void rejectsDifferentJobIdInPollResponse() {
    MockAura.define(request -> {
      if (request.method().equals("POST")) {
        return job(202, "queued").header("Retry-After", "0").build();
      }
      return Response.ok("{\"job_id\":\"550e8400-e29b-41d4-a716-446655440001\",\"status\":\"queued\"}").build();
    });
    assertThatThrownBy(() -> model().chat("Hello")).hasMessageContaining("different inference job ID");
  }

  @Test
  void pollHttpFailureDoesNotResubmitInference() {
    MockAura.define(request -> {
      if (request.method().equals("POST")) {
        posts.incrementAndGet();
        return job(202, "queued").header("Retry-After", "0").build();
      }
      polls.incrementAndGet();
      return Response.status(503).entity("{}").build();
    });
    assertThatThrownBy(() -> model().chat("Hello")).isInstanceOf(HttpException.class).hasMessageContaining("503");
    assertThat(posts).hasValue(1);
    assertThat(polls).hasValue(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"60", "invalid", "-1", "999999999999999999999"})
  void pollingRespectsRetryAfterAndDeadline(String retryAfter) {
    MockAura.define(_ -> {
      posts.incrementAndGet();
      return job(202, "queued").header("Retry-After", retryAfter).build();
    });
    var model = model(Duration.ofMillis(150));
    assertThatThrownBy(() -> model.chat("Hello")).hasMessageContaining("timeout");
    assertThat(posts).hasValue(1);
  }

  @Test
  void interruptedCallDoesNotSendRequest() {
    MockAura.define(_ -> {
      posts.incrementAndGet();
      return Response.serverError().build();
    });
    var model = model();
    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(() -> model.chat("Hello")).hasMessageContaining("interrupted");
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      assertThat(posts).hasValue(0);
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void cancellationInterruptsPollingWait() {
    var caller = Thread.currentThread();
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    MockAura.define(_ -> {
      posts.incrementAndGet();
      scheduler.schedule(caller::interrupt, 100, TimeUnit.MILLISECONDS);
      return job(202, "queued").header("Retry-After", "60").build();
    });
    try {
      assertThatThrownBy(() -> model().chat("Hello")).hasMessageContaining("interrupted");
      assertThat(caller.isInterrupted()).isTrue();
      assertThat(posts).hasValue(1);
    } finally {
      scheduler.shutdownNow();
      Thread.interrupted();
    }
  }

  @Test
  void dispatchesListenersOnSuccessAndFailure(ResourceResponder responder) {
    var requests = new AtomicInteger();
    var responses = new AtomicInteger();
    var errors = new AtomicInteger();
    var listener = new ChatModelListener() {
      @Override
      public void onRequest(ChatModelRequestContext context) {
        requests.incrementAndGet();
      }

      @Override
      public void onResponse(ChatModelResponseContext context) {
        responses.incrementAndGet();
      }

      @Override
      public void onError(ChatModelErrorContext context) {
        errors.incrementAndGet();
      }
    };
    var model = model(ModelOptions.builder().listeners(List.of(listener)).build());
    MockAura.define(_ -> responder.send("response.json"));
    model.chat("Hello");
    MockAura.define(_ -> Response.status(401).entity("{}").build());
    assertThatThrownBy(() -> model.chat("Hello")).isInstanceOf(HttpException.class);

    assertThat(requests).hasValue(2);
    assertThat(responses).hasValue(1);
    assertThat(errors).hasValue(1);
  }

  @Test
  void rejectsUnsupportedOptionsBeforeHttp() {
    MockAura.define(_ -> {
      posts.incrementAndGet();
      return Response.serverError().build();
    });
    assertThatThrownBy(() -> model(ModelOptions.builder().modelName("not-selectable").build()))
        .hasMessageContaining("leave Model empty");
    assertThatThrownBy(() -> model(ModelOptions.builder().hasTools(true).build()))
        .hasMessageContaining("Available tools empty");
    var tool = ToolSpecification.builder().name("whoami").build();
    assertThatThrownBy(() -> model().chat(ChatRequest.builder()
        .messages(UserMessage.from("Hello"))
        .toolSpecifications(tool).build())).hasMessageContaining("Available tools empty");
    assertThat(posts).hasValue(0);
  }

  private static WebTarget target() {
    return Ivy.rest().client("mockClient").path("aura")
        .register((ClientRequestFilter) request -> request.getHeaders().putSingle("X-Requested-By", "ivy"));
  }

  private static AuraChatModel model() {
    return model(ModelOptions.builder().build());
  }

  private static AuraChatModel model(ModelOptions options) {
    return new AuraChatModel(new AuraInferenceClient(target(), ORGANIZATION, Duration.ofSeconds(5)), options);
  }

  private static AuraChatModel model(Duration timeout) {
    return new AuraChatModel(new AuraInferenceClient(target(), ORGANIZATION, timeout), ModelOptions.builder().build());
  }

  private static Response.ResponseBuilder job(int status, String state) {
    return Response.status(status).entity("{\"job_id\":\"" + JOB + "\",\"status\":\"" + state + "\"}");
  }

  private static Response completed(ResourceResponder responder) {
    var body = JsonUtils.getObjectMapper().createObjectNode().put("job_id", JOB).put("status", "completed");
    body.set("result", JsonUtils.getObjectMapper().readTree(responder.load("response.json")));
    return Response.ok(body.toString()).build();
  }
}
