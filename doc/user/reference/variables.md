# Variables

Every setting Smart Workflow reads, in one place. All of them live under `AI` and are set in the **Engine Cockpit**, under **Variables** — that is the supported way to configure Smart Workflow. The variables themselves are declared by the projects you install; you set their values, not the variables.

> **Note:** Variables are read on **each agent call**, not cached at startup. Changing a provider, key, model or switch takes effect on the next call, with no restart.

Variables marked 🔒 are secrets: declare them as `${decrypt:}` in source control and set the real value in the Engine Cockpit after deployment, where the Engine encrypts it on entry. Never commit a real key.

## Core

| Variable | Default | Purpose |
| --- | --- | --- |
| `AI.DefaultProvider` | _(empty)_ | Provider used by agents that do not name one. Empty falls back to `OpenAI`. One of `OpenAI`, `AzureOpenAI`, `Gemini`, `xAI`, `Anthropic`, `Ollama`. |
| `AI.CircuitBreaker.Enabled` | `"false"` | `"true"` stops **every** agent call in the application. See [Circuit Breaker](../operate/circuit-breaker.md). |

## Providers

Each provider ships its own block with the corresponding `models/smart-workflow-*` project. Every provider accepts `BaseUrl` (empty means the vendor default) and names its model variable `DefaultModel`.

| Variable | 🔒 | Purpose |
| --- | :---: | --- |
| `AI.Providers.OpenAI.APIKey` | 🔒 | OpenAI API key. |
| `AI.Providers.OpenAI.BaseUrl` | | Override the API endpoint. |
| `AI.Providers.OpenAI.DefaultModel` | | `gpt-4o`, `gpt-4.1`, `gpt-4.1-mini`, `gpt-4.1-nano`, `gpt-5`. |
| `AI.Providers.OpenAI.DefaultEmbeddingModel` | | `text-embedding-3-small`, `text-embedding-3-large`, `text-embedding-ada-002`. |
| `AI.Providers.AzureOpenAI.Endpoint` | | Your Azure OpenAI resource URL. |
| `AI.Providers.AzureOpenAI.DefaultDeployment` | | Deployment used when an agent names no model. |
| `AI.Providers.AzureOpenAI.Deployments.<name>.Model` | | The model behind that deployment. |
| `AI.Providers.AzureOpenAI.Deployments.<name>.APIKey` | 🔒 | Per-deployment key. Azure has no provider-level key. |
| `AI.Providers.Gemini.APIKey` | 🔒 | Google Gemini API key. |
| `AI.Providers.Gemini.BaseUrl` | | Override the API endpoint. |
| `AI.Providers.Gemini.DefaultModel` | | `gemini-2.5-pro`, `gemini-2.5-flash`, `gemini-2.0-flash`, `gemini-2.0-flash-exp`, `gemini-1.5-pro`, `gemini-1.5-flash`. |
| `AI.Providers.Anthropic.APIKey` | 🔒 | Anthropic API key. |
| `AI.Providers.Anthropic.BaseUrl` | | Override the API endpoint. |
| `AI.Providers.Anthropic.DefaultModel` | | `claude-opus-4-6`, `claude-sonnet-4-6`, `claude-opus-4-5`, `claude-sonnet-4-5`, `claude-haiku-4-5`, `claude-opus-4-1`, `claude-opus-4-0`, `claude-sonnet-4-0`. |
| `AI.Providers.xAI.APIKey` | 🔒 | xAI API key. |
| `AI.Providers.xAI.BaseUrl` | | Override the API endpoint. |
| `AI.Providers.xAI.DefaultModel` | | `grok-4-1-fast`, `grok-4-1-mini`, `grok-4-1-large`, `grok-4-1-max`, and `-code` variants. A name outside the enum works but logs `Unknown xAI model … Compatibility not guaranteed`. |
| `AI.Providers.Ollama.BaseUrl` | | Your Ollama server, e.g. `http://localhost:11434`. No API key — the instance is yours. |
| `AI.Providers.Ollama.DefaultModel` | | Any model pulled in your instance. |
| `AI.Providers.Ollama.DefaultEmbeddingModel` | | e.g. `nomic-embed-text`, `mxbai-embed-large`. |
| `AI.Providers.Ollama.TimeoutSeconds` | `"300"` | Local Ollama on CPU can need minutes for large models or cold starts. |

