# Extending Smart Workflow

Smart Workflow discovers model providers, guardrails and Java tools through the Java SPI. Each of them is a class you implement and register, and each becomes available to every agent in the application once it is on the classpath.

This page is for people adding those extensions.

| Extension | Interface | Registered in `src/META-INF/services/` |
| --- | --- | --- |
| [Model provider](#adding-a-model-provider) | `ChatModelProvider` | `…model.spi.ChatModelProvider` |
| [Guardrail](#writing-a-custom-guardrail) | `SmartWorkflowInputGuardrail` / `SmartWorkflowOutputGuardrail` | `…guardrails.provider.GuardrailProvider` |
| [Java tool](#writing-a-java-tool) | `SmartWorkflowTool` | `…tools.provider.SmartWorkflowToolsProvider` |
| [Web search engine](#writing-a-web-search-engine) | `SmartWebSearchEngine` | `…tools.web.SmartWebSearchEngineProvider` |

> **Note:** Only the first line of each services file is read, so one Axon Ivy project registers one provider of each kind — one model provider, one guardrail provider, one tools provider. That is rarely a limit, since a single guardrail or tools provider can expose as many guardrails or tools as you like. A second provider of the same kind needs its own project.

## Adding a model provider

Each supported provider is an Ivy project under `models/` that supplies a `ChatModelProvider`. We are open to supporting more chat models from any vendor — if you miss your preferred one, contribute it to this space.

### Project setup

Create a directory `models/smart-workflow-PROVIDER`, replacing PROVIDER with your concrete vendor. Align the project coordinates with the existing workspace:

```xml
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-PROVIDER</artifactId>
  <packaging>iar</packaging>
```

Include the project in the build by adding your provider to the module list in the root `pom.xml`.

### Implementation

Implement `ChatModelProvider`, from the `smart-workflow` project, within your own:

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

Register your implementation in `src/META-INF/services/com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider`. The file must contain a single line stating your implementation type name.

Two parts of `ModelOptions` decide how your provider behaves in edge cases, and both belong in [Provider Capabilities](../user/reference/capabilities.md):

- `structuredOutput()` — whether to apply a JSON schema. If your vendor cannot, log an error and build the model without it rather than failing the call.
- `hasTools()` — whether the agent also has tools. Ollama uses this to drop the schema, since the two are mutually exclusive there.

### Variables

Every provider has its own set of variables. Contribute yours under `Variables.AI.Providers.PROVIDER`:

```yaml
Variables:
  AI:
    Providers:
      PROVIDER:
        #[password]
        APIKey: ${decrypt:}
        ...
```

Also add your provider to the global enumeration under `AI.DefaultProvider`, in the `variables.yaml` of the `smart-workflow` project.

### Libraries

Your provider depends on its vendor's client library, and that library normally pulls in artifacts `smart-workflow` already ships. Inspect what it brings with `mvn dependency:tree`, then exclude every duplicate from your `pom.xml` to keep the compiled project lean.

The `pom.xml` of the `smart-workflow-openai` project is a good example. Its client dependency excludes the whole `com.fasterxml.jackson.core` group, plus `langchain4j-core`, `langchain4j-http-client` and `langchain4j-http-client-jdk` — all of which already come from `smart-workflow` itself.

Re-run the tree after adding exclusions: a vendor library that upgrades often changes what it drags in.

### Testing

Tests for your model provider go in the common `smart-workflow-test` project, with provider-specific functionality under `src_test/com/axonivy/utils/smart/workflow/model/PROVIDER`. It is fine to add a dependency from the common test project to your new model provider.

### Demo

We expect all providers to work the same way, so no extra demonstration process is needed. **Do not** add dependencies to additional model providers to the `smart-workflow-demo` project.

### Checklist

- [ ] `ChatModelProvider` implemented and registered via SPI
- [ ] custom `variables.yaml` in your provider project
- [ ] provider listed in `AI.DefaultProvider` of the `smart-workflow` project's `variables.yaml`
- [ ] project added to the module list in the root `pom.xml`
- [ ] models, file-extraction support and structured-output behaviour documented in [Provider Capabilities](../user/reference/capabilities.md) — including any condition under which the schema is dropped
- [ ] provider configuration block added to [Model Providers](../user/providers.md#global-configuration), and the provider named in its supported-providers list
- [ ] variables listed in [Variables](../user/reference/variables.md#providers)
- [ ] provider added to the supported list in the `smart-workflow-product` README
- [ ] tests in `smart-workflow-test`

> **Important:** [Provider Capabilities](../user/reference/capabilities.md) is curated knowledge, not an enforced contract — nothing checks a capability before a request is sent. It is the only place a user can find out what your provider supports, so keep it honest.

## Writing a custom guardrail

Beyond the built-ins, you can implement your own — a domain rule, a compliance check, a redaction pass. Once registered, it can be used exactly like a built-in: selected in the pickers of an agent, or named in the application-wide variables.

The easiest way in is `BlockCompetitorMentionGuardrail` in the `smart-workflow-demo` project, together with the `DemoGuardrailProvider` that exposes it and the SPI file that registers the provider.

Implement `SmartWorkflowInputGuardrail` or `SmartWorkflowOutputGuardrail` depending on which picker the guardrail should appear in, or both if it belongs in both. Its `evaluate` method returns one of four outcomes:

| Factory | Effect |
| --- | --- |
| `allow()` | Pass the message through unchanged. |
| `allowWithRewrite(String)` | Pass through, replacing the message with your version. Use for redaction or normalization rather than rejection. |
| `block(String reason)` | Reject, with the reason surfaced in the BPM error. |
| `block(String reason, Throwable cause)` | Reject, attaching a cause so callers can tell *which* guardrail blocked without inspecting the reason text. |

The single-argument `evaluate` is enough for a stateless check. There is also a two-argument form taking an invocation id, for the rare case where a guardrail needs to correlate the input and output halves of the same agent call — that is how `PiiMaskingGuardrail` pairs its masking with its restoration.

> **Important:** SPI registration is required. Expose your guardrails through a `GuardrailProvider` and name that class in `src/META-INF/services/com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider`. Without it, Smart Workflow never discovers them and they never appear in the pickers.

Once registered, the guardrail's `name()` — the simple class name unless you override it — appears in the pickers on any agent element. To apply it everywhere, add the name to `AI.Guardrails.DefaultInput` or `AI.Guardrails.DefaultOutput` in the Engine Cockpit.

Two traps are worth knowing before you write one:

- Forgetting the SPI registration. The class compiles, the guardrail never runs, and nothing warns you.
- Holding state in a guardrail instance. The instance is shared across all agents and concurrent calls. If you need per-call state, key it on the invocation id from the two-argument `evaluate`.

See [Guardrails](../user/guardrails.md) for the built-ins and how they are configured.

## Writing a Java tool

Tool logic can be implemented in Java, but it is rarely the right choice — prefer callable subprocesses, which give the tool full access to the process designer. Reach for Java only when the logic has no workflow steps and is better expressed as a plain class.

A Java tool implements `SmartWorkflowTool` — a description, its input parameters, and an `execute` method — and is exposed through a `SmartWorkflowToolsProvider` registered via SPI, both under `com.axonivy.utils.smart.workflow.tools.provider`.

The easiest way in is `TaxCalculatorTool` in the `smart-workflow-demo` project. It takes a structured `Invoice` object, returns per-item tax calculations, and is registered in `DemoToolProvider` — a complete example you can adapt to your own case.

A few things are handy to know as you do. Conversion runs in both directions for you: arguments are deserialized into the declared type, and whatever you return is serialized back to the agent as JSON, custom types included. The `name()` method is optional and defaults to the simple class name, so you only need it when the agent-facing name should differ.

Providers are resolved on each agent call, so a newly registered tool appears without a restart.

See [Defining Tools](../user/tools.md) for how tools are selected on an agent and how their descriptions reach the model.

## Writing a web search engine

DuckDuckGo is the shipped default and the only built-in engine behind the `webSearch` tool. Custom engines plug in by implementing `SmartWebSearchEngine` and registering a `SmartWebSearchEngineProvider` via SPI, both from the `smart-workflow` project.

Select yours with `AI.Tool.WebSearch.Engine`, which must match the `name()` of a registered engine. Names are matched case-insensitively, and when the variable is empty the first available engine is used.
