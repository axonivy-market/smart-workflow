# Agent Setup

`AgenticProcessCall` is the process element that puts an AI agent step inside a process. You declare what to ask, what data to pass in, and where the answer goes; the element handles the model call.

Add it from **Extension > Program Elements** in the Designer, then double-click to configure. The element is backed by the `com.axonivy.utils.smart.workflow.AgenticProcessCall` Java bean.

## Prerequisites

An agent needs a model provider and a key before it can run — at minimum `AI.DefaultProvider` and that provider's `APIKey`. See [Model Providers](providers.md) for every provider's configuration block and how keys are handled, or [Getting Started](../getting-started.md) if you have not set one up yet.

## Element configuration

The editor is organized in five groups.

### Message

| Field | Purpose |
| --- | --- |
| `System message:` | The agent's standing instructions — who it is, what it must do, what format to produce. |
| `User message:` | The data to reason over on this call. |

![The Message group of the agent element](../img/agent-message-configurations.png)

Both accept `<%=...%>` for injecting process data, and both are multi-line.

A good system message is specific. The model knows nothing about your business, so state what to include, what to leave out, and what shape to return:

```text
You are an invoice summary agent.
Read the invoice text and return a single sentence containing
the invoice number, supplier name, total amount, and due date.
Do not add any other commentary.
```

The user message is usually a straight reference to a process data field:

```text
<%=in.invoiceText%>
```

The two fields resolve expressions differently, which matters more than it looks:

- **User message** — each `<%=...%>` is evaluated as an individual IvyScript expression, and the literal text around them is kept as-is. Expressions resolving to a file become image or PDF content, so this is the field that supports [file extraction](file-extraction.md).
- **System message** — expanded as one macro template. No file extraction; a file reference here is stringified, not attached.

> **Important:** Two behaviours are worth knowing before you start debugging a prompt.
>
> - An expression that cannot be resolved is inserted into the message as its own **raw `<%=...%>` text**. If the model echoes something like `<%=in.invoiceText%>`, the expression is wrong, not the prompt.
> - If expanding the user message throws, the agent call is **skipped entirely** and the process moves on as if nothing happened. The only trace is `Agent call was skipped, since there was no user query` in the log. A blank user message does the same thing, which is deliberate — it keeps a half-configured element from failing while you build it.

### Tools

`Available tools:` is a picker listing every tool Smart Workflow can find — callable sub-processes tagged `tool`, and Java tools registered via SPI.

> **Important:** An empty `Available tools:` field gives the agent **no tools at all**. It does not mean "all tools". If your agent ignores a tool you expected it to call, check that the tool is actually selected here.

See [Defining Tools](tools.md) for writing tools and for how their descriptions reach the model.

### Guardrails

`Input guardrails:` and `Output guardrails:` are pickers over the registered guardrails.

Unlike the tools field, **empty here means "use the defaults"** — `AI.Guardrails.DefaultInput` and `AI.Guardrails.DefaultOutput` from `variables.yaml`. So an agent with blank guardrail fields is not unguarded; it inherits whatever the application configured. This surprises people in tests especially, where a globally configured input guardrail can block fixture data.

See [Guardrails](../operate/guardrails.md).

### Model

| Field | Notes |
| --- | --- |
| `Provider:` | Empty falls back to `AI.DefaultProvider`. |
| `Model:` | A script field returning `String`, so quote it: `"gpt-4o"`. Empty falls back to the provider's `DefaultModel`. |

`Model:` being a script field is easy to trip over — a bare `gpt-4o` will not compile. See [Model Providers](providers.md#per-agent-override) for the full resolution order and per-provider model names.

### Output

| Field | Notes |
| --- | --- |
| `Expect result of type:` | A script field returning a `Class`. Leave empty for a plain `String`. |
| `Map result to:` | Where the result is written, e.g. `in.summary`. |

![The Output group of the agent element](../img/agent-other-configurations.png)

By default an agent returns plain text, and no output configuration is needed at all. Setting `Map result to:` to `in.summary` writes the response into the `summary` field of the process data — a `String`, ready to display, log, or pass on. No parsing, no casting.

> **Important:** `Map result to:` is executed as a generated assignment, and a failure is **logged but not thrown** — you get `Failed to map result to <expression>` in the log and a silently empty field. A type mismatch between the declared output class and the target field fails exactly this way.

## Structured output

To get a typed object instead of a string, set `Expect result of type:` to the class you want back:

```java
com.axonivy.utils.ai.Invoice.class
```

LangChain4j derives a JSON schema from the class, sends it to the model as a response-format constraint, and deserializes the reply into an instance. The rest of the process then reads typed fields directly:

```java
in.result.invoiceNumber
in.result.totalAmount
```

Field names are what the model sees, so name them the way you would describe them: `invoiceNumber`, `totalAmount`, `invoiceDate`. A clear name is worth more than a line of prompt.

The field is a script expression that must evaluate to a `java.lang.Class`. `MyClass.class` is the usual way to write that, but any expression returning a `Class` works — including a variable, which is how the Portal bridge passes a caller-chosen type:

```java
in.resultType
```

> **Important:** A bare collection is not a supported output type. To return a list, wrap it in a composite object that has the list as a field.

Both Ivy data classes and plain Java classes work, as long as the class is on the runtime classpath.

Structured output is the one area where providers differ enough to change your design — Gemini does not support it at all, and Ollama drops the schema when the agent also has tools. Check [Provider Capabilities](../reference/capabilities.md#structured-output) before you rely on it.

## Example

A minimal text agent, start to finish:

**System message:**

```text
You are an invoice summary agent.
Read the invoice text and return a single sentence containing
the invoice number, supplier name, total amount, and due date.
Do not add any other commentary.
```

| Field | Value |
| --- | --- |
| `User message:` | `<%=in.invoiceText%>` |
| `Expect result of type:` | _(empty — plain `String`)_ |
| `Map result to:` | `in.summary` |

To turn the same agent into a typed extractor, set `Expect result of type:` to `com.axonivy.utils.ai.Invoice.class` and describe the fields in the system message. [File Extraction](file-extraction.md#example) has that version in full, reading the invoice from a document rather than from text.

For working implementations, see the [demo processes](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/) — `AgentDemo/SupportAgent.p.json` for a tool-using agent and `Features/FileExtractionDemo.p.json` for typed extraction.

## When an agent does not respond

In rough order of how often it is the cause:

1. **The circuit breaker is on.** `AI.CircuitBreaker.Enabled: "true"` stops every AI call in the application. See [Circuit Breaker](../operate/circuit-breaker.md).
2. **The user message is blank or failed to expand.** The call is skipped; check the log for `Agent call was skipped`.
3. **A guardrail blocked it.** Blank guardrail fields still inherit the application defaults. See [Guardrails](../operate/guardrails.md).
4. **The result did not map.** The agent ran fine but the target field stayed empty; check the log for `Failed to map result to`.
5. **No tools were selected**, so the agent could not do the thing you expected it to do.

[Error Codes](../reference/error-codes.md#problems-without-an-error-code) pairs each symptom with the log line that identifies it.

## See also

- [Model Providers](providers.md) — choosing and configuring a provider
- [Defining Tools](tools.md) — giving an agent something to do
- [Guardrails](../operate/guardrails.md) — validating input and output
- [File Extraction](file-extraction.md) — images and PDFs as input
- [Human in the Loop](human-in-the-loop.md) — suspending an agent for a human decision
- [Agent Patterns](patterns.md) — structuring several agents in one process
