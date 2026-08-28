# Model Providers

Smart Workflow ships with several model providers. You choose one globally and can override it on any individual agent, without code changes.

This page is about *using* a provider. To *contribute* a new one, see the [Chat Models](../contribute/models.md) contribution guideline.

## Choosing a provider

The built-in providers span the three ways AI models are typically consumed, and the trade-offs differ more than the models do.

**Self-hosted** — you run an open-source model on your own hardware. Complete data privacy, no per-query cost, works offline, fully tunable. In exchange you buy or rent GPUs and operate the servers yourself. `Ollama` covers this case.

**Managed platform** — you reach models through a cloud platform that adds enterprise security, monitoring, evaluation, and governance on top. One integration serves several models and billing is centralized, at the price of extra moving parts, platform fees, and closer coupling to one cloud vendor. `Azure OpenAI` covers this case.

**Direct cloud API** — you call a vendor's proprietary models and pay per token. The fastest route to the most capable models with no hardware to manage, but your data leaves your network and cost scales with use. `OpenAI`, `Gemini`, `Anthropic`, and `xAI` cover this case.

## Capabilities

Providers are not interchangeable. Before committing to one, check what it supports:

| Provider | Category | PNG / JPEG | PDF | Structured output | Embedding |
| --- | --- | :---: | :---: | :---: | :---: |
| **OpenAI** | Direct | ✓ | ✓ | ✓ | ✓ |
| **Azure OpenAI** | Platform | ✓ | ✓ | ✓ | — |
| **Gemini** | Direct | ✓ | ✓ | — | — |
| **Anthropic** | Direct | ✓ | ✓ | ✓ | — |
| **xAI** | Direct | ✓ | — | ✓ | — |
| **Ollama** | Self-hosted | ✓ | — | ✓ *(not with tools)* | ✓ |

Model lists, the per-provider caveats, and the reasons behind each `—` are in [Provider Capabilities](../reference/capabilities.md). Read it before relying on file extraction or structured output: nothing is checked locally, so an unsupported combination fails at the provider with the provider's own error message.

## Global configuration

Set the default provider once for the whole application, in the **Engine Cockpit** under **Variables**. Every agent uses it unless it overrides it.

```yaml
Variables:
  AI:
    # [enum: OpenAI, AzureOpenAI, Gemini, xAI, Anthropic, Ollama]
    DefaultProvider: "OpenAI"
```

Each provider contributes its own variables under `AI.Providers`, shipped with the corresponding `models/smart-workflow-*` project — you set their values in the Engine Cockpit, you do not add the variables yourself. Every provider accepts `BaseUrl` (leave empty for the vendor default) and names its model variable **`DefaultModel`** — the model used when an agent does not specify one.

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

A model name outside the enum still works, but logs `Unknown xAI model ... Compatibility not guaranteed`.

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

Variables marked `#[password]` are secrets: encrypted at rest, decrypted only at runtime. Never type a real key into `variables.yaml`.

The `${decrypt:}` placeholder is what ships, and it is what you keep in source control. Set the real value after deployment in the Engine Cockpit, under your application's variables — for example `AI.Providers.OpenAI.APIKey` — where the Engine encrypts it on entry.

> **Note:** Variables are read on each agent call, not cached at startup. Changing a provider or key in the Engine Cockpit takes effect on the next call, with no restart.

## Per-agent override

Every `AgenticProcessCall` element has a **Model** group with two fields:

| Field | Widget | Notes |
| --- | --- | --- |
| `Provider` | picker | One of the installed providers. Empty means fall back to `AI.DefaultProvider`. |
| `Model` | script | An IvyScript expression returning a `String`, so the value must be **quoted**: `"gpt-4o"`. Empty means fall back to that provider's `DefaultModel`. |

`Model` being a script field is easy to get wrong — a bare `gpt-4o` does not compile. It also means the model can be computed at runtime, e.g. `in.selectedModel`.

Provider resolution runs in this order:

1. the element's `Provider` field
2. `AI.DefaultProvider`
3. `OpenAI`, as a hard-coded last resort

Because of step 3, an application with a blank `DefaultProvider` still works as long as `smart-workflow-openai` is installed. An unresolvable name fails with `Unknown model provider <name>`.

Selecting more than one provider in the picker is not supported; the first is used and a warning is logged.

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
- **Assuming a blank `DefaultProvider` breaks the application.** It falls back to `OpenAI`, so a misconfigured application can silently use a provider you did not intend.
- **Selecting several providers in the picker.** Only the first is used, with a warning in the log.
- **Choosing a provider before checking its capabilities.** Gemini cannot do structured output, xAI and Ollama cannot read PDFs, and only OpenAI and Ollama can embed. Nothing warns you until the provider rejects the request.
- **Committing a real API key.** Ship `${decrypt:}` and set the value in the Engine Cockpit.

## See also

- [Agent Setup](agent-setup.md) — configuring the agent element
- [Provider Capabilities](../reference/capabilities.md) — the full support matrix
- [Variables](../reference/variables.md) — every provider setting in one table
- [File Extraction](file-extraction.md) — passing images and PDFs to a model
- [RAG](rag.md) — where embedding support matters
- [Chat Models](../contribute/models.md) — contributing a new provider
