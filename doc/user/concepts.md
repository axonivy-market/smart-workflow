# Concepts

The vocabulary Smart Workflow uses, what each piece actually is, and the limits worth knowing before you design around them.

## AI models and messages

A **large language model (LLM)** is an AI model trained on large amounts of data to understand and generate language. It can perform tasks such as answering questions, summarizing documents, and extracting information.

You give the model instructions and information, and it generates a response. In Smart Workflow, you provide these through two fields:

| Field | Purpose | Example |
| --- | --- | --- |
| **System message** | Defines how the agent should behave and the rules it should follow. | `You are a helpful assistant. Answer in one short sentence.` |
| **User message** | Provides the task and the data for this call. It can contain text or values from process data. | `<%=in.question%>` |

## Prompt

A **prompt** is the input used to guide the model's response. People often use this term for a single instruction, but a model request can contain several messages. Your system message and user message are both part of that input.

For example, if the variable `in.question` contains `What is Axon Ivy?`, Smart Workflow sends that question together with the system instructions to the model.

The model has general knowledge from its training, but it does not automatically know your application data or current business information. Supply the information it needs through messages, tools, or conversation memory.

## Context and tokens

**Context** is the information available to the model when it generates a response. It can include your messages, tool definitions, tool results, chat messages, and supported files.

Models process text in small units called **tokens**. A token can be a word, part of a word, or punctuation. Tokens are not the same as characters or words.

Tokens matter for two reasons:

- **Capacity:** a model's **context window** limits how much tokens it can handle in one request.
- **Cost:** many providers charge based on the number of input and output tokens processed.

Long messages, large documents, and repeated tool calls can increase token usage. Include the information needed for the task, and avoid sending unrelated data.

## The agent

An **agent** is a single `AgenticProcessCall` element in a process. It is not a long-running service or a persistent assistant — it is one step that makes one model call (or several, if tools are involved) and writes a result into process data.

Everything an agent knows comes from three places:

- its **system message**, the standing instructions
- its **user message**, the data for this call
- whatever its **tools** return while it is working

Nothing else. The agent has no access to your database, your process history, or its own past runs unless a tool gives it one.

## Provider and model

The **provider** is the vendor integration — OpenAI, Anthropic and Ollama among others. The **model** is the specific model within it, like `gpt-4.1-mini` or `claude-haiku-4-5`.

Both are chosen per element, and both fall back: an empty `Provider` uses `AI.DefaultProvider`, an empty `Model` uses that provider's `DefaultModel`. Because the choice is per element, one process can use three different providers — see [Mixing providers](providers.md#mixing-providers-in-one-process).

They differ in what they support — check [Provider Capabilities](reference/capabilities.md) before relying on file input or typed output.

## Tools

A **tool** is something the agent can decide to call. Two kinds exist:

- a **callable sub-process** tagged `tool` — the normal case, and the one that gives you the whole process designer
- a **Java class** — for logic with no workflow steps

The model never sees your implementation. It sees the tool's description and its input parameter names, types and descriptions, and decides from those alone whether to call it. That makes those descriptions the highest-leverage text in the whole system.

Tools are *offered*, not *invoked*. The agent chooses. If you need something to happen every time, put it in the process, not in a tool.

Two rules follow from how they work:

- An empty `Available tools` field means the agent has **no tools to use at all**.
- Every tool you grant costs tokens in every request and gives the model another way to choose wrong. Keep lists tight.

See [Defining Tools](tools.md).

## Structured output

By default an agent returns a `String`. Set `Expect result of type` to a class — `com.axonivy.utils.ai.Invoice.class` — and it returns an instance of that class instead: a JSON schema is derived from the class, sent to the model as a response-format constraint, and the reply is deserialized.

This is what makes an agent usable in a process rather than just readable by a human. The field names you choose are what the model sees, so name them descriptively.

Not every provider supports it. See [Structured output](reference/capabilities.md#structured-output).

## Guardrails

A **guardrail** inspects a message and allows, rewrites, or blocks it. Input guardrails run before the model sees the user message; output guardrails run before the response is used.

Leaving the guardrail fields empty does **not** mean the agent has no guardrails. It uses the default guardrails set for the application. This is the opposite of the tools field, where empty means no tools at all.

See [Guardrails](guardrails.md).

## Memory

Each agent call is self-contained. The agent works from its system message, its user message, and whatever its tools return during that call — nothing is carried in from an earlier call, so every run is predictable and repeatable.

Within a call, the agent keeps the full conversation and remembers its own tool results while it works. That message list grows with each tool call and has no cap, so watch the cost on long tool loops.

To let an agent continue a conversation in a later call — for example when a person has to answer something before it can finish — use the `aiMemoryId` process data field. See [Human in the Loop](human-in-the-loop.md) for how it works and when to reach for it.

## Human in the loop

**Human in the loop** means involving a person during an otherwise automated workflow. A person might provide missing information, review an AI-generated result, or approve an action before it is executed.

For example, an agent could prepare a purchase request, then the process could wait for a person to review it before submitting the order.

Use process steps to enforce required human approval. An instruction asking the agent to seek approval is not a substitute for an approval step in the workflow.

If the agent needs to continue its conversation after the person's response, memory can carry the earlier context into the next call.

See [Human in the Loop](human-in-the-loop.md).

## Retrieval-augmented generation (RAG)

**Retrieval-augmented generation (RAG)** means retrieving relevant information from a source and supplying it to the model to help generate an answer.

For example, to answer a question about company leave policies, a workflow can:

1. Search the company's policy documents for relevant sections.
2. Provide those sections and the question to the model.
3. Ask the model to answer using the supplied information.

Retrieval can happen in a process step before the agent runs, or through a tool the agent can call. RAG does not retrain the model; it adds information to the current context.

RAG is useful for private or frequently changing information that the model may not know. Its reliability depends on finding the right sources and using them correctly. Retrieved content also consumes context space, so provide relevant passages rather than unrelated documents.

## Configuration

Everything is configured through Ivy variables under a single `AI` block — no separate config file, no code. You set their values in the **Engine Cockpit**; the variables themselves are declared by the projects you install. Values are read **on each agent call**, so a change takes effect immediately without a restart.

API keys are declared as secrets, and the Engine Cockpit encrypts them on entry — they are never stored in plain text and never belong in source control.

See [Variables](reference/variables.md) for the complete list.

## See also

- [Getting Started](getting-started.md) — build your first agent
- [Agent Setup](agent-setup.md) — the element in full
- [Agent Patterns](patterns.md) — arranging more than one agent
- [Troubleshooting](troubleshooting.md) — when something does not work
