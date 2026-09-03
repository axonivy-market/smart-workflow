# Agent Setup

`AgenticProcessCall` is the process element that puts an AI agent step inside a process. You declare what to ask, what data to pass in, and where the answer goes; the element handles the model call.

Add it from **Extension > Program Elements** in the Designer, then double-click to configure.

## Prerequisites

An agent needs a model provider and a key before it can run — at minimum `AI.DefaultProvider` and that provider's `APIKey`. See [Model Providers](providers.md) for every provider's configuration block and how keys are handled, or [Getting Started](getting-started.md) if you have not set one up yet.

## Element configuration

The editor is organized in five groups.

### Message

![The Message group of the agent element](img/agent-message-configurations.png)

Both fields are multi-line, and both accept `<%=...%>` to inject process data. Anything you can reach from IvyScript can go into either message.

**`System message`** holds the agent's standing instructions — who it is, what it must do, what format to produce. Be specific: the model knows nothing about your business, so state what to do, what to leave out, and what shape to return. Use `<%=...%>` for anything that should not be hard-coded into the element, such as company policy, the current department, or an approval threshold:

```text
You are a purchase request assistant for <%=in.companyName%>.

Follow these company policies at all times:
<%=in.policyText%>

Approve requests below <%=in.autoApproveLimit%> without asking.
Anything above that must go to a human approver.
Return a single sentence with the decision and the reason, and nothing else.
```

Where those values come from is up to the process — a CMS entry, an Ivy variable, a read from an earlier step. A policy change then updates one source instead of every agent in the application. Note that files are not handled in this field: every expression becomes text.

**`User message`** holds the data to reason over on this call, usually a straight reference to a process data field:

```text
<%=in.invoiceText%>
```

This is also the field that supports [file extraction](file-extraction.md) — an expression resolving to a file becomes image or PDF content.

If you run into trouble crafting these messages, [Messages and expressions](troubleshooting.md#messages-and-expressions) covers what usually causes it.

### Tools

`Available tools` lists the tools an agent can use — both callable sub-processes tagged `tool` and Java tools registered via SPI.

> **Important:** An empty `Available tools` field means the agent has **no tools to use at all**. If your agent ignores a tool you expected it to call, check that the tool is actually selected here.

See [Defining Tools](tools.md) for writing tools and for how their descriptions reach the model.

### Guardrails

`Input guardrails` and `Output guardrails` are pickers over the registered guardrails.

By default, Smart Workflow applies the guardrails configured for the application — `AI.Guardrails.DefaultInput` and `AI.Guardrails.DefaultOutput`. Every agent is protected without anything being set on the element.

To give one agent a different set, select them in these pickers; what you select replaces the defaults for that agent. Leave the fields empty to keep the application defaults.

> **Note:** Because empty means "use the defaults" rather than "no guardrails", an agent you never configured still runs whatever the application set. Worth remembering in tests, where a global input guardrail can reject fixture data.

See [Guardrails](guardrails.md).

### Model

**`Provider`** selects which model provider this agent calls. Leave it empty to use the application's provider from `AI.DefaultProvider`; set it when one agent needs something different, such as a self-hosted model for a step handling sensitive data.

**`Model`** selects which of that provider's models to use. Leave it empty for the provider's default model. This is a script field, so the value must be quoted: `"gpt-4o"` — a bare `gpt-4o` will not compile. See [Model Providers](providers.md#per-agent-override) for the resolution order and per-provider model names.

### Output

![The Output group of the agent element](img/agent-other-configurations.png)

**`Expect result of type`** declares the type the agent should return. Leave it empty for plain text, which is what most agents need and requires no output configuration at all. To get a typed Java object instead, set it to a class such as `com.axonivy.utils.ai.Invoice.class` — see [Structured output](#structured-output).

**`Map result to`** is where the result is written, for example `in.summary`. The response lands in that process data field ready to display, log, or pass on — no parsing, no casting.

If the mapped field stays empty after a run, [Troubleshooting](troubleshooting.md#the-agent-did-not-answer) covers the usual cause.

## Structured output

An agent returns text by default. To get a typed Java object instead, set `Expect result of type` to the class you want back:

```java
com.axonivy.utils.ai.Invoice.class
```

Smart Workflow derives a JSON schema from that class, sends it to the model as a response-format constraint, and deserializes the reply into an instance. Because the schema comes from the class, your field names are what the model sees, so name them the way you would describe them, since a clear `invoiceNumber` is worth more than a line of prompt. Any Ivy data class or plain Java class works, provided it is on the runtime classpath; a bare collection is not valid, so to return a list, declare a class with the list as one of its fields.

Check the structured output support of your provider in [Provider Capabilities](reference/capabilities.md#structured-output).

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
| `User message` | `<%=in.invoiceText%>` |
| `Expect result of type` | _(empty — plain `String`)_ |
| `Map result to` | `in.summary` |

To turn the same agent into a typed extractor, set `Expect result of type` to `com.axonivy.utils.ai.Invoice.class` and describe the fields in the system message. [File Extraction](file-extraction.md#example) has that version in full, reading the invoice from a document rather than from text.

For working implementations, see the [demo processes](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/) — `AgentDemo/SupportAgent.p.json` for a tool-using agent and `Features/FileExtractionDemo.p.json` for typed extraction.

## Calling an agent from a subprocess

Smart Workflow also exposes an agent as a callable subprocess, used by Axon Ivy Portal features:

```text
Portal/SmartWorkflowAgent:invokeAgent(String,String,List<String>,Class)
```

The arguments are the system message, the user message, the list of tool names, and the expected result type. Refer to the Axon Ivy Portal documentation for how Portal uses it.

## See also

- [Model Providers](providers.md) — choosing and configuring a provider
- [Defining Tools](tools.md) — giving an agent something to do
- [Guardrails](guardrails.md) — validating input and output
- [File Extraction](file-extraction.md) — images and PDFs as input
- [Human in the Loop](human-in-the-loop.md) — suspending an agent for a human decision
- [Agent Patterns](patterns.md) — structuring several agents in one process
- [Troubleshooting](troubleshooting.md) — when an agent does not answer, or answers wrongly
