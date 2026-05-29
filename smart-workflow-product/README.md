# Smart Workflow

Smart Workflow integrates AI agents into Axon Ivy processes, enabling intelligent automation across routine tasks and integrations.

![Agent message configurations](img/agent-message-configurations.png)

Use prebuilt demo workflows and connectors to integrate language models and tools directly into your business processes.

**Key features**

- Invoke AI agents from your processes to automate decisions and generate structured outputs.
- Connect to multiple AI providers and tools with built-in adapters for flexible integration.
- Explore demo workflows for common scenarios like support ticket creation and invoice extraction.
- Receive structured results suitable for downstream automation and reporting.
- Configure connectors and runtime behavior via repository variables for easy deployment.
- Includes reusable dataclasses and UI components to accelerate integration and testing.

## Demo

Explore the demo implementations included in the `smart-workflow-demo` module for hands-on examples and runnable scenarios.

### Demo Workflows

#### Features (smart-workflow-demo/processes/Features)

##### File Extraction Demo (CMS)

1. Launch the File Extraction demo from the demo menu.
2. Upload or select an invoice image from the CMS or local files.
3. The agent analyzes the document and extracts invoice data.
4. Review the extracted invoices in the logs or UI.

##### File Extraction Demo (Binary)

1. Launch the File Extraction (Binary) demo from the demo menu.
2. Upload a PDF file containing invoices.
3. The agent processes the binary input and extracts invoice information.
4. Review results and exported data.

#### AgentDemo (smart-workflow-demo/processes/AgentDemo)

##### Support Agent with Tools Demo

1. Launch the Support Agent with Tools demo from the demo menu.
2. Provide a support query or select a predefined example.
3. The agent runs with configured tools and returns a support ticket result.
4. Review the support ticket details and follow up as needed.

## Setup

- **Roles:** Roles configuration not documented
- **OpenAPI:** No information was delivered for this section.


### Variables

```
@variables.yaml@
```

## Components

### Callable Subprocesses

#### SmartWorkflowAgent.p.json

- **invokeAgent -> resultObject: Object**

- Input:
  - `query` (String) - The user query/message to send to the AI agent
  - `systemMessage` (String) - System message to define agent behavior
  - `tools` (List<String>) - List of tool names available to the agent. Keep empty to use all tools
  - `resultType` (Class) - Expected result type class for structured output. Keep null for String result
- Result:
  - `resultObject` (Object) - The AI agent response result

### Dialog Components

#### SupportAgentData — Support agent data payload

- **Namespace:** AgentDemo
- **Component type:** Data Class
- **Fields:**
  - `taskInfo` (com.axonivy.utils.smart.workflow.demo.dto.TaskInfo) — Task information object
  - `query` (String) — The raw query text
  - `targetObject` (com.axonivy.utils.smart.workflow.demo.dto.SupportTicket) — Support ticket object
  - `customInstructions` (List<String>) — Custom instructions for the agent

### Web Services

- No information was delivered for this section.

### Maven Artifacts

1. smart-workflow

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow</artifactId>
  <type>iar</type>
</dependency>
```

2. smart-workflow-demo

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-demo</artifactId>
  <type>iar</type>
</dependency>
```

3. smart-workflow-openai

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-openai</artifactId>
  <type>iar</type>
</dependency>
```

4. smart-workflow-azure-openai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-azure-openai</artifactId>
  <type>iar</type>
</dependency>
```

5. smart-workflow-gemini *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-gemini</artifactId>
  <type>iar</type>
</dependency>
```

6. smart-workflow-xai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-xai</artifactId>
  <type>iar</type>
</dependency>
```

7. smart-workflow-anthropic *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-anthropic</artifactId>
  <type>iar</type>
</dependency>
```
