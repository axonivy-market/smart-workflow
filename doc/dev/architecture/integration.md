# Smart Workflow Integration

Smart Workflow is a harness for the Axon Ivy Platform.
It reuses the platform's capabilities and integrates through its extension points.

### Platform integration points

- 🛍️ Discover:
  - Market: Find, install, and promote solutions for Axon Ivy.
  - Process Editor: Installed agent steps appear as ready-to-use components in the extension drawer.
- 🗒️ Configure:
  - Variables: Configure development- and runtime-specific settings in the Variables Editor and Engine Cockpit.
  - Manage global defaults with per-application and per-agent-element overrides.
- 🔀 Collaborate (workflow):
  - Call existing processes as tools.
  - Map unstructured input to structured process data.
  - Involve people through tasks.
  - Integrate with Portal's optional AI service provider.
- ⏯️ Execute:
  - Runtime logs: Quickly access REST traffic to LLM providers.
  - REST monitoring: Detect slow requests and trace agent and tool execution.
  - Business Data repository: Store runtime memory and opt-in conversation history.
- ♻️ Maintain:
  - CI/CD pipelines on axonivy-market.org.
  - Releases in GitHub Packages.
  - Dependency management with Renovate.
  - Changelog generation with Release Drafter.

## 1. Platform Integration Map

Show how AI capabilities are added through Ivy's process extension points and how they use native workflow and operational facilities. Keep the Smart Workflow harness inside its own boundary so the platform integration is visible.

```mermaid
flowchart LR
  subgraph Ivy["Axon Ivy platform"]
    Process["Ivy business process"]
    AgentStep["AgenticProcessCall<br/>process-program extension"]
    Subprocess["Callable subprocesses<br/>including human tasks"]
    Rest["Ivy REST-client integration<br/>(provider-dependent)"]
    Ops["Ivy logs, traces, monitoring"]
    Cockpit["Engine Cockpit<br/>application variable values"]
  end

  subgraph SmartWorkflow["Smart Workflow harness"]
    Runtime["Agent runtime<br/>LangChain4j"]
    Constraints["Instructions, typed results,<br/>guardrails, memory"]
    ToolFilter["Configured tool allow-list"]
    IvyTools["Ivy subprocess tool provider"]
    JavaTools["Smart Workflow Java tools<br/>SPI"]
    WebSearch["Built-in webSearch tool"]
    RagTool["Optional OpenSearch RAG tool"]
    ModelSpi["ChatModelProvider SPI<br/>provider modules"]
    Listeners["AI listeners<br/>history, custom fields, tracing"]
  end

  VariableDefs["AI variables declared in variables.yaml<br/>core defaults and provider settings"]
  Model["Selected model provider"]
  Web[("Public web<br/>data source")]
  OpenSearch[("OpenSearch vector store<br/>data container")]

  Process --> AgentStep --> Runtime
  Runtime --> Constraints
  Runtime --> ToolFilter
  ToolFilter --> IvyTools --> Subprocess
  ToolFilter --> JavaTools
  ToolFilter --> WebSearch --> Web
  JavaTools -. "optional tool contribution" .-> RagTool --> OpenSearch
  Runtime --> ModelSpi --> Model
  ModelSpi -. "where the connector uses Ivy transport" .-> Rest
  Runtime -. "AI events" .-> Listeners -. "trace integration" .-> Ops
  AgentStep -. "process execution and errors" .-> Ops
  VariableDefs --> Cockpit
  Cockpit -- "current values read on each agent call" --> Runtime
```

## 2. Platform Interfaces and Extension Points

Smart Workflow integrates with the Axon Ivy Platform through its extension points and can itself be customized and extended to meet customer needs.

