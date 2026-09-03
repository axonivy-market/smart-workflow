# Model Providers

Smart Workflow ships a connector for each supported model provider — you bring the API key, or your own Ollama instance. Choose a provider globally, and override it on any individual agent, without code changes.

This page is about *using* a provider. To *contribute* a new one, see the [Chat Models](../contribute/models.md) contribution guideline.

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

Model lists, the per-provider caveats, and the reasons behind each `—` are in [Provider Capabilities](../reference/capabilities.md). Read it before relying on file extraction or structured output: nothing is checked locally, so an unsupported combination fails at the provider with the provider's own error message.

> **Note:** The asterisk on Ollama is worth knowing before you design around it — structured output and tools cannot be used in the same request. [Troubleshooting](../troubleshooting.md#the-agent-answered-but-not-as-expected) explains what happens and how to work around it.

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

## Per-agent override

Every `AgenticProcessCall` element has a **Model** group with two fields:

| Field | Widget | Notes |
| --- | --- | --- |
| `Provider` | picker | One of the installed providers. Empty means fall back to `AI.DefaultProvider`. |
| `Model` | script | An IvyScript expression returning a `String`, so the value must be **quoted**: `"gpt-4o"`. Empty means fall back to that provider's `DefaultModel`. |

`Model` being a script field is easy to get wrong — a bare `gpt-4o` does not compile. It also means the model can be computed at runtime, e.g. `in.selectedModel`.

By default, an agent uses the provider set in `AI.DefaultProvider`. Selecting a `Provider` on the element overrides this for that agent only. If the selected provider cannot be found — most often because its connector project is not installed — the agent call fails with `Unknown model provider <name>`.

### Mixing providers in one process

Since the choice is per element, a single process can route each step to whatever fits it. A three-step invoice flow might use:

| Step | Provider / Model | Why |
| --- | --- | --- |
| Extract invoice fields | `OpenAI` / `"gpt-4o"` | Structured extraction of several typed fields. Accuracy matters most here — an error corrupts everything downstream, and this is the step that meets unusual date formats and missing values. |
| Classify urgency | `Anthropic` / `"claude-haiku-4-5"` | One number in, one of three words out. Any modern model does this correctly, so pay for the cheapest and fastest. |
| Check supplier risk | `Ollama` / `"llama3.2"` | Supplier names are sensitive business intelligence that must not leave the network. A self-hosted model keeps the data on-premise. |

The point is that you are not locked into one vendor per application: spend capability where accuracy is non-negotiable, drop to a cheap model where the task is trivial, and keep sensitive data on hardware you own — all by changing two fields.

## Common mistakes

- **An unquoted model name.** `Model` is a script field; a bare `gpt-4o` does not compile.
- **Choosing a provider before checking its capabilities.** Gemini cannot do structured output, xAI and Ollama cannot read PDFs, and only OpenAI and Ollama can embed. Nothing warns you until the provider rejects the request.
- **Committing a real API key.** Ship `${decrypt:}` and set the value in the Engine Cockpit.

## See also

- [Agent Setup](agent-setup.md) — configuring the agent element
- [Provider Capabilities](../reference/capabilities.md) — the full support matrix
- [Variables](../reference/variables.md) — every provider setting in one table
- [File Extraction](file-extraction.md) — passing images and PDFs to a model
- [RAG](rag.md) — where embedding support matters
- [Chat Models](../contribute/models.md) — contributing a new provider
