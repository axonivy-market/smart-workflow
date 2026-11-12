package com.axonivy.utils.smart.workflow.model.aura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.ai.mock.MockAura;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector.AuraConf;
import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.test.TestToolUserData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.resource.ResourceResponder;
import jakarta.ws.rs.core.Response;

/** Synthetic Aura fixtures drive real Ivy agent elements, not live Aura models. */
@RestResourceTest
class TestAuraProviderFlows {

  private static final String ORGANIZATION = "550e8400-e29b-41d4-a716-446655440000";
  private static final String TOKEN = "synthetic-provider-test-token";
  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @BeforeEach
  void setup(AppFixture fixture) {
    MockAura.reset();
    fixture.var(AiConf.DEFAULT_PROVIDER, AuraModelProvider.NAME);
    fixture.var(AuraConf.BASE_URL, Ivy.rest().client("mockClient").path("aura").getUri().toString());
    fixture.var(AuraConf.ORGANIZATION_ID, ORGANIZATION);
    fixture.var(AuraConf.BEARER_TOKEN, TOKEN);
    // Required only by the local Ivy mock server, not by Aura.
    fixture.config("RestClients.aura.Features", List.of("ch.ivyteam.ivy.rest.client.security.CsrfHeaderFeature"));
  }

  @AfterEach
  void cleanup() {
    MockAura.reset();
  }

  @Test
  void globalSelectionMapsTextIntoProcessData(BpmClient client, ResourceResponder responder) {
    var requests = new AtomicInteger();
    MockAura.define(request -> {
      requests.incrementAndGet();
      assertThat(request.authorization()).isEqualTo("Bearer " + TOKEN);
      assertThat(request.organization()).isEqualTo(ORGANIZATION);
      assertThat(request.body().has("model")).isFalse();
      assertThat(request.body().has("tools")).isFalse();
      return responder.send("response.json");
    });

    var result = client.start().process(AGENT_TOOLS.elementName("expandQuery")).execute();
    TestToolUserData data = result.data().last();
    assertThat(data.getResult()).isEqualTo("Hello from Aura");
    assertThat(requests).hasValue(1);
  }

  @Test
  void globalSelectionMapsTypedOutput(BpmClient client, ResourceResponder responder) {
    MockAura.define(request -> {
      assertThat(request.body().at("/response_format/type").asString()).isEqualTo("json_schema");
      assertThat(request.body().at("/response_format/json_schema/schema/properties/title/type").asString()).isEqualTo("string");
      return responder.send("story-response.json");
    });

    var result = client.start().process(AGENT_TOOLS.elementName("structuredOutputList")).execute();
    TestToolUserData data = result.data().last();
    assertThat(data.getStory().getTitle()).isEqualTo("A short story");
    assertThat(data.getStory().getCharacters()).extracting(person -> person.getFirstName()).containsExactly("Alex");
  }

  @Test
  void agentOverrideUsesAuraForTextAndTypedOutput(BpmClient client, AppFixture fixture, ResourceResponder responder) {
    fixture.var(AiConf.DEFAULT_PROVIDER, DummyChatModelProvider.NAME);
    var requests = new AtomicInteger();
    MockAura.define(request -> {
      requests.incrementAndGet();
      assertThat(request.authorization()).isEqualTo("Bearer " + TOKEN);
      return responder.send(request.body().has("response_format") ? "person-response.json" : "response.json");
    });

    var result = client.start().process(BpmProcess.name("AuraProviderTest").elementName("start")).execute();
    TestToolUserData data = result.data().last();
    assertThat(data.getResult()).isEqualTo("Hello from Aura");
    assertThat(data.getPerson().getFirstName()).isEqualTo("Alex");
    assertThat(data.getPerson().getLastName()).isEqualTo("Example");
    assertThat(requests).hasValue(2);
  }

  @Test
  void credentialAndOrganizationChangesDoNotMutateExistingModels(AppFixture fixture, ResourceResponder responder) {
    List<String> identities = new ArrayList<>();
    MockAura.define(request -> {
      identities.add(request.organization() + ":" + request.authorization());
      return responder.send("response.json");
    });
    var provider = ChatModelFactory.create(AuraModelProvider.NAME).orElseThrow();
    var first = provider.setup(ModelOptions.builder().build());
    first.chat("Hello");

    String otherOrganization = "550e8400-e29b-41d4-a716-446655440001";
    fixture.var(AuraConf.ORGANIZATION_ID, otherOrganization);
    fixture.var(AuraConf.BEARER_TOKEN, "rotated-synthetic-token");
    provider.setup(ModelOptions.builder().build()).chat("Hello");
    first.chat("Hello again");

    assertThat(identities).containsExactly(
        ORGANIZATION + ":Bearer " + TOKEN,
        otherOrganization + ":Bearer rotated-synthetic-token",
        ORGANIZATION + ":Bearer " + TOKEN);
  }