```mermaid
flowchart LR
  subgraph Platform["Axon Ivy platform interfaces used"]
    ProcessExt["Process-program extension<br/>AgenticProcessCall"]
    WorkflowApi["Workflow APIs<br/>callable subprocesses, data, tasks"]
    ConfigApi["Variables and configuration<br/>Variables Editor / Engine Cockpit"]
    BusinessDataApi["Business Data repository<br/>Ivy.repo()"]
    RestApi["REST-client integration<br/>(provider-dependent)"]
    OpsApi["Logging, tracing, monitoring"]
  end

  Core["Smart Workflow core<br/>agent runtime and adapters"]
  ChatMemory["Persistent chat memory<br/>BusinessDataMemory"]
  HistoryStorage["Opt-in conversation history<br/>IvyRepoHistoryStorage"]

  subgraph Extensions["Smart Workflow extension and integration surface"]
    ModelSpi["Model providers<br/>ChatModelProvider SPI"]
    ProcessTools["Workflow tools<br/>Ivy subprocess tool adapter"]
    JavaTools["Java tools<br/>SmartWorkflowTool + SmartWorkflowToolsProvider SPI"]
    SearchSpi["Web-search engines<br/>SmartWebSearchEngineProvider SPI"]
    PortalBridge["Portal AI bridge<br/>Portal/SmartWorkflowAgent callable process"]
  end

  ProviderModules["Provider modules"]
  ToolModules["Java tool modules"]
  SearchModules["Search-engine modules"]

  Portal["👱️ Axon Ivy Portal<br/>opt-in integration"]
  EngineCockpit["🧑‍💻️ Engine Cockpit<br/>operations and configuration"]

  ProcessExt --> Core
  WorkflowApi --> Core
  ConfigApi --> Core
  Core --> ChatMemory --> BusinessDataApi
  Core --> HistoryStorage --> BusinessDataApi
  RestApi --> Core
  OpsApi --> Core
  Core --> ModelSpi --> ProviderModules
  Core --> ProcessTools --> WorkflowApi
  Core --> JavaTools --> ToolModules
  Core --> SearchSpi --> SearchModules
  Portal --> PortalBridge --> Core
  EngineCockpit --> OpsApi
  EngineCockpit --> RestApi
  EngineCockpit --> ConfigApi
```

## 3. Agent Runtime and Human Handoff

Show one agent call from the process perspective. The configured goal, output type, and tool allow-list bound the step; the process receives the result and decides what happens next. A human decision is a process-managed pause/resume, not a requirement that every solution use chat.

```mermaid
sequenceDiagram
  participant P as Ivy process
  participant A as AgenticProcessCall
  participant H as Smart Workflow runtime
  participant M as Provider adapter / model
  participant T as Tool provider
  participant W as Ivy subprocess or task
  participant U as Human
  participant O as Ivy tracing / monitoring

  P->>A: Invoke with goal, allowed tools, expected result type
  A->>H: Execute agent step
  Note over H: Read current AI.* Ivy variable values for this call
  H-->>O: Emit agent, model, tool, and guardrail events
  H->>M: Send request
  M-->>H: Response or requested tool call

  loop While the model requests an allowed tool
    alt Ordinary workflow or Java tool
      H->>T: Dispatch selected tool
      T->>W: Invoke callable subprocess or tool implementation
      W-->>T: Return tool result
      T-->>H: Return tool result
      H->>M: Continue agent conversation
      M-->>H: Response or another tool call
    else Tool requests a human decision
      H->>P: Raise human-decision handoff with decision options
      P->>W: Create and wait for Ivy user task
      W->>U: Present decision task
      U-->>W: Submit decision
      W-->>P: Return selected decision
      P->>A: Resume agent with persisted conversation memory
      A->>H: Continue agent step
      H->>M: Continue agent conversation
      M-->>H: Final response
    end
  end

  H-->>A: Return text or typed result
  A-->>P: Map result into process data
```

## 4. Market Installation and Runtime Availability

Keep installation separate from runtime execution. The Market product manifest resolves the core project and provider/RAG projects; once installed as project dependencies, their SPI registrations make the extensions available to agent calls. This packaging lets Smart Workflow release independently of Ivy core while integrating into workflows as a native-feeling capability.

