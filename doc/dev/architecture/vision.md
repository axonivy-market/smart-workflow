# 🪄 Smart Workflow Vision

## 🧭 Vision

Make AI a native, composable capability of Axon Ivy processes. A workflow can use an agent to interpret unstructured language, call relevant tools, and return structured process data, while Ivy remains responsible for orchestration, business rules, and human work.

## 🎯 Goals

- Make an agent available as a process element that Ivy developers can add and configure in Designer.
- Let each application bring its own model provider, with provider and model choices configurable per agent.
- Connect agent decisions to existing process capabilities: callable subprocesses as the preferred tools, Java tools for logic without workflow steps, and typed results mapped to process data.
- Keep consequential work inside the process, where validation, authorization, error handling, and human approval can be explicit.
- Use Axon Ivy configuration and operational facilities, including application variables, logs, tracing, and monitoring.


## 🧩 Principles

- **🔄 Process first:** Reuse workflow orchestration for controlled decisions, extending it with AI's unstructured-language capabilities. Prefer callable subprocesses for workflow-based tools and Java tools for standalone logic.
- **🎛️ Bound agent authority:** Give each agent a clear goal, only the tools needed for that step, and typed results where process logic depends on them. Keep validation and authorization in the process, and handle required human decisions through process tasks; a prompt is not an approval control, and chat is not a workflow handoff.
- **📦 Evolve as a Market extension:** Keep Smart Workflow outside Ivy core but build on its extension points, delivering a native-feeling workflow capability with room for rapid iteration and independent, frequent releases.
- **🌐 Stay provider-neutral and adaptable:** Connect to external hosted or self-managed models through provider integrations. Do not build or operate Smart Workflow's own models, and do not tie the architecture to a single AI vendor.
- **📡 Operate through the platform:** Reuse Ivy variables and Engine Cockpit for configuration, and platform logging, tracing, and monitoring for operations.

## ✨ Expected impact

- **For process developers:** Add language understanding and agent-driven tool selection to existing workflows without standing up a separate agent service. Reuse familiar process steps and data mappings.
- **For users:** Provide requests in natural language where that is useful, while still receiving process-managed outcomes, clear tasks, and structured follow-up.
- **For operators:** Configure providers centrally and inspect AI activity through the platform's operational facilities. Application-level observability can reveal model and tool execution, while trace content and retention still require deliberate governance.
- **For organizations:** Combine flexible model behavior with deterministic process controls. AI can prepare or recommend an action; the process remains the authority for validation, authorization, and required approval.

These are intended effects, not measured guarantees. Model capabilities, provider behavior, and operational costs vary.

## 🚧 Boundaries

Smart Workflow does not provide its own model, make conversational UI mandatory, or make model output inherently trustworthy. RAG and Portal integration are optional. The supplier demo contains a reusable chat component; confirm its packaging before describing it as available from the component shelf.

## 🔗 Related documents

- [Architecture diagrams](integration.md)
- [Architecture decisions](decisions.md)
- [Concepts](../../user/concepts.md)
- [Defining tools](../../user/tools.md)
- [Human in the loop](../../user/human-in-the-loop.md)
- [Observability](../../user/observability.md)