Full YAML blocks for each provider are in [Model Providers](../build/providers.md#global-configuration).

## Guardrails

| Variable | Default | Purpose |
| --- | --- | --- |
| `AI.Guardrails.DefaultInput` | _(empty)_ | Comma-separated input guardrails applied to every agent that configures none. |
| `AI.Guardrails.DefaultOutput` | _(empty)_ | Comma-separated output guardrails applied to every agent that configures none. |
| `AI.Guardrails.PromptInjection.Classifier.Provider` | _(empty)_ | Provider for the `AiPromptInjectionInputGuardrail` classifier. Empty falls back to `AI.DefaultProvider`. |
| `AI.Guardrails.PromptInjection.Classifier.Model` | _(empty)_ | Pin a cheap model for the classifier, e.g. `gpt-4.1-nano`. Empty uses the provider default. |
| `AI.Guardrails.PromptInjection.Classifier.SystemPrompt` | _(empty)_ | Custom classifier prompt. Must instruct the model to reply only `YES` or `NO`. |
| `AI.Guardrails.PromptInjection.Classifier.MinLength` | `"0"` | Skip the LLM call for messages shorter than this many characters. `0` evaluates everything. |

> **Important:** An agent with **empty** guardrail fields is not unguarded — it inherits these defaults. Contrast with tools, where empty means none. See [Guardrails](../operate/guardrails.md#using-guardrails-in-agents).

## Web search

| Variable | Shipped value | Purpose |
| --- | --- | --- |
| `AI.Tool.WebSearch.Engine` | `duckduckgo` | Must match the `name()` of a registered `SmartWebSearchEngine`. Empty uses the first available engine. |
| `AI.Tool.WebSearch.MaxResults` | _(empty)_ | Results per query. Empty falls back to `5`. |
| `AI.Tool.WebSearch.WhitelistDomains` | _(empty)_ | Comma-separated allowed domains. Empty allows all. |

## RAG

Shipped by `smart-workflow` (retrieval defaults) and `smart-workflow-opensearch-rag` (the connection).

| Variable | Default | Purpose |
| --- | --- | --- |
| `AI.RAG.MaxResults` | `"5"` | Segments returned per query. The agent can override it per call. |
| `AI.RAG.MinScore` | `"0.6"` | Cosine similarity threshold, 0.0–1.0. Overridable per call. |
| `AI.RAG.ChunkSize` | `"300"` | Chunk size at ingestion. Applies to new documents only. |
| `AI.RAG.ChunkOverlap` | `"20"` | Overlap between consecutive chunks. Ingestion only. |
| `AI.RAG.EmbeddingModel.Provider` | _(empty)_ | Must support embedding — OpenAI or Ollama. Empty falls back to `AI.DefaultProvider`. |
| `AI.RAG.EmbeddingModel.Name` | _(empty)_ | Empty uses the provider's `DefaultEmbeddingModel`. |
| `AI.RAG.EmbeddingModel.ApiKey` | 🔒 | Optional separate key for embedding calls, billed separately from chat. |
| `AI.RAG.OpenSearch.Url` | _(empty)_ | Base URL of the OpenSearch server. |
| `AI.RAG.OpenSearch.ApiKey` | 🔒 | Leave blank when using basic auth or when security is disabled. |
| `AI.RAG.OpenSearch.UserName` | _(empty)_ | Basic authentication user. |
| `AI.RAG.OpenSearch.Password` | 🔒 | Basic authentication password. |
| `AI.RAG.OpenSearch.TrustSelfSignedCertificates` | `"false"` | Dev and test only. Never `true` in production with sensitive data. |

> **Note:** `ChunkSize` and `ChunkOverlap` are counted in **characters**, not tokens, despite what the comments in `variables.yaml` say. Documents are split with a recursive character splitter.

## Observability

| Variable | Default | Purpose |
| --- | --- | --- |
| `AI.Observability.Openinference.Enabled` | off | Export OpenInference traces to Arize Phoenix. |
| `AI.Observability.Openinference.HideInputMessages` | off | Omit prompts from exported traces, keeping timing, cost and model metadata. |
| `AI.Observability.Openinference.HideOutputMessages` | off | Omit responses from exported traces. |
| `AI.Observability.Ivy.Enabled` | off | Record every conversation into the Ivy repository for governance audit. |
| `AI.Observability.CustomFields.Enabled` | `"true"` | Mark Cases and Tasks with the `aiAssisted` custom field. **On by default.** |

## See also

- [Provider Capabilities](capabilities.md) — what each provider supports
- [Error Codes](error-codes.md) — the BPM errors Smart Workflow raises
- [Security and Data](../operate/security-and-data.md) — how keys and prompts are handled