```mermaid
flowchart LR
  Market["Smart Workflow Market product"] --> Installer["Maven import / dependency installer"]

  subgraph Resolved["Resolved Ivy projects"]
    Core["smart-workflow core IAR"]
    Provider["At least one model-provider IAR"]
    MoreProviders["Additional provider IARs<br/>(optional)"]
    Rag["OpenSearch RAG IAR<br/>(optional)"]
    Demos["Demo/application IARs<br/>(imported separately)"]
    VariableDefs["variables.yaml declarations<br/>core and provider settings"]
  end

  Installer --> Core
  Installer --> Provider
  Installer -. optional .-> MoreProviders
  Installer -. optional .-> Rag
  Installer -. import .-> Demos

  subgraph Engine["Ivy engine / application runtime"]
    Agent["AgenticProcessCall"]
    Discovery["SPI extension discovery"]
    Cockpit["Engine Cockpit<br/>application variable values"]
  end

  Core --> Agent
  Provider --> Discovery
  MoreProviders -.-> Discovery
  Rag -.-> Discovery
  Discovery --> Agent
  Core --> VariableDefs
  Provider --> VariableDefs
  VariableDefs -- "declare available settings and defaults" --> Cockpit
  Cockpit -- "updated values; read on each call" --> Agent
```

## 5. Observability and Governance

Smart Workflow offers separate channels for inspecting traces and reviewing conversation history:

1. Engine Cockpit provides a basic view of Ivy traces.
2. Arize Phoenix provides a richer, opt-in OpenInference trace viewer.
3. The Governance Dashboard displays recorded history and conversations. Analytics and reports are WIP in [PR #355](https://github.com/axonivy-market/smart-workflow/pull/355).

```mermaid
flowchart LR
  Agent["⏩️ Smart Workflow agent execution"]

  subgraph Ivy["Axon Ivy Engine"]
    Trace["✍️ Ivy tracing and monitoring"]
    Cockpit["🧑‍💻️ Engine Cockpit<br/>basic trace viewer<br/>and variable configuration"]
    Marker["☑️ AI-assisted custom field<br/>Case / Task"]
    History["📼️ Conversation history recorder"]
    Repository[("Ivy repository<br/>OpenSearch BusinessData")]
    Dashboard["📊️ Governance dashboard<br/>history / conversation viewer"]
    Portal["👱️ Axon Ivy Portal<br/>Case and Task lists"]
  end

  Phoenix["💵️ Arize Phoenix<br/>OpenInference trace viewer"]

  Agent -->|"agent, model, tool, guardrail spans"| Trace
  Trace --> Cockpit
  Trace -. "optional OTLP export" .-> Phoenix
  Cockpit -. "configure observability variables" .-> Agent
  Agent -->|"default on"| Marker
  Marker -->|"AI-assisted state shown in lists"| Portal
  Agent -->|"opt in"| History
  History --> Repository --> Dashboard
```

## Implementation Anchors

- [`AgenticProcessCall`](../../../smart-workflow/src/com/axonivy/utils/smart/workflow/AgenticProcessCall.java) is the Ivy process-program extension.
- [`AgentCallExecutor`](../../../smart-workflow/src/com/axonivy/utils/smart/workflow/program/internal/AgentCallExecutor.java) wires model selection, structured results, tools, guardrails, memory, and listeners.
- [`IvySubProcessToolsProvider`](../../../smart-workflow/src/com/axonivy/utils/smart/workflow/tools/provider/IvySubProcessToolsProvider.java) exposes callable Ivy subprocesses as tools.
- [`OpenSearchRagToolProvider`](../../../rag/smart-workflow-opensearch-rag/src/com/axonivy/utils/smart/workflow/rag/tool/OpenSearchRagToolProvider.java) contributes the optional RAG tool.
- [`product.json`](../../../smart-workflow-product/product.json) declares the Market import and dependency sets.
- [`variables.yaml`](../../../smart-workflow/config/variables.yaml) declares shared AI defaults; provider projects declare their own settings.
- [Variable reference](../../user/reference/variables.md) documents Engine Cockpit configuration and per-call value resolution.
- [Extending Smart Workflow](../EXTENDING.md) describes provider and tool SPI registration.
