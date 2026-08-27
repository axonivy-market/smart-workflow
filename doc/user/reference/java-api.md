# Java API

The public Java surface of Smart Workflow. Most processes need none of this — tools and guardrails are usually built as callable sub-processes. Reach for Java when the logic has no workflow steps, or when you are extending the product itself.

Everything below lives under `com.axonivy.utils.smart.workflow`. All extension points are discovered through the Java **SPI** mechanism: a file under `src/META-INF/services/` named after the interface, containing the implementation's fully-qualified class name.

> **Note:** Only the **first line** of a services file is read. To register two providers, use two files — a second class name in the same file is silently ignored.

## Tools

**`tools.provider.SmartWorkflowTool`** — one callable capability.

```java
public interface SmartWorkflowTool {
  record ToolParameter(String name, String description, String type) {}

  String description();
  List<ToolParameter> parameters();
  Object execute(Map<String, Object> args);

  default String name() { return getClass().getSimpleName(); }
}
```

`description()` and each parameter's name, type and description are what the model sees. `type` is a string naming the Java type: a primitive (`"int"`), a class (`"java.lang.String"`, `"com.example.MyClass"`), or a generic list (`"java.util.List<java.lang.String>"`). Arrays are not supported. Arguments are deserialized into the declared type automatically, and the return value is serialized back to the agent as JSON.

**`tools.provider.SmartWorkflowToolsProvider`** — groups tools for discovery.

```java
public interface SmartWorkflowToolsProvider {
  List<SmartWorkflowTool> getTools();

  default String name() { return getClass().getSimpleName(); }
}
```

Register in `src/META-INF/services/com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider`. Providers are resolved on each agent call, so a newly registered tool appears without a restart.

See [Defining Tools](../build/tools.md#java-tools) for a worked example.

## Guardrails

**`guardrails.entity.SmartWorkflowGuardrail`** — the shared contract. `SmartWorkflowInputGuardrail` and `SmartWorkflowOutputGuardrail` are marker sub-interfaces that only decide which list a guardrail belongs in.

```java
public interface SmartWorkflowGuardrail {
  GuardrailResult evaluate(String message);

  default GuardrailResult evaluate(String message, String invocationId) {
    return evaluate(message);
  }

  default String name() { return getClass().getSimpleName(); }
}
```

Implement the single-argument form for a stateless check. Override the two-argument form only to correlate the input and output halves of one agent call — that is how `PiiMaskingGuardrail` pairs masking with restoration.

**`guardrails.entity.GuardrailResult`** — four outcomes.

| Factory | Effect |
| --- | --- |
| `allow()` | Pass the message through unchanged. |
| `allowWithRewrite(String)` | Pass through, replacing the message. Use for redaction or normalization rather than rejection. |
| `block(String reason)` | Reject; the reason surfaces in the BPM error. |
| `block(String reason, Throwable cause)` | Reject with a typed cause. The cause travels through the LangChain4j guardrail exception, letting a caller tell *which* guardrail blocked without parsing the message. |

**`guardrails.provider.GuardrailProvider`** — registers custom guardrails, via `src/META-INF/services/com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider`.

```java
public interface GuardrailProvider {
  List<SmartWorkflowInputGuardrail> getInputGuardrails();
  List<SmartWorkflowOutputGuardrail> getOutputGuardrails();
}
```

See [Custom Guardrails](../contribute/guardrails-spi.md).

## Circuit breaker

**`guardrails.circuitbreaker.CircuitBreakerSignal`** — the application-wide stop switch, callable from process code.

```java
public interface CircuitBreakerSignal {
  Optional<String> stopReason();

  static CircuitBreakerSignal defaultSignal();
  static void stopAll();
  static void resumeAll();
}
```

`stopAll()` and `resumeAll()` write the `AI.CircuitBreaker.Enabled` variable, so they take effect for the whole application on the next agent call. See [Circuit Breaker](../operate/circuit-breaker.md).

## Human in the loop

**`tools.human.DecisionMaker`** — writes a human's answer into a suspended conversation.

```java
public class DecisionMaker {
  public DecisionMaker(String memoryId);
  public void resolve(String decision);
}
```

Construct it with the value of the process data field `aiMemoryId`, then call `resolve` with the answer before the flow re-enters the agent element. It appends the answer as the result of the pending tool call, so the resumed agent finds the tool it was waiting on has returned.

`resolve` answers exactly **one** pending tool request and throws `IllegalStateException` when it cannot find what it needs:

- `Found no pending ChatMemory for id: …` — no suspended conversation under that id, usually because `aiMemoryId` was never stored
- `Found no pending AiMessage for id: …` — the stored conversation has no message with tool calls
- `Found no pending ToolExecutionRequest for id: …` — every tool call in that conversation is already answered

See [Human in the Loop](../build/human-in-the-loop.md).

## Model providers

**`model.spi.ChatModelProvider`** — supplies a chat model, and optionally an embedding model.

```java
public interface ChatModelProvider {
  String name();
  ChatModel setup(ModelOptions options);
  List<String> models();
  List<String> secretsVars();

  default boolean supportsEmbedding() { return false; }
  default Optional<EmbeddingModel> setupEmbedding(EmbeddingModelOptions options);
  default String resolveEmbeddingModelName(EmbeddingModelOptions options);
}
```

`ModelOptions` is a record carrying `modelName`, `structuredOutput`, `hasTools` and `listeners`, built fluently from `ModelOptions.options()`. `hasTools` is what lets a provider decide whether a schema can be applied — Ollama uses it to drop structured output when tools are present.

Register in `src/META-INF/services/com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider`. See [Chat Models](../contribute/models.md).

## Web search

**`tools.web.SmartWebSearchEngine`** and **`tools.web.SmartWebSearchEngineProvider`** — plug in a search backend for the built-in `webSearch` tool. DuckDuckGo is the shipped default and the only built-in engine. Engine names are matched case-insensitively against `AI.Tool.WebSearch.Engine`.

See [Standard Tools](../build/tools.md#standard-tools).

## Callable subprocess

Smart Workflow also exposes an agent as a callable subprocess, used by Axon Ivy Portal features:

```text
Portal/SmartWorkflowAgent:invokeAgent(String,String,List<String>,Class)
```

The arguments are the system message, the user message, the list of tool names, and the expected result type. Refer to the Axon Ivy Portal documentation for how Portal uses it.

## See also

- [Defining Tools](../build/tools.md) — tools as processes or Java
- [Custom Guardrails](../contribute/guardrails-spi.md) — implementing and registering a guardrail
- [Error Codes](error-codes.md) — the BPM errors these APIs raise
