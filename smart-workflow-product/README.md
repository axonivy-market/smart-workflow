# Smart Workflow 🪄️

Let AI Agent elements drive your dynamic processes.

The project integrates AI agents, tools, and retrievers to enable automated, observable, and extensible workflows.

![Agent message configurations](img/agent-message-configurations.png)

**Key features**

- Invoke AI agents directly from your processes to automate tasks and extract structured data.
- Explore pre-built demo workflows to evaluate agent-driven scenarios quickly.
- Integrate with multiple model providers (OpenAI, Azure, Gemini, Anthropic) for flexible model choice.
- Enhance responses with Retrieval-Augmented Generation (RAG) using OpenSearch connectors for context-aware outputs.
- Monitor AI interactions with built-in observability and tracing (Arize Phoenix integration).
- Use built-in tools (web search, file extraction, guardrails) to safely extend agent capabilities.

## Demo

![Support ticket example](img/support-ticket-example.png)

### Demo Workflows

#### Features (smart-workflow-demo/Features)

##### Smart Workflow Agent Demo
1. Launch the Smart Workflow Agent demo from the demo menu.
2. Enter or paste a user query that describes the task you want the agent to perform.
3. Confirm and submit; the agent runs and returns a structured result.
4. Review the output and copy or persist the structured data as needed.

##### Web Search Demo
1. Start the Web Search demo from the Features demo list.
2. Provide a search query in the dialog and submit.
3. The demo performs a web search and presents a concise summary and individual search results.
4. Review the summarized findings and follow result links for full sources.

#### Shopping (smart-workflow-demo/Business/ShoppingDemo)

##### Create new product
1. Launch the "Create new product" workflow from the demo menu.
2. Fill in product metadata in the dialog (name, category, price, description).
3. Submit the form to create the product and verify the confirmation message.

##### Shopping Store
1. Launch the "Shopping Store" workflow from the demo menu.
2. Browse available products and select items to add to the cart.
3. Proceed to checkout and confirm the order.

## Setup

- **Roles:** Roles configuration not documented
- **OpenAPI:** No information was delivered for this section.

### Variables

![Tool configurations](img/tool-configurations.png)

```
@variables.yaml@
```

1. Arize Phoenix
   1.1 Run Arize Phoenix using Docker: `docker run --rm -p 6006:6006 -p 4317:4317 arizephoenix/phoenix:nightly`
   1.2 Visit the tracing platform in your browser [http://localhost:6006](http://localhost:6006)
2. Visual Studio Code
   2.1 Install the Axon Ivy Designer extension
   2.2 Open the Settings and search for Axon Ivy, in it define:
      - `AxonIvy > Engine: VM args` : `-Dotel.traces.exporter=otlp -Dotel.exporter.otlp.endpoint=http://localhost:6006 -Dotel.resource.attributes=openinference.project.name=smart-workflow`
   2.3 Restart Visual Studio Code (Command > Developer: Reload Window)
   2.4 Set the variable `AI.Observability.Openinference.Enabled=true` in the `config/variables.yaml` of a project depending on smart-workflow.
   2.5 Run an AI assisted process in smart-workflow-demo
3. Devcontainer
   3.1 Our Devcontainer is pre-configured to run Arize Phoenix within your codespace. Define the AI Provider API key to enable reporting to Arize Phoenix.
4. Querying
   4.1 Click on the "smart-workflow" project
   4.2 Enter filter condition `span_kind == 'LLM'`
   4.3 Switch to `All` next to the filter bar

## Components

### Callable Subprocesses

#### SmartWorkflowAgent.p.json

- **Signature**: invokeAgent(String query, String systemMessage, List<String> tools, Class resultType) -> resultObject: Object
    - Input:
        - `query` (String) — The user query/message to send to the AI agent
        - `systemMessage` (String) — System message to define agent behavior
        - `tools` (List<String>) — List of tool names available to the agent. Keep empty to use all tools
        - `resultType` (Class) — Expected result type class for structured output. Keep null for String result
    - Result:
        - `resultObject` (Object) — The AI agent response result

### Dialog Components

- No information was delivered for this section.

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

2. smart-workflow-openai

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-openai</artifactId>
  <type>iar</type>
</dependency>
```

3. smart-workflow-azure-openai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-azure-openai</artifactId>
  <type>iar</type>
</dependency>
```

4. smart-workflow-gemini *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-gemini</artifactId>
  <type>iar</type>
</dependency>
```

5. smart-workflow-xai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-xai</artifactId>
  <type>iar</type>
</dependency>
```

6. smart-workflow-anthropic *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-anthropic</artifactId>
  <type>iar</type>
</dependency>
```
