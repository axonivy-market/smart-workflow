# Concepts

The vocabulary Smart Workflow uses, what each piece actually is, and the limits worth knowing before you design around them.

## The agent

An **agent** is a single `AgenticProcessCall` element in a process. It is not a long-running service or a persistent assistant — it is one step that makes one model call (or several, if tools are involved) and writes a result into process data.

Everything an agent knows comes from three places:

- its **system message**, the standing instructions
- its **user message**, the data for this call
- whatever its **tools** return while it is working

Nothing else. The agent has no access to your database, your process history, or its own past runs unless a tool gives it one.

## Provider and model

The **provider** is the vendor integration — OpenAI, Anthropic and Ollama among others. The **model** is the specific model within it, like `gpt-4.1-mini` or `claude-haiku-4-5`.

Both are chosen per element, and both fall back: an empty `Provider` uses `AI.DefaultProvider`, an empty `Model` uses that provider's `DefaultModel`. Because the choice is per element, one process can use three different providers — see [Mixing providers](build/providers.md#mixing-providers-in-one-process).

Providers are not interchangeable. Check [Provider Capabilities](reference/capabilities.md) before relying on file input or typed output.

## Tools

A **tool** is something the agent can decide to call. Two kinds exist:

- a **callable sub-process** tagged `tool` — the normal case, and the one that gives you the whole process designer
- a **Java class** implementing `SmartWorkflowTool`, registered via SPI — for logic with no workflow steps

The model never sees your implementation. It sees the tool's description and its input parameter names, types and descriptions, and decides from those alone whether to call it. That makes those descriptions the highest-leverage text in the whole system.

Tools are *offered*, not *invoked*. The agent chooses. If you need something to happen every time, put it in the process, not in a tool.

Two rules follow from how they work:

- An empty `Available tools` field means the agent has **no tools to use at all**.
- Every tool you grant costs tokens in every request and gives the model another way to choose wrong. Keep lists tight.

See [Defining Tools](build/tools.md).

## Structured output

By default an agent returns a `String`. Set `Expect result of type` to a class — `com.axonivy.utils.ai.Invoice.class` — and it returns an instance of that class instead: a JSON schema is derived from the class, sent to the model as a response-format constraint, and the reply is deserialized.

This is what makes an agent usable in a process rather than just readable by a human. The field names you choose are what the model sees, so name them descriptively.

Not every provider supports it. See [Structured output](reference/capabilities.md#structured-output).

## Guardrails

A **guardrail** inspects a message and allows, rewrites, or blocks it. Input guardrails run before the model sees the user message; output guardrails run before the response is used.

Leaving the guardrail fields empty does **not** mean the agent has no guardrails. It uses the default guardrails set for the application. This is the opposite of the tools field, where empty means no tools at all.

See [Guardrails](operate/guardrails.md).

## Memory

Each agent call is self-contained. The agent works from its system message, its user message, and whatever its tools return during that call — nothing is carried in from an earlier call, so every run is predictable and repeatable.

Within a call, the agent keeps the full conversation and remembers its own tool results while it works. That message list grows with each tool call and has no cap, so watch the cost on long tool loops.

To let an agent continue a conversation in a later call — for example when a person has to answer something before it can finish — use the `aiMemoryId` process data field. See [Human in the Loop](build/human-in-the-loop.md) for how it works and when to reach for it.

## Configuration

Everything is configured through Ivy variables under a single `AI` block — no separate config file, no code. You set their values in the **Engine Cockpit**; the variables themselves are declared by the projects you install. Values are read **on each agent call**, so a change takes effect immediately without a restart.

API keys are declared as secrets, and the Engine Cockpit encrypts them on entry — they are never stored in plain text and never belong in source control.

See [Variables](reference/variables.md) for the complete list.

## See also

- [Getting Started](getting-started.md) — build your first agent
- [Agent Setup](build/agent-setup.md) — the element in full
- [Agent Patterns](build/patterns.md) — arranging more than one agent
- [Security and Data](operate/security-and-data.md) — what leaves your network
- [Troubleshooting](troubleshooting.md) — when something does not work
