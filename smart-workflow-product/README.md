# Smart Workflow

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

## Demo

### Axon Ivy Support Agent Demo

This demo showcases how to use the Axon Ivy Support Agent, an AI-powered agent integrated into a business workflow. The agent is designed to classify support problems, check for missing information, and create support tasks automatically.

**Workflow Overview:**

1. **Input:** The agent receives a support question and the username of the reporter.
2. **Classification:** It analyzes the problem, determines if information is missing (such as version), and classifies the issue (Portal, Core, or Market product).
3. **Task Creation:** If necessary, the agent creates a support task using the `createAxonIvySupportTask` tool and provides a link to the created task.
4. **Summary & Response:** The agent summarizes the problem and replies to the user with a detailed response.

**Technical Details:**

- The agent is implemented as a callable sub-process (`AxonIvySupportAgent.p.json`) and uses the `com.axonivy.utils.smart.workflow.AgenticProcessCall` Java bean.
- The agent is configured to use a specific tool (`createAxonIvySupportTask`), which allows it to create support tasks automatically within the workflow. This is achieved by specifying the tool name in the agent's configuration (see example below).
- The agent's output is mapped to a structured Java object (`AxonIvySupportResponse`), making it easy to use the AI-generated result directly in Axon Ivy processes. This object typically contains details such as the classification, created task link, and a summary of the support issue.

**Agent Configuration Example:**

To configure the agent, define a program element with the following settings:

![Support Ticket example](img/support-ticket-example.png)

This configuration ensures the agent uses only the specified tool and returns its output as a structured Java object.

**Demo Run Example:**

Suppose a user submits a support question: "I have NPE when open Case Details in Portal 12.0.9"

1. The agent receives the question and username.
2. It checks for missing information (e.g., version), classifies the issue as a Portal problem, and determines that a support task should be created.
3. The agent calls the `createAxonIvySupportTask` tool, which creates a new support task and returns a link to it.
4. The agent summarizes the problem and provides a response such as:

```text
Classification: Portal
Summary: The problem is a NullPointerException (NPE) occurring when opening Case Details in Portal version 12.0.9. Since the issue is related to the Portal product and the version is provided, a support task has been created to address this problem.
```

This response is mapped to the `AxonIvySupportResponse` object and can be used directly in subsequent workflow steps.

How to Run the Demo:

