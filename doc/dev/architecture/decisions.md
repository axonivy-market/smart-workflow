# 🏗️ Architecture Decisions

Short-form records of important decisions made while building Smart Workflow.

## ADR-001: Integrate AI as a process capability

- **Context:** AI must participate in business workflows without displacing Ivy's process engine or its human tasks.
- **Decision:** Expose the agent through an Ivy process-program extension and keep orchestration, data mapping, and downstream actions in the process.
- **Consequences:** Developers can place agent calls in existing process flows and use normal Ivy subprocesses and tasks. The workflow owns the business lifecycle, not a separate long-running agent service.

## ADR-002: Connect to model providers; do not operate our own models

- **Context:** Applications have different provider, deployment, and model requirements; the architecture should not promote one AI vendor.
- **Decision:** Integrate hosted and self-managed models through provider adapters. Keep provider selection extensible through `ChatModelProvider` rather than building or operating Smart Workflow-owned models.
- **Consequences:** Smart Workflow stays independent of any one provider and does not proxy or resell model services; users manage their own provider plans and billing. Provider adapters also make local model runtimes possible, preserving deployment choice.

## ADR-003: Ship Smart Workflow as a Market extension

- **Context:** AI capabilities and providers evolve quickly. Smart Workflow should feel native in Ivy workflows without requiring every change to ship with Ivy core.
- **Decision:** Keep Smart Workflow as a separately packaged and versioned Axon Ivy Market extension, built on Ivy core APIs and extension points.
- **Consequences:** Independent releases broaden contributions across team boundaries without waiting for Ivy core releases. Early betas can target Ivy nightlies and other leading-edge versions, letting us preview and validate features with customers before a stable release. We must communicate compatibility and beta support boundaries.

## ADR-004: Build on LangChain4j and Java

- **Context:** AI harnesses evolve quickly. [LangChain4j](https://docs.langchain4j.dev/) is open source, has strong momentum, and an active community discussing emerging AI patterns and capabilities. Our contributions show that the community welcomes outside participation.
- **Decision:** Build the Java harness on LangChain4j and contribute to its evolution rather than maintain a bespoke runtime or implement one in Python.
- **Consequences:** Smart Workflow benefits from community-driven progress and established Java and Ivy platform practices, while accepting a dependency on LangChain4j's APIs and release direction.

## ADR-005: Processes First

- **Context:** Ivy users already design processes. Reusing agent activities and callable subprocesses as tools lets them build nondeterministic agent workflows with familiar concepts and a minimal learning curve.
- **Decision:** Define agents and prompts on process activities, not in text files. Prefer callable subprocesses as tools. Keep Java tools exceptional: exposing parameters and adding tool annotations bring substantially more technical complexity.
- **Consequences:** Users can build agent workflows with familiar design concepts. Java tools remain possible where subprocesses do not fit, but require more specialized implementation.

## ADR-006: Keep LangChain4j behind stable extension APIs

- **Context:** LangChain4j APIs evolve quickly and can introduce incompatible changes. Exposing them directly would pass that churn and compatibility burden to Java extension authors.
- **Decision:** Keep LangChain4j as an implementation detail. Expose Smart Workflow-owned Java SPIs and adapters for guardrails, Java tools, and web-search engines.
- **Consequences:** Extension authors target Smart Workflow contracts instead of upstream APIs; Smart Workflow absorbs upstream changes behind those adapters and manages compatibility of its own extension APIs.


## Sources

- [Vision](vision.md)
- [Architecture diagrams](integration.md)
- [Extending Smart Workflow](../EXTENDING.md)
- [Concepts](../../user/concepts.md)
- [Model providers](../../user/providers.md)
- [Defining tools](../../user/tools.md)
- [Human in the loop](../../user/human-in-the-loop.md)
- [Observability](../../user/observability.md)
