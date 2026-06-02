# 🧪 Testing

Features of smart-workflow are highly covered by tests
in order to keep them stable and easy to evolve.
All test sources live under `smart-workflow-test`.

## 📂 Test Categories

### ⚡ Unit Tests

Small, fast-executing tests.
None of them should call third-party services on the wire or cause long execution times.

- Plain JUnit 5 — pure Java logic, no Ivy runtime needed
- `@IvyTest` — code reads Ivy variables or uses SPI loading
- `@IvyProcessTest` — code requires a full Ivy process context
- `@RestResourceTest` — lightweight REST-resource tests; preferred when mocking external LLM interactions to keep tests isolated and fast. See the [Mocking](#-mocking) section.

### 🐳 Integration Tests

Tests with heavy infrastructure involved, started automatically via Docker / Testcontainers.
Use `@Testcontainers` together with `@IvyTest`, for example to spin up an OpenSearch node.

- These classes are suffixed with `ContainerTest` or `IT`.
- Not part of the regular CI build — run nightly via the [`IT-Build`](../../.github/workflows/it.yml) workflow.


### 🌐 End-to-End Tests

Tests that call real LLM models over the network.
API keys are required and are passed via system properties (e.g. `-DOPEN_AI_API_KEY=…`).

- Classes are named `*E2E` (e.g. `OpenAiModelE2E`).
- Not part of the regular CI build — run weekly (Sunday) via the [`E2E-Build`](../../.github/workflows/e2e.yml) workflow.

Example — calling a real OpenAI model and asserting structured output:

```java
@IvyProcessTest
class OpenAiModelE2E {
  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, OpenAiModelProvider.NAME);
    fixture.var(OpenAiConf.API_KEY, TestUtils.getSystemProperty("OPEN_AI_API_KEY"));
  }

  @Test
  void structuredOutput_e2e(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var res = client.start().process(AGENT_TOOLS.elementName("structuredOutput"))
        .as().session(Ivy.session()).execute();
    assertThat(res.data().last().getPerson().getFirstName()).isEqualTo("James");
  }
}
```

---

## 🎭 Mocking

CI unit tests must run fast and independently, but the code naturally interacts with slow remote LLMs.
Instead of calling real services, static responses captured from real model interactions are replayed.
Follow these three steps to write such a test:

**1. Create a Demo or Test process that invokes an Agent / LLM.**

The Process should call an Agent exactly in the manner you like to test. Define 'messages', 'tools', 'output', ... as needed.

**2. Record the real REST response.**

Enable the Runtime Log at filter level `Trace`, invoke the process against a live model, then copy the logged response body into a JSON file next to your test class (e.g. `response1.json`, `response2.json`):

```json
{
  "id": "tool-demo-1",
  "model": "gpt-4o-mini",
  "choices": [{
    "message": {
      "role": "assistant",
      "content": null,
      "tool_calls": [{ "id": "call_tax001", "type": "function",
        "function": { "name": "calculateTax", "arguments": "{\"invoiceNumber\":\"INV-2024-042\", ...}" }
      }]
    },
    "finish_reason": "tool_calls"
  }],
  "usage": { "prompt_tokens": 500, "completion_tokens": 100, "total_tokens": 600 }
}
```

**3. Write a `@RestResourceTest` that drives the process and mocks LLM responses.**

The test starts the real Ivy process via `BpmClient` and asserts on its output data.
`MockOpenAI.defineChat(handler)` intercepts every outgoing LLM HTTP call before it hits the network
and returns the pre-recorded file instead — the process under test is unaware of the swap.
A multi-turn conversation returns different files depending on how far the conversation has progressed:

```java
@RestResourceTest
class TestToolDemo {

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool-demo"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(request -> respond(request, responder));
  }

  private Response respond(JsonNode request, ResourceResponder responder) {
    var messages = (ArrayNode) request.get("messages");
    return messages.size() <= 2
        ? responder.send("response1.json")  // first turn: tool-call request
        : responder.send("response2.json"); // second turn: final answer
  }

  @Test
  void calculateInvoiceTax(BpmClient client) {
    var result = client.start().process(TOOL_DEMO.elementName("start")).execute();
    assertThat(result.data().last().getTaxedInvoice()).isNotNull();
  }
}
```

### 📋 Asserting log output

Use `LoggerAccess` as a JUnit 5 extension to capture and assert log messages emitted during a test:

```java
@RegisterExtension
LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

@Test
void httpCallIsLogged(BpmClient client) {
  // ... trigger action
  assertThat(log.infos()).anyMatch(line -> line.contains("POST /v1/chat/completions"));
}
```