1. Ensure you have completed the [Configurations](#configurations) section.
2. Trigger the Axon Ivy Support Agent process with a support question and username.
3. Review the agent's response, which includes classification, task creation (if needed), and a summary.

### Shopping Demo

This demo showcases how AI can transform the operations of a small e-commerce fashion store. It’s more advanced and combines two mini-demos: one on product creation and another on semantic search. Because of its complexity, we won’t dive into the detailed code or step-by-step instructions here. If you’d like to explore the implementation, please check out the demo project `smart-workflow-demo`.

**Product creation**

Traditionally, adding a product requires the store operator to manually fill many fields and to validate or create dependent records (supplier, brand, category). For a small store this process can take hours or a full day: manual data entry, hunting for missing info, and re-checking for mistakes.

With Smart Workflow agents, the operator simply imports the product specification and image files. The agents handle parsing, validation, dependency resolution, and product creation — significantly reducing manual work and time-to-publish.

Developers need to create four agents

1. Product agent

- Input: parsed product specification
- Tools:
  - Find product: Find product in the system
  - Create product: Create a new product using the provided specification
  - Check product dependencies: Call other agents to find and validate dependencies (supplier, brand, and category)

2. Supplier agent

- Input: supplier information
- Tools:
  - Find supplier: Find supplier in the system
  - Create supplier: Create a new supplier using the provided information

3. Category agent

- Input: product category information
- Tools:
  - Find category: Find category in the system
  - Create category: Create a new category using the provided information

4. Brand agent

- Input: product brand information
- Tools:
  - Find brand: Find brand in the system
  - Create brand: Create a new brand using the provided information

Demo flow

1. Operator uploads product specification and image files.
2. Smart Workflow parses the files, extracts product attributes (title, SKU, description, price, supplier info, brand, category, images).
3. Validators check semantics and constraints (required fields, formats, SKU uniqueness, image requirements).
4. For each dependency (supplier, brand, category), Smart Workflow asks the appropriate agent:
  if the entity exists → return the ID,
  if missing → create it using the provided spec.
5. Product agent creates the product with validated attributes and links to dependency IDs.
6. System returns a summary and optionally opens a human-review screen with prefilled fields for final approval.

The new AI-powered process resulted in fewer errors, far less manual work, and a much faster time-to-publish.

**Semantic search**

Before AI, shoppers typed keyword queries like “red dress,” then manually applied filters (price, brand, category) and scanned the results. This process was not only slow and rigid but also often failed to capture synonyms, styles, or intent (e.g., party vs. work).

With semantic search the user speaks or types a natural request. AI understands intent and constraints (color, price, occasion, urgency), converts that into a structured criteria object. The backend then converts that object into SQL predicates and returns matched results. Offers clear explanations, familiar tooling, and easier deployment.

Developers need to add an additional `Find product by criteria` tool to the `Product agent` with input is the search criteria.

Demo flow

1. Shopper: types or says “I need a $100 red dress for a party tonight.”
2. `Product agent` extracts attributes and expands the query (synonyms, acceptable price range: $80–$120).
3. Axon Ivy Business Data turns criteria into an optimized filters and search for the products.
4. Return the top products matched criteria.

To quickly set up the demo data, run the process `Create data for shopping demo` from the process list.

### File Extraction Demo

This demo shows how AI agents can extract structured data directly from uploaded files — images and PDFs — using multimodal language models. It eliminates manual data entry by reading documents and producing typed Java objects ready for use in subsequent workflow steps.

**Purpose:** Demonstrate multimodal AI extraction from binary content (images, PDFs) without any manual data parsing.

**Workflow Overview:**

1. **Input:** The demo prepares file content — either from the Axon Ivy CMS or from a binary stream loaded at runtime.
2. **Extraction:** The file content is embedded in the agent's query; the AI model reads and extracts the relevant fields.
3. **Output:** The result is mapped to a typed Java object (e.g., an invoice or receipt data class) for immediate use downstream.

**Technical Details:**

- Implemented in `processes/Features/FileExtractionDemo.p.json` using a `ProgramInterface` element backed by `com.axonivy.utils.smart.workflow.AgenticProcessCall`.
- Two variants are provided: `extractFromCMS` — loads files via `ivy.cm.ref(...)`, and `extractFromBinary` — loads files as `InputStream` / `Binary` from the filesystem at runtime.
- The file content is passed inline inside the agent `query`; no file system tooling is required.
- The `resultType` is set to the target Java class so the agent returns a fully populated structured object.
- Not all providers support multimodal input — see the [Models Contribution Guideline](../doc/MODELS.md#file-extraction-support) for the list of supported providers.

---

### Guardrail Demo

This demo shows how built-in Smart Workflow guardrails protect AI agents from prompt injection attacks and prevent sensitive data from leaking in AI responses. It demonstrates both input and output guardrails in action with real attack scenarios.

**Purpose:** Show how to harden AI agents against common security threats using Smart Workflow's built-in guardrail infrastructure without writing custom code.

**Workflow Overview:**

1. **Prompt Injection variant:** A crafted malicious message is submitted to the agent. The `PromptInjectionInputGuardrail` intercepts it _before_ the AI model is called and raises an error that the process handles via an `ErrorBoundaryEvent`.
2. **Sensitive Data variant:** The agent is prompted to include sensitive content in its response. The `SensitiveDataOutputGuardrail` intercepts the response _after_ the AI model returns it and blocks the output before it reaches the process.
3. **Error Handling:** Both variants demonstrate catching the guardrail violation with a boundary event and routing to a safe error path.

**Technical Details:**

- Implemented in `processes/Features/GuardrailDemo.p.json`.
- Uses built-in guardrails `PromptInjectionInputGuardrail` and `SensitiveDataOutputGuardrail` configured in the agent's `inputGuardrails` / `outputGuardrails` fields.
- Error paths are wired via `ErrorBoundaryEvent` attached to the `ProgramInterface` element.
- Default guardrails can be set globally in `variables.yaml` under `AI.Guardrails.DefaultInput` and `AI.Guardrails.DefaultOutput` — any agent that does not specify guardrails explicitly will inherit these defaults.

---

### Custom Guardrail Demo

This demo shows how to implement and register a domain-specific business rule as a reusable custom guardrail using the Smart Workflow SPI. Once registered, the guardrail applies automatically to all agents without touching individual prompts.

**Purpose:** Demonstrate the `GuardrailProvider` SPI pattern for encoding company-wide policies (e.g., never mention a competitor) as a reusable, centrally managed guardrail.

**Workflow Overview:**

1. **Blocked variant:** The agent receives a query that mentions a competitor (Camunda). The custom `BlockCompetitorMentionGuardrail` detects this at input time and blocks the request before the AI model is called.
2. **Allowed variant:** The agent receives a neutral business query. The guardrail passes the input through, and the agent responds normally.
3. Both variants illustrate that the guardrail logic lives in one place and is applied transparently.

**Technical Details:**

- Implemented in `processes/Features/CustomGuardrailDemo.p.json` with two `ProgramInterface` elements, each specifying `inputGuardrails: ["BlockCompetitorMentionGuardrail"]`.
- The guardrail class `BlockCompetitorMentionGuardrail` implements the `SmartWorkflowInputGuardrail` SPI.
- Registration is done via `META-INF/services/com.axonivy.utils.smart.workflow.guardrails.spi.SmartWorkflowInputGuardrail`.
- Once registered, the guardrail name appears in the designer's Available Input Guardrails list automatically.

---

### Smart Workflow Agent Demo

This demo shows how to invoke a Smart Workflow AI agent through a reusable callable subprocess, separating the AI logic from the calling process and making it independently deployable.

**Purpose:** Demonstrate the callable subprocess pattern for encapsulating AI agent logic — enabling reuse, independent testing, and clean separation from business process orchestration.

**Workflow Overview:**

1. **Preparation:** The calling process prepares sample input (e.g., invoice text) and passes it via parameters to a `SubProcessCall`.
2. **Callable execution:** The callable subprocess (`CALLABLE_SUB`) runs the `ProgramInterface` agent, processes the input, and returns a typed result.
3. **Result usage:** The calling process receives the structured result object and continues its workflow.

**Technical Details:**

- Implemented in `processes/Features/SmartWorkflowAgentDemo.p.json` as the entry point and a separate `CALLABLE_SUB` containing the `ProgramInterface`.
- The calling process uses a standard `SubProcessCall` element — no agent-specific API is needed in the caller.
- The callable `CALLABLE_SUB` encapsulates the `ProgramInterface`, system message, model selection, and output mapping.
- This pattern allows multiple different calling processes to share the same AI callable without duplicating agent configuration.

---

### Meeting Minutes Demo

This demo shows an AI-assisted multi-step pipeline where AI extracts structured action items from a meeting transcript, a human reviews and edits the list, and Axon Ivy tasks are automatically created per action item.

**Purpose:** Demonstrate a complete human-in-the-loop AI workflow — AI produces initial output, a human validates it, and the confirmed items drive downstream process automation.

**Workflow Overview:**

1. **Input:** The user pastes a meeting transcript and optionally selects a target language for translation.
2. **AI Extraction:** A `TaskSwitchEvent` triggers the AI to extract a structured `FeedbackList` from the transcript — each item has a title, description, assignee, and priority.
3. **Optional Translation:** A second agent step can translate all item text to the selected language.
4. **Human Review:** The extracted list is presented in a task form where the reviewer can add, edit, or remove items.
5. **Task Fan-out:** For each confirmed feedback item, a `TriggerCall` fires a separate Ivy task assigned to the named team member.

**Technical Details:**

- Implemented in `processes/Business/MeetingMinutesDemo/MeetingMinutesDemo.p.json`.
- Uses `TaskSwitchEvent` for the AI extraction step so the agent output is observable as an Ivy task before the human review starts.
- The human review dialog is invoked via `DialogCall` using the `FeedbackReview` HTML dialog.
- `TriggerCall` fans out one task per feedback item using the `createFeedbackTask(String, String, String)` callable signature.
- Model classes (`FeedbackList`, `FeedbackItem`) are annotated with `@Description` for reliable AI extraction.

---

### Job Application Screening Demo

This demo automates the initial CV screening step of HR recruitment using a two-stage AI pipeline followed by a human review step — before any communication is sent to the candidate.

**Purpose:** Demonstrate a multi-stage AI pipeline with structured extraction and scoring, where each stage produces a typed object, and a human finalises the output before it leaves the system.

**Workflow Overview:**

1. **Input:** HR enters the CV text and job description (pre-filled with a sample Java developer CV and job spec).
2. **Profile Extraction:** A `TaskSwitchEvent` triggers the first AI agent to extract a `CandidateProfile` — name, email, skills, years of experience, and education level.
3. **Fit Scoring:** A second `TaskSwitchEvent` runs the scoring agent against both the profile and the job description, producing a `FitScore` — a 0–100 score, a qualified flag, strengths, gaps, and a hiring recommendation.
4. **Communication Draft:** A script builds a personalised accept or reject communication from the structured scoring output.
5. **Human Review:** The recruiter reviews and optionally edits the draft in a task form before it is finalised.

**Technical Details:**

- Implemented in `processes/Business/JobApplicationScreeningDemo/JobApplicationScreening.p.json`.
- Two independent `TaskSwitchEvent` steps keep extraction and scoring separately observable and re-runnable.
- `CandidateProfile` and `FitScore` model classes use `@Description` annotations on each field to guide reliable AI extraction.
- Null-safe expressions (`<%=in.field != null ? in.field : ""` patterns) are required in query templates — AI may return `null` for optional fields.
- The human review dialog is `ReviewCommunication`, invoked via `DialogCall`.

---

### Medical Report Analyzer Demo

This demo shows how to build a reusable AI callable that delegates work to three specialist tool subprocesses — an orchestrator agent that decides which tool to call and in what order, rather than hardwiring the sequence in the process diagram.

**Purpose:** Demonstrate the agent-with-tools-in-callable pattern, where a single `CALLABLE_SUB` hides all AI orchestration behind a clean interface, making it independently deployable and testable.

**Workflow Overview:**

1. **Input:** The user enters a medical report text (pre-filled with a sample patient report).
2. **Orchestrator invocation:** A `SubProcessCall` invokes the `MedicalReportAnalyzer:analyze` callable, passing the report text.
3. **Tool dispatch (inside the callable):** The orchestrator `ProgramInterface` agent autonomously calls three specialist tools:
   - `extractVitals` — extracts blood pressure, heart rate, BMI, and oxygen saturation.
   - `identifyAbnormalities` — flags out-of-range values and urgent clinical findings.
   - `generateSummary` — produces a concise clinical summary for physician review.
4. **Result display:** The compiled analysis result is shown in a read-only result dialog.

**Technical Details:**

- Two process files: `MedicalReportAnalyzer.p.json` (`CALLABLE_SUB`) contains the orchestrator and three tool `CallSubStart` elements; `MedicalReportCaller.p.json` (`NORMAL`) is the user-facing entry point.
- Each tool `CallSubStart` is tagged with `"tags": ["tool"]` — without this the framework does not register it as an AI-invocable tool.
- The orchestrator `ProgramInterface` specifies `tools: ["extractVitals", "identifyAbnormalities", "generateSummary"]` explicitly; the three tool agents specify `tools: "[]"` (no nested tools).
- The callable can be deployed independently and invoked by any process that needs medical report analysis, without exposing internal AI wiring.

---

### Support Agent Demo (Callable Multi-Agent)

This demo shows how to orchestrate multiple AI agents within a formal Axon Ivy approval workflow — each agent handling one focused responsibility inside its own callable subprocess.

**Purpose:** Demonstrate multi-agent composition in a real business process: AI creates a ticket, selects an approver, and prepares task data — each step independently callable and testable.

**Workflow Overview:**

1. **Ticket creation:** The first AI callable receives user input and produces a structured `SupportTicket` object.
2. **Approver selection:** A second AI callable reads the ticket type and selects the appropriate approver from a policy list.
3. **Task preparation:** A third AI callable prepares HR-specific task info for the approval lane.
4. **Approval workflow:** A `TriggerCall` starts the `SupportBusiness` approval process, which runs a two-stage approval flow with `DialogCall` forms for the approvers.

**Technical Details:**

- Implemented across `processes/AgentDemo/SupportAgent.p.json` (orchestrating process) and `processes/AgentDemo/SupportBusiness.p.json` (approval back-end).
- Each AI step is a separate `CALLABLE_SUB` with its own `ProgramInterface`, typed input, and typed output — one responsibility per callable.
- Callables: `Create SupportTicket object`, `Create task`, `chooseTicketApprover(SupportTicket)`, `handleHrTicket(SupportTicket)`.
- `SupportBusiness.p.json` provides two entry points: `createSupportTicket` (routes to first approval task) and `handleHrTicket` (two-stage approval with `DialogCall` forms).

---

### Support Agent with Tools Demo

This demo shows how an AI agent autonomously decides when and how to invoke a subprocess tool — without the process designer explicitly scripting the call sequence.

**Purpose:** Demonstrate autonomous tool invocation: the agent receives a goal, decides which tool satisfies it, invokes the tool, and returns the combined result — all without branching logic in the process diagram.

**Workflow Overview:**

1. **Input:** A support problem description is submitted to the agent.
2. **Autonomous decision:** The `ProgramInterface` agent (system: "You are a Support Agent") reviews the problem and decides to call the `sumarizeProblem` tool.
3. **Tool execution:** The tool callable runs and returns a summary.
4. **Response:** The agent incorporates the tool result and returns a final response.

**Technical Details:**

- Implemented in `processes/AgentDemo/SupportAgentTools.p.json`.
- The `ProgramInterface` specifies `tools: ["sumarizeProblem"]` — only the listed tool is available to the agent.
- The tool `CallSubStart` is tagged with `"tags": ["tool"]`; its description tells the agent when to use it.
- Best practice: keep tool interfaces simple — one focused input, one typed output; let the agent handle invocation ordering.

---

### Support Agent with Planning Demo

This demo shows how an AI agent can execute a multi-step business goal expressed entirely as a natural language instruction, planning the tool call sequence autonomously without any explicit branching in the process diagram.

**Purpose:** Demonstrate goal-oriented autonomous planning — the designer states _what_ to achieve; the agent plans _how_, calling multiple tools in the correct order to reach the goal.

**Workflow Overview:**

1. **Input:** A support request is submitted.
2. **Planning:** The `ProgramInterface` agent reads the system prompt goal ("Create a support ticket then create a task to handle it") and plans the required tool invocations.
3. **Execution:** The agent calls the tools in the order it determines is correct — no explicit sequencing in the process diagram.
4. **Result:** All tool outputs are combined and the final result is returned to the caller.

**Technical Details:**

- Implemented in `processes/AgentDemo/SupportAgentToolsWithPlanning.p.json`.
- The system prompt contains the complete goal description; tool availability is declared in the `tools` field.
- Requires a planning-capable model (GPT-4.1, Claude Sonnet 4+) — weaker models may miss steps or invoke tools out of order.
- Pair with guardrails when using broad tool access; autonomous agents with goal-oriented prompts have a wider action surface than single-step agents.

## Setup

To start your AI initiative, we need to define the Models and Tools in advance.

### Models

Smart Workflow isn't bound to a specific AI vendor. 
You can select your preferred model providers at installation time.

After installation, please choose your default model provider

The selection of your provider is done with the variable `AI.DefaultProvider`. 
Furthermore, most model providers need an ApiKey or another unique identifier.
Check your provider below, to see which variables need to be set in addition.

To request support for additional AI model providers, please open an issue or submit a pull request on GitHub.
When contributing, make sure to follow the [Models Contribution Guideline](../doc/MODELS.md) to keep your provider aligned with the Smart Workflow ecosystem.

```yaml
@variables.yaml@
```

#### OpenAI Models

<details>

<summary>OpenAI setup instructions</summary>
OpenAI models are natively supported. If you wish to use them import the `smart-workflow-openai` project and define your OpenAI key.

```yaml
@variables.openai@
```
</details>

#### Azure OpenAI Models

<details>

<summary>Azure OpenAI setup instructions</summary>
Azure OpenAI models are supported. To use Azure OpenAI, import the `smart-workflow-azure-openai` project and configure your Azure OpenAI endpoint and deployments.

Each deployment in Azure OpenAI represents a model instance with its own API key. You can configure multiple deployments to use different models for different tasks.

```yaml
@variables.azureopenai@
```

Example Configuration:

```yaml
@variables.azureopenai.example@
```
</details>

#### Google Gemini Models

<details>

<summary>Google Gemini setup instructions</summary>
Google Gemini models are supported. To use Google Gemini, import the `smart-workflow-gemini` project and configure your Gemini API key and default model.
This provider does not support the structured output feature because Google Gemini models do not support structured JSON responses.

```yaml
@variables.gemini@
```

Example Configuration:

```yaml
@variables.gemini.example@
```
</details>

#### x.AI Models

<details>

<summary>x.AI setup instructions</summary>
x.AI models are supported, import the `smart-workflow-xai` to work with these.


```yaml
@variables.xai@
```

Example Configuration:

```yaml
@variables.xai.example@
```

</details>

#### Anthropic Models

<details>

<summary>Anthropic setup instructions</summary>
Claude models (including Claude Opus, Sonnet and Haiku) from Anthropic are supported. Import the `smart-workflow-anthropic` project, configure your API key to get started.

> **Note:** Structured outputs are only supported on Claude Opus 4.6, Claude Sonnet 4.6, Claude Sonnet 4.5, Claude Opus 4.5, and Claude Haiku 4.5. Older models (e.g., Claude Sonnet 4, Claude Opus 4) do not support this feature.

```yaml
@variables.anthropic@
```

Example Configuration:

```yaml
@variables.anthropic.example@
```

</details>

### File Extraction

Axon Ivy Smart Workflow supports extracting content from PDF and image files (PNG, JPG, and JPEG) using multimodal LLMs.
This allows AI agents to read and reason over uploaded documents and images directly within your workflows.

Not all providers and models support multimodal input.
Refer to the [Models Contribution Guideline](../doc/MODELS.md#file-extraction-support) for the full list of supported providers and file types.

### Guardrails

Guardrails protect AI agents by validating user input before it reaches the model and by checking model outputs before they are used. Smart Workflow includes the following built-in guardrails:

| Guardrail | Type | Description |
|-----------|------|-------------|
| `PromptInjectionInputGuardrail` | Input | Blocks common prompt injection attacks |
| `SensitiveDataOutputGuardrail` | Output | Blocks responses containing API keys or private keys |

#### Configuring Default Guardrails

Set default guardrails in `variables.yaml`:

```yaml
Variables:
  AI:
    Guardrails:
      # Comma-separated list of guardrail names
      DefaultInput: PromptInjectionInputGuardrail
      DefaultOutput: SensitiveDataOutputGuardrail
```

#### Using Guardrails in Agents

In the agent configuration, specify guardrails as a String array:

```java
// Input guardrails
["PromptInjectionInputGuardrail", "MyCustomInputGuardrail"]

// Output guardrails
["SensitiveDataOutputGuardrail", "MyCustomOutputGuardrail"]
```

If no guardrails are specified, the agent uses the default guardrails from `variables.yaml`.

Smart Workflow also lets you implement custom guardrails and handle guardrail errors. For more details, see the [Guardrails Guideline](../doc/GUARDRAILS.md).

### Defining Tools with Callable Processes

To function effectively, AI agents require tools to perform tasks. With Smart Workflow, creating a tool is straightforward: simply define a callable process and add the `tool` tag to it.

To select the appropriate tool, AI agents rely on the descriptions of callable processes. To ensure efficient tool selection, clearly describe the tool's purpose in the `description` field.

![Tool configurations](img/tool-configurations.png)

### Defining AI agent

To define an AI agent, create a program element backed by the `com.axonivy.utils.smart.workflow.AgenticProcessCall` Java bean. In the `Configuration` tab, you can access and customize detailed settings for your AI agent.

#### Message

In the `Message` section, you can specify the user message and system message for the agent. By allowing code injection directly into these fields, Smart Workflow offers a convenient way for developers to define messages before they are sent to the AI service.

![Message configurations](img/agent-message-configurations.png)

#### Tools

Below the `Messages` section is the `Tools` section, where you can define the set of tools the agent should use as a String array. For example:

```java
["findProduct","createProduct","checkProductDependencies", "createProductSearchCriteria"]
```

By default, if no tools are specified, Smart Workflow assumes the agent can use all available tools. Therefore, it is recommended to define a specific set of tools for each agent to improve response speed and prevent the use of inappropriate tools.

#### Model

Not all AI agents are created equal. 
In Axon Ivy, we recognize that AI agents handle tasks of varying complexity. 
Some agents perform simple tasks, such as creating leave requests or gathering user information, 
while others must search databases for products and evaluate dependencies like suppliers and brands. 
Therefore, Smart Workflow allows developers to select the underlying AI model based on the use case.

To do this, simply enter the desired AI model in the `Model` section. 
By default, if no model is specified, Smart Workflow uses the model defined in the variable `AI.OpenAI.Model`.

#### Output

For enterprise-level AI applications, it is common to require the AI agent’s result in the form of a usable object.
To address this need, the Smart Workflow AI agent can produce output as a Java object, ready to be used directly by Axon Ivy processes.

You can easily configure this by specifying both the expected result type and the target object to map the result to in the `Output` section.

![Other configurations](img/agent-other-configurations.png)
