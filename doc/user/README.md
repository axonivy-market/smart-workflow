# Smart Workflow Documentation

Building AI-assisted processes with Smart Workflow.

**New here?** [Getting Started](getting-started.md) takes you from installation to a working agent in about fifteen minutes. [Concepts](concepts.md) explains the vocabulary and the limits worth knowing early.

## Build

Everything you need to make an agent do something.

| Guide | What it covers |
| --- | --- |
| [Agent Setup](build/agent-setup.md) | The `AgenticProcessCall` element — messages, output, structured output, and the failure modes worth knowing |
| [Model Providers](build/providers.md) | Choosing and configuring a provider, globally or per agent |
| [Defining Tools](build/tools.md) | Callable process tools, Java tools, and the built-in `webSearch` |
| [File Extraction](build/file-extraction.md) | Passing images and PDFs to a model |
| [RAG](build/rag.md) | Grounding answers in your own documents via OpenSearch |
| [Human in the Loop](build/human-in-the-loop.md) | Suspending an agent for a human decision, then resuming it |
| [Agent Patterns](build/patterns.md) | Arranging several agents and tools in one application |

## Operate

Running agents safely, and knowing what they did.

| Guide | What it covers |
| --- | --- |
| [Guardrails](operate/guardrails.md) | Prompt-injection defence, sensitive-data blocking, and PII masking |
| [Circuit Breaker](operate/circuit-breaker.md) | The application-wide switch that stops all AI calls |
| [Observability](operate/observability.md) | Arize Phoenix tracing, Ivy conversation history, and AI-assisted custom fields |
| [Security and Data](operate/security-and-data.md) | What leaves your network, what is stored, and for how long |

## Reference

Look-up tables. Each fact lives here once; the guides link to it.

| Page | What it covers |
| --- | --- |
| [Provider Capabilities](reference/capabilities.md) | Which provider supports images, PDFs, structured output and embedding |
| [Variables](reference/variables.md) | Every `AI.*` setting, with defaults and secrets marked |
| [Error Codes](reference/error-codes.md) | Every BPM error — and every failure that raises none |
| [Java API](reference/java-api.md) | The public Java surface: tools, guardrails, providers, `DecisionMaker` |

## Contribute

Extending Smart Workflow itself.

| Guide | What it covers |
| --- | --- |
| [Chat Models](contribute/models.md) | Adding a new model provider |
| [Custom Guardrails](contribute/guardrails-spi.md) | Implementing and registering your own guardrail |
| [Demo Projects](contribute/demo.md) | The `demo/` folder convention and how to add a demo |
| [Writing Documentation](contribute/documentation.md) | House style for these pages |
