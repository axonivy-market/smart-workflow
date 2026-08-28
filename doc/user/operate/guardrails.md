# Guardrails

Guardrails protect AI agents by validating both user input and AI output. Smart Workflow provides built-in guardrails for common safety concerns, and you can add your own.

**Built-in guardrails:**

| Guardrail | Type | Description |
| --- | --- | --- |
| `PromptInjectionInputGuardrail` | Input | Blocks common prompt injection attacks using regex patterns. Low latency, no LLM cost. Use as a basic first line of defence. |
| `AiPromptInjectionInputGuardrail` | Input | LLM-based classifier that catches subtle injections missed by regex — roleplay jailbreaks, authority spoofing, narrative payloads, gradual drift. Use when stricter protection is needed. |
| `SensitiveDataOutputGuardrail` | Output | Blocks responses that leak credentials — both your own configured API keys and anything matching a known key format. |
| `PiiMaskingGuardrail` | Input **and** Output | Masks personal data before it reaches the model and restores it in the response. Does not block. See [PII masking](#pii-masking). |

## Choosing an input guardrail

| | `PromptInjectionInputGuardrail` | `AiPromptInjectionInputGuardrail` |
| --- | --- | --- |
| **Detection method** | Regex patterns | LLM classifier |
| **Catches** | Keyword-based attacks | All of the above + roleplay, authority claims, narrative payloads, obfuscation |
| **False positives** | Low (narrowed patterns) | Very low (intent-aware) |
| **Latency** | ~0 ms | +LLM call per message |
| **Cost** | Free | Token cost (pin a cheap model with `AI.Guardrails.PromptInjection.Classifier.Model`) |
| **When to use** | Default / general use | High-security deployments, customer-facing chatbots |

## Configuring `AiPromptInjectionInputGuardrail`

Four variables control cost, coverage, and classification behaviour. Set them in the **Engine Cockpit**:

```yaml
Variables:
  AI:
    Guardrails:
      PromptInjection:
        Classifier:
          # AI provider for the classifier. When blank, falls back to AI.DefaultProvider.
          # Use a provider that offers cheap, fast models (e.g. OpenAI for gpt-4.1-nano).
          Provider: ""
          # Pin a cheaper model for the classifier to reduce token cost.
          # When blank, the provider's default model is used.
          Model: "gpt-4.1-nano"
          # Custom system prompt for the YES/NO classifier.
          # When blank, the built-in prompt is used (covers 8 attack categories and 5 safe categories).
          # Must instruct the model to reply with only YES or NO.
          SystemPrompt: ""
          # Allow messages shorter than this character count without an LLM call.
          # Default is 0 (all messages are evaluated). Raise this to skip the LLM
          # for very short messages once you understand your traffic patterns.
          MinLength: "0"
```

This guardrail costs one LLM call per message. `Model` and `MinLength` are the two levers that keep that bill down.

### Customising the system prompt

The built-in prompt covers generic prompt injection patterns. For domain-specific deployments you may need to extend it — for example, a financial chatbot that should also block attempts to invoke "advisor mode" with no compliance checks, or a support bot that should reject attempts to impersonate internal staff.

Set `SystemPrompt` to your own text. The prompt **must** end with an instruction to reply with only `YES` or `NO`:

```text
You are a prompt injection classifier for a financial services chatbot.
[... your custom rules ...]
Reply ONLY YES or NO.
```

Leave the variable blank to use the built-in prompt.

> **Important:** The classifier must reply with `YES` or `NO`. If the model returns anything else (e.g. a sentence), the guardrail **blocks the message as a precaution** and logs a warning to alert you to the misconfiguration.

## PII masking

`PiiMaskingGuardrail` keeps personal data out of a third-party model without changing what the user sees. It does not block anything: on the way in it replaces each detected value with an opaque token, and on the way out it substitutes the originals back. The model only ever processes anonymized text, while the caller gets a normal response.

This is the guardrail to reach for when GDPR, CCPA, or similar rules restrict what may be sent to an external processor.

### Registering it

It implements both interfaces and is stateful, so it must appear in **both** lists:

| Field | Value |
| --- | --- |
| `Input guardrails` | `PiiMaskingGuardrail` |
| `Output guardrails` | `PiiMaskingGuardrail` |

> **Important:** Registering it in only one list is worse than not registering it at all.
>
> - **Input only** — tokens are never translated back, so raw `<TYPE_hash>` placeholders reach the caller, and the stored mapping is never released.
> - **Output only** — the guardrail sees the model's reply as if it were an input and **masks the response** rather than restoring it.
>
> Phase is inferred from whether a mapping already exists for the current invocation, which is why the pairing matters.

### What it detects

Seven types, applied in this order:

| Token type | Detects | Limits |
| --- | --- | --- |
| `IP_ADDRESS` | IPv4 addresses, octet-range validated | IPv4 only — no IPv6 |
| `MAC_ADDRESS` | `:` or `-` separated | |
| `EMAIL` | Email addresses | |
| `PHONE` | International numbers | Requires a `+` or `00` prefix; `555-123-4567` is not detected |
| `CREDIT_DEBIT_CARD_NUMBER` | 13–19 digits passing a Luhn check | Any Luhn-valid number matches, so invoice or serial numbers can false-positive |
| `SSN` | US social security numbers | Requires a `-` or space separator; `123456789` is not detected |
| `DATE_OF_BIRTH` | Day-first dates in `19xx`/`20xx` | `DD/MM/YYYY` only — `06/15/1990` is not detected |

Tokens look like `<EMAIL_9f2c41ab77de>`: the type name plus 12 hex characters derived from a SHA-256 hash of the original value. The hash is a stable identifier, not something the model can reverse.

Reliable detection of phone numbers and card numbers in context needs NLP; these are regex rules and are documented in-source as approximations.

### Helping the model preserve tokens

Restoration works by finding the exact token in the response. If the model paraphrases it — writing "the email address" instead of `<EMAIL_9f2c41ab77de>` — there is nothing to substitute, and the value is silently not restored.

A short system message hint reduces that risk considerably:

```text
Values formatted as <TYPE_hash> are anonymized placeholders — the original sensitive data
was removed before reaching you. Treat each placeholder as an opaque token and echo it back as-is.
```

### Limits worth knowing

> **Important:** Masking is skipped, silently and completely, when the guardrail is invoked without an invocation id. Both the single-argument `evaluate` and a null id return "allow" without touching the message. If you have built a code path that calls guardrails directly rather than through an agent, PII passes through unmasked.

The guardrail instance is shared across all agents and holds its mappings in a plain `HashMap`, so concurrent agent calls are not safe against each other.

And masking is a risk reduction, not a privacy guarantee — session identifiers, metadata, and contextual detail can still identify a person. Pair it with data minimization; see [Security and Data](security-and-data.md).

For a working example, see the `piiMaskingGuardrailDemo` start in the [`GuardrailDemo`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Features/GuardrailDemo.p.json) process.

## Configuring default guardrails

Set the default guardrails in the **Engine Cockpit**, under **Variables**. They apply to every agent that does **not** explicitly configure its own guardrail list:

```yaml
Variables:
  AI:
    Guardrails:
      # Comma-separated list of guardrail names
      DefaultInput: PromptInjectionInputGuardrail
      DefaultOutput: SensitiveDataOutputGuardrail
```

## Using guardrails in agents

The `AgenticProcessCall` element has a **Guardrails** group with two pickers, `Input guardrails` and `Output guardrails`, listing every registered guardrail.

If a list is left empty, the agent falls back to the defaults from `variables.yaml`. Empty therefore does **not** mean unguarded — an agent with blank fields still runs whatever the application configured. This catches people out in tests especially, where a global input guardrail can reject fixture data.

> **Note:** Older processes stored these fields as a JSON array (`["PromptInjectionInputGuardrail"]`). That form is migrated automatically, but the current format is a comma-separated list produced by the picker. Use the picker rather than typing either form by hand.

## Handling guardrail errors

A blocked message becomes a BPM error. Which code you get depends on which side blocked:

| Error code | Raised when |
| --- | --- |
| `smartworkflow:guardrail:input:violation` | An input guardrail blocked the user message. |
| `smartworkflow:guardrail:output:violation` | An output guardrail blocked the model's response. |

Catch either with an **Error Boundary Event** on the agent element — the full recipe, and every other code Smart Workflow raises, is in [Error Codes](../reference/error-codes.md).

> **Note:** Output guardrails do not retry. The first failure discards the response — there is no second attempt with a re-prompt, so an output guardrail that blocks legitimate answers costs you the whole call.

## Observability

Every guardrail execution is recorded, in both channels: the Ivy conversation history when `AI.Observability.Ivy.Enabled` is on, and a dedicated `GUARDRAIL` span in Arize Phoenix when `AI.Observability.Openinference.Enabled` is on. The full record and span layouts are in [Observability](observability.md#guardrail-records).

## Common mistakes

- **Assuming blank means unguarded.** Blank inherits `AI.Guardrails.Default*`. This is the reverse of the tools field, where blank means none.
- **Registering `PiiMaskingGuardrail` on one side only.** Worse than not registering it — see above.
- **Matching on the error message.** Branch on the error code; the message is wrapped by LangChain4j and is not a stable contract.
- **Expecting an output guardrail to retry.** It does not. A false positive costs the whole call.
- **Paying for the LLM classifier on every message.** Pin a cheap model and raise `MinLength` once you know your traffic.

## See also

- [Agent Setup](../build/agent-setup.md) — where guardrails are configured on the element
- [Custom Guardrails](../contribute/guardrails-spi.md) — implementing and registering your own
- [Observability](observability.md) — viewing guardrail records and spans
- [Error Codes](../reference/error-codes.md) — handling a violation
- [Security and Data](security-and-data.md) — what leaves your network

For working examples, see the [`GuardrailDemo`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Features/GuardrailDemo.p.json) process, which has separate start links for the prompt-injection, sensitive-data, PII-masking, and custom-guardrail paths.
