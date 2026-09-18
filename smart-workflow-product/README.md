# Smart Workflow

*[Deutsche Version](README_DE.md)*

**Smart Workflow** brings AI directly into Axon Ivy, so developers can build, run, and improve AI agents inside existing Axon processes. It lets business workflows leverage large language models to understand natural language, make autonomous decisions, and adapt to changing requirements — all without heavy architectural changes.

Key benefits of Smart Workflow:

- **Familiar setup:** Drop AI agents into BPMN processes with no structural changes and configure everything through Axon Ivy’s standard interfaces.
- **Enterprise-ready:** Built for enterprise needs with logging, monitoring, and configuration controls.
- **Flexible tools:** Turn any callable process into an AI-discoverable tool.
- **Multi-model support:** Use lightweight or advanced models depending on the task.
- **Type-safe outputs:** Produce structured Java objects from AI responses for immediate use.
- **Natural language handling:** Accept unstructured input and return human-friendly output.

**Disclaimer**

The **user is solely responsible** for the configuration, deployment, and operation of the AI and its associated agents. Any decisions, actions, or outcomes resulting from the use of this connector are entirely the responsibility of the user.

We provide only the **technical capability** to enable such configurations and expressly disclaim any liability for misuse, misconfiguration, or unintended consequences arising from its use. By using this connector, you acknowledge and accept these limitations.

## Features

| Feature | What it gives you |
| --- | --- |
| [AI agent element](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/agent-setup.md) | `AgenticProcessCall` — an agent as a process step, returning text or a typed Java object |
| [Model providers](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/providers.md) | Hosted APIs, an enterprise platform, or a model on your own hardware — chosen globally or per agent |
| [Tools](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/tools.md) | Any callable sub-process becomes a tool the agent can invoke, plus Java tools and a built-in `webSearch` |
| [File extraction](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/file-extraction.md) | Read invoices, forms and scans directly from PDF and image files |
| [RAG](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/rag.md) | Ground answers in your own documents with OpenSearch vector search |
| [Human in the loop](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/human-in-the-loop.md) | Suspend an agent mid-run for a human decision as an Ivy task, then resume it |
| [Guardrails](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/guardrails.md) | Prompt-injection defence, credential-leak blocking, and PII masking for GDPR-sensitive data |
| [Circuit breaker](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/circuit-breaker.md) | One switch that stops every AI call in the application |
| [Observability](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/observability.md) | Arize Phoenix tracing, Ivy conversation history for audit, and AI-usage custom fields |

📘 **[Full documentation](https://github.com/axonivy-market/smart-workflow/blob/master/README.md)** · 🚀 **[Getting Started](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/getting-started.md)**

## Demo

The `smart-workflow-demo` project ships with runnable examples for every feature. Complete the [Setup](#setup) first, then start the named process from the process list.

### Axon Ivy Support Agent Demo

An AI-powered support desk inside a business workflow. The agent reads a support question, classifies it as a Portal, Core or Market issue, notices when information such as the product version is missing, and creates a support task through the `createAxonIvySupportTask` tool. Its answer comes back as a typed Java object, so the classification, task link and summary are ready for the next process step.

![Support Ticket example](img/support-ticket-example.png)

Start the **Axon Ivy Support** process with a question and a username.

### Shopping Demo

A small e-commerce store run by agents, in two parts. **Product creation** replaces hours of manual data entry: the operator imports a product specification and images, and four agents parse them, validate the attributes, and resolve or create the supplier, brand and category before creating the product. **Semantic search** lets a shopper ask for "a $100 red dress for a party tonight" — the agent extracts the intent and price range as structured criteria, which the backend turns into a query.

Start **Create new product** or **Shopping Store**, and **Create data for shopping demo** first to populate the store.

### File Extraction Demo

Reads invoice data straight from uploaded images and PDFs with no manual data entry. Reference the file in the agent's user message, and a multimodal model returns the fields as a typed Java object.

Start **File Extraction Demo (CMS)** or **File Extraction Demo (Binary)**. Not every provider reads every format — see [Provider Capabilities](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/reference/capabilities.md#file-extraction).

### Guardrail Demo

Shows the built-in guardrails stopping an attack from both directions: `PromptInjectionInputGuardrail` blocks a crafted message before the model is called, and `SensitiveDataOutputGuardrail` blocks a response that would leak credentials. Each violation raises a BPM error that the process catches with an error boundary event and routes to a safe fallback.

Start **Prompt Injection Guardrail Demo** or **Sensitive Data Output Guardrail Demo**.

### Custom Guardrail Demo

A company policy enforced in one place instead of in every system prompt: `BlockCompetitorMentionGuardrail` refuses any query mentioning a competitor product. Once registered, a custom guardrail appears in the agent's guardrail pickers like any built-in.

Start **Custom Guardrail Demo - Blocked** or **Custom Guardrail Demo - Allowed**.

### Agent Patterns

The demo project also illustrates three ways to structure agents and tools in a larger application: a linear **Agent Pipeline**, a **Self-Contained Agent** with co-located tools, and **Feature-Grouped** agents that share tools across a business domain.

See [Agent Patterns](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/patterns.md).

## Setup

Smart Workflow isn't bound to a specific AI vendor — you choose your model provider at installation time and can change it later without touching a process.

1. Import the provider project you want to use, for example `smart-workflow-openai`.
2. In the **Engine Cockpit**, under **Variables**, set `AI.DefaultProvider` to that provider.
3. Set the provider's API key in the same place. The Cockpit encrypts it as you save, so keys are never stored in plain text. Ollama needs no key — the instance is yours.

Each provider has its own variables. The configuration block for every one of them is documented here:

| Provider | Configuration |
| --- | --- |
| OpenAI | [Setup](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/providers.md#openai) |
| Azure OpenAI | [Setup](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/providers.md#azure-openai) |
| Google Gemini | [Setup](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/providers.md#gemini) |
| Anthropic | [Setup](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/providers.md#anthropic) |
| x.AI | [Setup](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/providers.md#xai) |
| Ollama (self-hosted) | [Setup](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/providers.md#ollama) |

They differ in what they support — images, PDFs, structured output and embedding are not available everywhere. Check [Provider Capabilities](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/reference/capabilities.md) before you commit to one.

To request support for an additional provider, please open an issue or submit a pull request on GitHub.

### Next steps

- [Getting Started](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/getting-started.md) — from installation to a working agent
- [Agent Setup](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/agent-setup.md) — every field of the `AgenticProcessCall` element
- [Defining Tools](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/tools.md) — turning a callable sub-process into a tool
- [Guardrails](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/guardrails.md) — validating what goes to the model and what comes back
- [Variables](https://github.com/axonivy-market/smart-workflow/blob/master/doc/user/reference/variables.md) — every `AI.*` setting in one table
