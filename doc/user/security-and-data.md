# Security and Data

What leaves your network when an agent runs, what is stored where, and which controls you have over both. If you are answering a security review or a data-protection question, start here.

## What leaves your network

For each agent call, Smart Workflow sends to the configured provider:

| Sent | Notes |
| --- | --- |
| The system message | After macro expansion, so any process data interpolated into it goes too. |
| The user message | After expression evaluation, including any file content. |
| Tool definitions | Each selected tool's description and its **input** parameter names, types and descriptions. Result parameter descriptions are not sent. |
| Tool results | Whatever your tool returned, serialized as JSON, is sent back to the model on the next turn. |
| File content | Images and PDFs base64-encoded in full. PDFs are **not** parsed locally — the raw document goes to the provider. |

The exceptions are self-hosted and platform providers: with **Ollama** the model runs on your own hardware and nothing leaves your network; with **Azure OpenAI** the data stays inside your Azure tenancy under your own resource. Everything else is a direct cloud API. See [Provider Capabilities](reference/capabilities.md).

Because the provider is chosen per element, you can route a single process across all three — sensitive steps to Ollama, the rest to a cloud model. See [Mixing providers in one process](providers.md#mixing-providers-in-one-process).

## API keys

Every provider key is declared `#[password]` in `variables.yaml`, which means it is encrypted at rest and decrypted only at runtime.

- Set the value in the Engine Cockpit, under your application's variables — for example `AI.Providers.OpenAI.APIKey`. The Engine encrypts it on entry, so keys are never stored in plain text.
- Never commit a real key. The declaration shipped in source control is `${decrypt:}` and should stay that way.
- Keys are read on each call, so rotating one takes effect immediately with no restart.
- `SensitiveDataOutputGuardrail` blocks a response that leaks a credential — both your own configured keys and anything matching a known key format. It is not enabled by default; add it to `AI.Guardrails.DefaultOutput`.

The full list of secret variables is marked 🔒 in [Variables](reference/variables.md).

## Keeping personal data out of the model

`PiiMaskingGuardrail` replaces personal data with opaque tokens before the request leaves, and restores the originals in the response. The model only ever processes anonymized text.

This is the control to reach for under GDPR, CCPA and similar regimes. It has real limits — regex-based detection, seven types, no IPv6, shared mutable state across concurrent calls — all documented in [PII masking](guardrails.md#pii-masking). Read them before relying on it: masking is a risk reduction, not a privacy guarantee.

Data minimization is the stronger control. An agent that is never sent a customer's full record cannot leak it.

## What is stored, and for how long

Three stores can retain prompt and response content. All three are off or narrow by default, and each is a separate decision:

| Store | Switch | Contains | Where |
| --- | --- | --- | --- |
| Ivy conversation history | `AI.Observability.Ivy.Enabled` (off) | Full prompts, responses, tool arguments, guardrail evaluations | The Ivy repository, against the Case and Task |
| Arize Phoenix traces | `AI.Observability.Openinference.Enabled` (off) | Full prompts and responses, plus timing, cost and model metadata | Your Phoenix instance, over OTLP |
| Suspended conversations | none — automatic | The agent's message list while a [human-in-the-loop](human-in-the-loop.md) task is pending | Ivy Business Data, keyed by `aiMemoryId` |

The Ivy conversation history is a **durable audit record** — that is its purpose. Treat retention as a policy decision, and prefer masking the data on the way in over redacting the record afterwards.

Phoenix traces can be stripped of message content while keeping the operational metadata:

```yaml
Variables:
  AI:
    Observability:
      Openinference:
        Enabled: "true"
        HideInputMessages: "true"
        HideOutputMessages: "true"
```

Suspended conversations clear themselves: the stored messages and the `aiMemoryId` value are both erased when the agent completes normally. An agent suspended forever keeps its messages forever.

One further trace of AI usage is on by default: `AI.Observability.CustomFields.Enabled` marks each Case and Task with an `aiAssisted` custom field. It records *that* AI was used, never what was sent.

## Stopping everything

`AI.CircuitBreaker.Enabled: "true"` refuses every agent call in the application — a leaked prompt, a provider incident, a compliance hold. It takes effect on the next call, application-wide, regardless of how individual agents are configured.

It does **not** cancel a call already in flight, and it is all-or-nothing for the whole application. See [Circuit Breaker](circuit-breaker.md).

## Cost

Cost is a security concern when it is unbounded. Smart Workflow enforces no spend limit of its own, so the controls are:

- **Model choice per agent.** A cheap model on a trivial step is the single largest lever. See [Mixing providers in one process](providers.md#mixing-providers-in-one-process).
- **Tool list length.** Every tool granted to an agent costs tokens in every request. Keep the list tight.
- **`AiPromptInjectionInputGuardrail` costs one extra LLM call per message.** Pin a cheap classifier model and raise `MinLength`.
- **Images are always sent at `DetailLevel.HIGH`.** Relevant when processing documents in bulk.
- **The in-call message list is uncapped.** A long tool loop grows the context on every turn, and there is no variable to limit it.
- **Arize Phoenix reports token cost per call**, which is the practical way to find out what you are actually spending. See [Observability](observability.md#querying).

## Known limits

Worth stating plainly in a review:

- Provider capability is **not** checked before a request is sent. An unsupported file type fails at the provider, after the data has left.
- There is no cross-call conversation memory, and no cap on the in-call message list.
- Guardrail instances are shared across agents and are not concurrency-safe against each other.
- The circuit breaker cannot cancel an in-flight call, and cannot target a single agent or user.
- PII masking is regex-based and covers seven types.

## See also

- [Guardrails](guardrails.md) — input and output validation
- [Observability](observability.md) — what is recorded and where
- [Circuit Breaker](circuit-breaker.md) — the stop switch
- [Variables](reference/variables.md) — every setting, with secrets marked
- [Model Providers](providers.md) — self-hosted, platform and direct-API options
