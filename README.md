# Smart Workflow 🪄️

[![CI Build](https://github.com/axonivy-market/smart-workflow/actions/workflows/ci.yml/badge.svg)](https://github.com/axonivy-market/smart-workflow/actions/workflows/ci.yml)

Let AI Agent elements drive your dynamic processes.

Smart Workflow adds an **AI agent** to Axon Ivy as a process element. Give it instructions, a set of your own callable sub-processes as tools, and a model — it reads natural language, decides what to call, and writes a typed Java object back into your process data. It connects to several model providers, from hosted APIs to a model running on your own hardware.

**New here?** [Getting Started](doc/user/getting-started.md) takes you from installation to a working agent in about fifteen minutes. [Concepts](doc/user/concepts.md) explains the vocabulary and the limits worth knowing early. If something is not working, start from [Troubleshooting](doc/user/troubleshooting.md).

## Build

Everything you need to make an agent do something.

| Guide | What it covers |
| --- | --- |
| [Agent Setup](doc/user/build/agent-setup.md) | The `AgenticProcessCall` element — messages, output, structured output, and the failure modes worth knowing |
| [Model Providers](doc/user/build/providers.md) | Choosing and configuring a provider, globally or per agent |
| [Defining Tools](doc/user/build/tools.md) | Callable process tools, Java tools, and the built-in `webSearch` |
| [File Extraction](doc/user/build/file-extraction.md) | Passing images and PDFs to a model |
| [RAG](doc/user/build/rag.md) | Grounding answers in your own documents via OpenSearch |
| [Human in the Loop](doc/user/build/human-in-the-loop.md) | Suspending an agent for a human decision, then resuming it |
| [Agent Patterns](doc/user/build/patterns.md) | Arranging several agents and tools in one application |

## Operate

Running agents safely, and knowing what they did.

| Guide | What it covers |
| --- | --- |
| [Guardrails](doc/user/operate/guardrails.md) | Prompt-injection defence, sensitive-data blocking, and PII masking |
| [Circuit Breaker](doc/user/operate/circuit-breaker.md) | The application-wide switch that stops all AI calls |
| [Observability](doc/user/operate/observability.md) | Arize Phoenix tracing, Ivy conversation history, and AI-assisted custom fields |
| [Security and Data](doc/user/operate/security-and-data.md) | What leaves your network, what is stored, and for how long |

## Reference

Look-up tables. Each fact lives here once; the guides link to it.

| Page | What it covers |
| --- | --- |
| [Provider Capabilities](doc/user/reference/capabilities.md) | Which provider supports images, PDFs, structured output and embedding |
| [Variables](doc/user/reference/variables.md) | Every `AI.*` setting, with defaults and secrets marked |
| [Error Codes](doc/user/reference/error-codes.md) | Every BPM error — and every failure that raises none |
| [Java API](doc/user/reference/java-api.md) | The public Java surface: tools, guardrails, providers, `DecisionMaker` |

## Contribute

Extending Smart Workflow itself.

| Guide | What it covers |
| --- | --- |
| [Chat Models](doc/user/contribute/models.md) | Adding a new model provider |
| [Custom Guardrails](doc/user/contribute/guardrails-spi.md) | Implementing and registering your own guardrail |
| [Demo Projects](doc/user/contribute/demo.md) | The `demo/` folder convention and how to add a demo |
| [Writing Documentation](doc/user/contribute/documentation.md) | House style for these pages |

## Also

- 🛒️ [Market product page](smart-workflow-product/README.md) — what ships, the demos, and provider setup
- 🔧 [Developer docs](doc/dev/README.md) — dev container, testing and releasing, for working *on* Smart Workflow
