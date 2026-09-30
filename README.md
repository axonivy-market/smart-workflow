# Smart Workflow 🪄️

[![CI Build](https://github.com/axonivy-market/smart-workflow/actions/workflows/ci.yml/badge.svg)](https://github.com/axonivy-market/smart-workflow/actions/workflows/ci.yml)

Let AI Agent elements drive your dynamic processes.

Smart Workflow adds an **AI agent** to Axon Ivy as a process element. Give it instructions, a set of your own callable sub-processes as tools, and a model — it reads natural language, decides what to call, and writes a typed Java object back into your process data. It connects to several model providers, from hosted APIs to a model running on your own hardware.

**New here?** [Getting Started](doc/user/getting-started.md) takes you from installation to a working agent in about fifteen minutes. [Concepts](doc/user/concepts.md) explains the vocabulary and the limits worth knowing early. If something is not working, start from [Troubleshooting](doc/user/troubleshooting.md).

## Build

Everything you need to make an agent do something.

| Guide | What it covers |
| --- | --- |
| [Agent Setup](doc/user/agent-setup.md) | The `AgenticProcessCall` element — messages, output, structured output, and the failure modes worth knowing |
| [Model Providers](doc/user/providers.md) | Choosing and configuring a provider, globally or per agent |
| [Defining Tools](doc/user/tools.md) | Callable process tools, Java tools, and the built-in `webSearch` |
| [File Extraction](doc/user/file-extraction.md) | Passing images and PDFs to a model |
| [RAG](doc/user/rag.md) | Grounding answers in your own documents via OpenSearch |
| [Human in the Loop](doc/user/human-in-the-loop.md) | Suspending an agent for a human decision, then resuming it |
| [Agent Patterns](doc/user/patterns.md) | Arranging several agents and tools in one application |

## Operate

Running agents safely, and knowing what they did.

| Guide | What it covers |
| --- | --- |
| [Guardrails](doc/user/guardrails.md) | Prompt-injection defence, sensitive-data blocking, and PII masking |
| [Circuit Breaker](doc/user/circuit-breaker.md) | The application-wide switch that stops all AI calls |
| [Observability](doc/user/observability.md) | Arize Phoenix tracing, Ivy conversation history, and AI-assisted custom fields |

## Reference

Look-up tables. Each fact lives here once; the guides link to it.

| Page | What it covers |
| --- | --- |
| [Provider Capabilities](doc/user/reference/capabilities.md) | Which provider supports images, PDFs, structured output and embedding |
| [Variables](doc/user/reference/variables.md) | Every `AI.*` setting, with defaults and secrets marked |
| [Error Codes](doc/user/reference/error-codes.md) | Every BPM error — and every failure that raises none |

## Contribute

Extending Smart Workflow itself. These live in [doc/dev/](doc/dev/README.md), alongside the dev container, testing and release guides.

| Guide | What it covers |
| --- | --- |
| [Adding a model provider](doc/dev/EXTENDING.md#adding-a-model-provider) | Adding support for a new model provider |
| [Writing a custom guardrail](doc/dev/EXTENDING.md#writing-a-custom-guardrail) | Implementing and registering your own guardrail |
| [Writing a Java tool](doc/dev/EXTENDING.md#writing-a-java-tool) | A tool as a Java class rather than a callable sub-process |
| [Demo Projects](doc/dev/DEMOS.md) | The `demo/` folder convention and how to add a demo |
| [Writing Documentation](doc/dev/DOCUMENTATION.md) | House style for these pages |

## Also

- 🛒️ [Market product page](smart-workflow-product/README.md) — what ships, and the demos
- 🔧 [Developer docs](doc/dev/README.md) — dev container, testing and releasing, for working *on* Smart Workflow
