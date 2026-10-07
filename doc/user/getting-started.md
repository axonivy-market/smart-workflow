# Getting Started

From nothing to a working AI agent in a process. Around fifteen minutes, most of it waiting for Maven.

**You need:** an Axon Ivy Designer on **14.0.0**, and an API key from one model provider. OpenAI is the default and the quickest to start with; if you would rather run a model on your own hardware and skip the key entirely, jump to [Ollama](providers.md#ollama).

## 1. Install from the Market

Install **Smart Workflow** from the Axon Ivy Market. The installer adds two things to your workspace.

**Dependencies** — the runtime, added to your project:

| Project | |
| --- | --- |
| `smart-workflow` | The core. Always installed. |
| `smart-workflow-openai` | Connects to OpenAI. Installed by default. |
| `smart-workflow-anthropic` and the other connector projects | Optional — select the ones you want during installation. See [Model Providers](providers.md). |
| `smart-workflow-opensearch-rag` | Optional. Needed only for [RAG](rag.md). |
| `smart-workflow-dashboard` | Optional. The governance dashboard — see [Observability](observability.md). |

**Demo projects** — imported into the workspace so you have something to read: `smart-workflow-demo` and `smart-workflow-supplier-demo`.

## 2. Configure a provider

Configure Smart Workflow using Ivy variables. These variables are already defined in the installed provider project. You only need to set their values:

| Variable | Value |
| --- | --- |
| `AI.DefaultProvider` | `OpenAI` |
| `AI.Providers.OpenAI.APIKey` | your API key |

[Variables](reference/variables.md) lists every one.

## 3. Add an agent to a process

In this example, the agent receives a question and saves its answer to the process data.

### Prepare the process data

Add these two fields to your process data class:

| Field | Type | Purpose |
| --- | --- | --- |
| `question` | `String` | The question to ask the agent |
| `answer` | `String` | The answer returned by the agent |

Add a **Script** step to your process and set the question:

```java
in.question = "What is Axon Ivy?";
```

### Configure the agent

After the Script step, add an **AgenticProcessCall** element from **Extension > Program Elements**. Double-click it to open its configuration.

Fill in these three fields:

| Group | Field | Value |
| --- | --- | --- |
| Message | System message | `You are a helpful assistant. Answer in one short sentence.` |
| Message | User message | `<%=in.question%>` |
| Output | Map result to | `in.answer` |

The **System message** tells the agent how to respond. The **User message** reads the question from your process data. **Map result to** saves the agent’s response in `answer`.

Keep the other settings at their defaults. The agent uses the provider and model configured in step 2 and returns its answer as plain text.

### Run the process

Smart Workflow sends the system message (the instructions) and the user message (your question) to the configured AI model. The model generates a response, which Smart Workflow stores in `in.answer`. The next steps in your process can then use this answer.

## 4. Run it

Start the process. The agent sends your question to the model and writes the reply straight into `in.answer`:

```
Axon Ivy is a low-code platform for designing, automating and running business processes.
```

That is a working AI agent — three fields and no code. From here it is the same element throughout: add tools and it can act on your processes, set `Expect result of type` to a class such as `com.axonivy.utils.ai.Invoice.class` and it returns a typed Java object instead of a string.

## Common mistakes

A few things are easy to get wrong the first time. If your agent does not answer as expected, check the list below.

| Symptom | Cause |
| --- | --- |
| No call was made at all | The user message expanded to nothing. Look for `Agent call was skipped, since there was no user query` in the log. |
| `in.answer` is empty but the log shows a call | `Map result to` failed, usually a type mismatch. Look for `Failed to map result to`. |
| The reply contains `<%=in.question%>` literally | The expression could not be resolved — check the field name on the data class. |
| An error about an unknown provider | `AI.DefaultProvider` does not match an installed provider project. |
| An authentication error from the provider | The key is wrong, or was never set in the Engine Cockpit. |

[Troubleshooting](troubleshooting.md) covers the rest, with the log line that identifies each one.

## Where to go next

You now have an agent that talks. To make it *do* something:

| Next | Why |
| --- | --- |
| [Defining Tools](tools.md) | Let the agent call your processes. This is the step that turns a chatbot into a workflow participant. |
| [Agent Setup](agent-setup.md) | The full field reference, including typed output instead of a string. |
| [Guardrails](guardrails.md) | Before anything faces real users. |
| [Concepts](concepts.md) | The vocabulary, and the limits worth knowing early. |

The `smart-workflow-demo` project in your workspace has a working example of each feature, and [Agent Patterns](patterns.md) covers how to arrange several agents once one is not enough.
