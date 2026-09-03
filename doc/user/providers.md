# Model Providers

Smart Workflow ships a connector for each supported model provider — you bring the API key, or your own Ollama instance. Choose a provider globally, and override it on any individual agent, without code changes.

To add support for a new provider, see [Contributing a provider](#contributing-a-provider) below.

## Supported providers

Smart Workflow connects to a wide range of LLM platforms, so you can use whichever fits your project — and switch later without changing a line of code.

- **Hosted cloud APIs** — `OpenAI`, `Gemini`, `Anthropic` and `xAI`.
- **Enterprise platform** — `Azure OpenAI`.
- **Self-hosted** — `Ollama`, running on your own hardware.

Each agent can use a different provider and model, giving you the flexibility to pick the right one for each task — see [Mixing providers in one process](#mixing-providers-in-one-process).

## Capabilities

They differ in what they support, so it is worth a look before you pick one:

| Provider | Category | PNG / JPEG | PDF | Structured output | Embedding |
| --- | --- | :---: | :---: | :---: | :---: |
| **OpenAI** | Direct | ✓ | ✓ | ✓ | ✓ |
| **Azure OpenAI** | Platform | ✓ | ✓ | ✓ | — |
| **Gemini** | Direct | ✓ | ✓ | — | — |
| **Anthropic** | Direct | ✓ | ✓ | ✓ | — |
| **xAI** | Direct | ✓ | — | ✓ | — |
| **Ollama** | Self-hosted | ✓ | — | ✓ *(not with tools)* | ✓ |

Model lists, the per-provider caveats, and the reasons behind each `—` are in [Provider Capabilities](reference/capabilities.md). Read it before relying on file extraction or structured output: nothing is checked locally, so an unsupported combination fails at the provider with the provider's own error message.

> **Note:** The asterisk on Ollama is worth knowing before you design around it — structured output and tools cannot be used in the same request. [Troubleshooting](troubleshooting.md#the-agent-answered-but-not-as-expected) explains what happens and how to work around it.

## Global configuration

`AI.DefaultProvider` sets the provider for the whole application. Set it in the **Engine Cockpit**, under **Variables**.

```yaml
Variables:
  AI:
    # [enum: OpenAI, AzureOpenAI, Gemini, xAI, Anthropic, Ollama]
    DefaultProvider: "OpenAI"
```

Each provider contributes its own variables under `AI.Providers`, shipped with the corresponding `models/smart-workflow-*` project.

### OpenAI

```yaml
Variables:
  AI:
    Providers:
      OpenAI:
        #[password]
        APIKey: ${decrypt:}
        BaseUrl: ""
        # [enum: gpt-4o, gpt-4.1, gpt-4.1-mini, gpt-4.1-nano, gpt-5]
        DefaultModel: "gpt-4.1-mini"
        # [enum: text-embedding-3-small, text-embedding-3-large, text-embedding-ada-002]
        DefaultEmbeddingModel: ""
```

### Azure OpenAI

The only provider with a nested shape. There is no provider-level `APIKey`; each deployment carries its own key and model.

```yaml
Variables:
  AI:
    Providers:
      AzureOpenAI:
        Endpoint: https://my-openai-resource.openai.azure.com/
        DefaultDeployment: your-deployment-name
        Deployments:
          your-deployment-name:
            Model: gpt-4.1-mini
            #[password]
            APIKey: ${decrypt:}
```

Deployment names become YAML keys, so use kebab-case — lowercase letters, digits and hyphens, not starting with a digit.

### Gemini

```yaml
Variables:
  AI:
    Providers:
      Gemini:
        #[password]
        APIKey: ${decrypt:}
        BaseUrl: ""
        # [enum: gemini-2.5-pro, gemini-2.5-flash, gemini-2.0-flash-exp, gemini-2.0-flash, gemini-1.5-flash, gemini-1.5-pro]
        DefaultModel: "gemini-2.5-flash"
```

### Anthropic

```yaml
Variables:
  AI:
    Providers:
      Anthropic:
        #[password]
        APIKey: ${decrypt:}
        BaseUrl: ""
        # [enum: claude-opus-4-6, claude-sonnet-4-6, claude-opus-4-5, claude-sonnet-4-5, claude-haiku-4-5, claude-opus-4-1, claude-opus-4-0, claude-sonnet-4-0]
        DefaultModel: "claude-haiku-4-5"
```

### xAI

```yaml
Variables:
  AI:
    Providers:
      xAI:
        #[password]
        APIKey: ${decrypt:}
        BaseUrl: ""
        # [enum: grok-4-1-fast, grok-4-1-mini, grok-4-1-large, grok-4-1-max, grok-4-1-mini-code, grok-4-1-large-code, grok-4-1-max-code]
        DefaultModel: "grok-4-1-fast"
```

### Ollama

No API key, since the instance is yours.

```yaml
Variables:
  AI:
    Providers:
      Ollama:
        BaseUrl: http://localhost:11434
        DefaultModel: "llama3.2"
        # E.g. nomic-embed-text, mxbai-embed-large
        DefaultEmbeddingModel: ""
        # Local Ollama on CPU can need minutes for large models or cold starts.
        TimeoutSeconds: "300"
```

### Handling API keys

Always enter API keys in the **Engine Cockpit**, under **Variables** — for example `AI.Providers.OpenAI.APIKey`. The Cockpit encrypts the value as you save it, so the key is never stored in plain text.

Do not open `variables.yaml` in a text editor to set a key. A key typed there stays readable and ends up in source control. The file ships with the `${decrypt:}` placeholder, and that is what should stay in it.

> **Note:** Keys are read on each agent call, so a key you add or rotate in the Cockpit takes effect on the next call, with no restart.

## Setting a provider and model for one agent

Every `AgenticProcessCall` element has a **Model** group with two fields. By default an agent uses the application's provider and that provider's default model, so both can stay empty — set them when one agent needs something different.

**`Provider`** selects which model provider this agent calls. Leave it empty to use `AI.DefaultProvider`; set it when one agent needs something the others do not, such as a self-hosted model for a step handling sensitive data. Selecting a provider here overrides the default for that agent only. If the provider cannot be found — most often because its connector project is not installed — the agent call fails with `Unknown model provider <name>`.

**`Model`** selects which of that provider's models to use. Leave it empty for the provider's `DefaultModel`. This is a script field, so the value must be quoted: `"gpt-4o"` — a bare `gpt-4o` will not compile. Being a script field also means the model can be computed at runtime, for example `in.selectedModel`.

## Mixing providers in one process

Since the choice is per element, a single process can route each step to whatever fits it. A three-step invoice flow might use:

| Step | Provider / Model | Why |
| --- | --- | --- |
| Extract invoice fields | `OpenAI` / `"gpt-4o"` | Structured extraction of several typed fields. Accuracy matters most here — an error corrupts everything downstream, and this is the step that meets unusual date formats and missing values. |
| Classify urgency | `Anthropic` / `"claude-haiku-4-5"` | One number in, one of three words out. Any modern model does this correctly, so pay for the cheapest and fastest. |
| Check supplier risk | `Ollama` / `"llama3.2"` | Supplier names are sensitive business intelligence that must not leave the network. A self-hosted model keeps the data on-premise. |

The point is that you are not locked into one vendor per application: spend capability where accuracy is non-negotiable, drop to a cheap model where the task is trivial, and keep sensitive data on hardware you own — all by changing two fields.

## Contributing a provider

Each supported provider is an Ivy project under `models/` that supplies a `ChatModelProvider`. We are open to supporting more chat models from any vendor — if you miss your preferred one, contribute it to this space.

### Project setup

Create a directory `models/smart-workflow-PROVIDER`, replacing PROVIDER with your concrete vendor. Align the project coordinates with the existing workspace:

```xml
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-PROVIDER</artifactId>
  <packaging>iar</packaging>
```

Include the project in the build by adding your provider to the [main module build](https://github.com/axonivy-market/smart-workflow/blob/master/pom.xml).

### Implementation

Implement [`ChatModelProvider`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow/src/com/axonivy/utils/smart/workflow/model/spi/ChatModelProvider.java) within your project:

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

Two parts of `ModelOptions` decide how your provider behaves in edge cases, and both belong in [Provider Capabilities](reference/capabilities.md):

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

Also add your provider to the global enumeration in [`smart-workflow/config/variables.yaml`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow/config/variables.yaml), under `AI.DefaultProvider`.

### Libraries

Smart Workflow providers are built on the existing LangChain4j providers. Exclude any dependency from your `pom.xml` that is already part of smart-workflow — classically `langchain4j-core` and `langchain4j-http-client`.

### Testing

Tests for your model provider go in the common `smart-workflow-test` project, with provider-specific functionality under `src_test/com/axonivy/utils/smart/workflow/model/PROVIDER`. It is fine to add a dependency from the common test project to your new model provider.

### Demo

We expect all providers to work the same way, so no extra demonstration process is needed. **Do not** add dependencies to additional model providers to the `smart-workflow-demo` project.

### Checklist

- [ ] `ChatModelProvider` implemented and registered via SPI
- [ ] custom `variables.yaml` in your provider project
- [ ] provider listed in `AI.DefaultProvider` of [`smart-workflow/config/variables.yaml`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow/config/variables.yaml)
- [ ] project added to the [main module build](https://github.com/axonivy-market/smart-workflow/blob/master/pom.xml)
- [ ] models, file-extraction support and structured-output behaviour documented in [Provider Capabilities](reference/capabilities.md) — including any condition under which the schema is dropped
- [ ] provider configuration block added to [Model Providers](providers.md#global-configuration)
- [ ] variables listed in [Variables](reference/variables.md#providers)
- [ ] setup section added to the product [README.md](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-product/README.md), with the product [build](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-product/pom.xml) extended to interpolate your `@variables.PROVIDER@` token
- [ ] tests in `smart-workflow-test`

> **Important:** [Provider Capabilities](reference/capabilities.md) is curated knowledge, not an enforced contract — nothing checks a capability before a request is sent. It is the only place a user can find out what your provider supports, so keep it honest.

## Common mistakes

- **An unquoted model name.** `Model` is a script field; a bare `gpt-4o` does not compile.
- **Choosing a provider before checking its capabilities.** Gemini cannot do structured output, xAI and Ollama cannot read PDFs, and only OpenAI and Ollama can embed. Nothing warns you until the provider rejects the request.
- **Committing a real API key.** Ship `${decrypt:}` and set the value in the Engine Cockpit.

## See also

- [Agent Setup](agent-setup.md) — configuring the agent element
- [Provider Capabilities](reference/capabilities.md) — the full support matrix
- [Variables](reference/variables.md) — every provider setting in one table
- [File Extraction](file-extraction.md) — passing images and PDFs to a model
- [RAG](rag.md) — where embedding support matters
