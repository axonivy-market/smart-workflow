# Provider Capabilities

What each provider can and cannot do. This page is the single source of truth for these facts — other pages link here rather than restating them.

> **Important:** This table is curated knowledge, not an enforced contract. Smart Workflow builds image and PDF content unconditionally — no provider declares a vision or PDF capability and nothing is checked before the request is sent. An unsupported combination therefore fails **at the provider**, with the provider's own error message, not locally. When contributing a provider, keep this table honest; it is the only place users can find out.

## At a glance

| Provider | Category | PNG / JPEG | PDF | Structured output | Embedding |
| --- | --- | :---: | :---: | :---: | :---: |
| **OpenAI** | Direct cloud API | ✓ | ✓ | ✓ | ✓ |
| **Azure OpenAI** | Managed platform | ✓ | ✓ | ✓ | — |
| **Gemini** | Direct cloud API | ✓ | ✓ | — | — |
| **Anthropic** | Direct cloud API | ✓ | ✓ | ✓ | — |
| **xAI** | Direct cloud API | ✓ | — | ✓ | — |
| **Ollama** | Self-hosted | ✓ | — | ✓ *(not with tools)* | ✓ |

## Models

Taken from each provider's `DefaultModel` enum in `variables.yaml`. A name outside the enum is not rejected, but compatibility is not guaranteed.

| Provider | Models |
| --- | --- |
| **OpenAI** | `gpt-4o`, `gpt-4.1`, `gpt-4.1-mini`, `gpt-4.1-nano`, `gpt-5` |
| **Azure OpenAI** | Any deployment in your resource; capability follows the deployment's underlying model, not its name |
| **Gemini** | `gemini-2.5-pro`, `gemini-2.5-flash`, `gemini-2.0-flash`, `gemini-2.0-flash-exp`, `gemini-1.5-pro`, `gemini-1.5-flash` |
| **Anthropic** | `claude-opus-4-6`, `claude-sonnet-4-6`, `claude-opus-4-5`, `claude-sonnet-4-5`, `claude-haiku-4-5`, `claude-opus-4-1`, `claude-opus-4-0`, `claude-sonnet-4-0` |
| **xAI** | `grok-4-1-fast`, `grok-4-1-mini`, `grok-4-1-large`, `grok-4-1-max`, and the `-code` variants |
| **Ollama** | Any model pulled in your instance (`llama3.2`, `gemma3:27b`, `qwen3:30b`, `llava`, …) |

Embedding models: OpenAI offers `text-embedding-3-small`, `text-embedding-3-large` and `text-embedding-ada-002`; on Ollama, pull a dedicated embedding model such as `nomic-embed-text` or `mxbai-embed-large`.

## File extraction

Supported input formats are PNG, JPG, JPEG and PDF. See [File Extraction](../build/file-extraction.md) for how to pass a file to an agent.

**Azure OpenAI** — capability depends on the underlying model of your deployment, not the deployment name. Make sure the deployment uses a vision-capable model.

**xAI** — PDFs are not supported by the xAI API. Convert them to images before passing them to Smart Workflow.

**Anthropic** — images and PDFs can be sent by URL or base64, both supporting text extraction and visual understanding of charts, diagrams and layouts.

**Ollama** — image support needs a vision-capable model pulled first (`ollama pull llava`); text-only models reject image input. PDFs are not supported — convert them to images.

### Payload size

File content is base64-encoded before being sent. Smart Workflow enforces **no size cap of its own**, so the provider's own request limit is the only bound — and it is the provider that rejects an oversized request. Keep files within the limits published by your provider; base64 adds roughly one third to the raw byte count.

Images are always sent at `DetailLevel.HIGH`, which costs more tokens than a low-detail request. This matters when processing documents in bulk.

## Structured output

How each provider constrains the response to a schema. See [Agent Setup](../build/agent-setup.md#structured-output) for how to request it.

| Provider | Supported | Mechanism |
| --- | :---: | --- |
| **OpenAI** | ✓ | `responseFormat("json_schema")` |
| **Azure OpenAI** | ✓ | `RESPONSE_FORMAT_JSON_SCHEMA` + `strictJsonSchema` |
| **Anthropic** | ✓ | `RESPONSE_FORMAT_JSON_SCHEMA` |
| **xAI** | ✓ | `RESPONSE_FORMAT_JSON_SCHEMA` + `strictJsonSchema` |
| **Ollama** | ✓ *(not with tools)* | Schema applied only when the agent has no tools |
| **Gemini** | — | Unsupported; logs an error and continues |

Two of these differ in ways that change how you design a process:

**Gemini** — no structured output at all. Requesting it does not fail: Smart Workflow logs an error, builds the model without the schema capability, and falls back to asking for the shape in the prompt and parsing the reply. Treat it as best-effort, and prefer plain text output on Gemini.

**Ollama** — the schema is dropped as soon as an agent has tools, because the two cannot go in the same request. It is dropped **silently**, so an agent configured with both gets its tools and unconstrained text. Split the work across two agents if you need both.

**Anthropic** — the capability is applied for every model in the list above, including the 4.0 generation. Smart Workflow imposes no version gate; newer models are simply more reliable at producing schema-valid output.

## Embedding

Embedding support matters only for [RAG](../build/rag.md), where `AI.RAG.EmbeddingModel.Provider` accepts only a provider whose Embedding column is ticked — currently **OpenAI** and **Ollama**.

## See also

- [Model Providers](../build/providers.md) — choosing and configuring a provider
- [Variables](variables.md) — every `AI.*` setting
- [Chat Models](../contribute/models.md) — contributing a new provider, and keeping this page current