  @Test
  void realToolSelectionStillFailsBeforeHttp(BpmClient client) {
    var requests = new AtomicInteger();
    MockAura.define(_ -> {
      requests.incrementAndGet();
      return Response.serverError().build();
    });
    assertThatThrownBy(() -> client.start().process(AGENT_TOOLS.elementName("reducedTools")).execute())
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("Aura does not support native tools; leave Available tools empty");
    assertThat(requests).hasValue(0);
  }

  @Test
  void missingTokenFailsBeforeHttp(AppFixture fixture) {
    var requests = new AtomicInteger();
    MockAura.define(_ -> {
      requests.incrementAndGet();
      return Response.serverError().build();
    });
    fixture.var(AuraConf.BEARER_TOKEN, "");
    assertThatThrownBy(() -> ChatModelFactory.create(AuraModelProvider.NAME).orElseThrow()
        .setup(ModelOptions.builder().build()))
        .hasMessageContaining(AuraConf.BEARER_TOKEN);
    assertThat(requests).hasValue(0);
  }

  @Test
  void refusesCredentialsWhenRestClientDestinationDiffers(AppFixture fixture) throws Exception {
    var base = URI.create(Ivy.var().get(AuraConf.BASE_URL));
    String otherHost = "localhost".equals(base.getHost()) ? "127.0.0.1" : "localhost";
    var otherOrigin = new URI(base.getScheme(), null, otherHost, base.getPort(), base.getPath(), null, null);
    fixture.config("RestClients.aura.Url", otherOrigin.toString());
    var requests = new AtomicInteger();
    MockAura.define(_ -> {
      requests.incrementAndGet();
      return Response.ok("{}").build();
    });

    var model = ChatModelFactory.create(AuraModelProvider.NAME).orElseThrow().setup(ModelOptions.builder().build());
    assertThatThrownBy(() -> model.chat("Hello"))
        .isInstanceOf(IllegalStateException.class).hasMessageNotContaining(TOKEN).hasNoCause();
    assertThat(requests).hasValue(0);
  }

  @ParameterizedTest
  @ValueSource(strings = {"other-service", "aur", "aura/child", "aura/../other-service", "aura/%2e%2e/other-service"})
  void refusesCredentialsWhenRestClientPathDiffers(String approvedPath, AppFixture fixture, ResourceResponder responder) {
    var actualRoot = URI.create(Ivy.var().get(AuraConf.BASE_URL));
    fixture.var(AuraConf.BASE_URL, actualRoot.resolve(approvedPath).toString());
    fixture.config("RestClients.aura.Url", actualRoot.toString());
    var requests = new AtomicInteger();
    MockAura.define(_ -> {
      requests.incrementAndGet();
      return responder.send("response.json");
    });

    assertThatThrownBy(() -> ChatModelFactory.create(AuraModelProvider.NAME).orElseThrow()
        .setup(ModelOptions.builder().build()).chat("Hello"))
        .isInstanceOf(IllegalStateException.class).hasMessageNotContaining(TOKEN).hasNoCause();
    assertThat(requests).hasValue(0);
  }

  @Test
  void matchingRootAllowsTrailingSlashAndAuthenticatedPolling(AppFixture fixture, ResourceResponder responder) {
    String actualRoot = Ivy.var().get(AuraConf.BASE_URL);
    fixture.var(AuraConf.BASE_URL, actualRoot + "/");
    fixture.config("RestClients.aura.Url", actualRoot);
    String jobId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    var requests = new AtomicInteger();
    MockAura.define(request -> {
      requests.incrementAndGet();
      assertThat(request.authorization()).isEqualTo("Bearer " + TOKEN);
      assertThat(request.organization()).isEqualTo(ORGANIZATION);
      if (request.method().equals("POST")) {
        return Response.status(202).header("Retry-After", "0")
            .entity("{\"job_id\":\"" + jobId + "\",\"status\":\"queued\"}").build();
      }
      assertThat(request.jobId()).isEqualTo(jobId);
      return Response.ok("{\"job_id\":\"" + jobId + "\",\"status\":\"completed\",\"result\":"
          + responder.load("response.json") + "}").build();
    });

    var model = ChatModelFactory.create(AuraModelProvider.NAME).orElseThrow().setup(ModelOptions.builder().build());
    assertThat(model.chat("Hello")).isEqualTo("Hello from Aura");
    assertThat(requests).hasValue(2);
  }

  @Test
  void auraClientIsSensitiveAndSeparateFromLangChain() {
    var aura = Ivy.rest().client(AuraServiceConnector.REST_CLIENT);
    assertThat(aura.getConfiguration().getProperty("SensitiveData").toString()).isEqualTo("true");
    assertThat(Ivy.rest().client("langChain").getUri()).isNotEqualTo(aura.getUri());
  }
}
