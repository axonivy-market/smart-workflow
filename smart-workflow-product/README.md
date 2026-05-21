# Smart Workflow

Smart Workflow integrates AI agent capabilities into your Axon Ivy processes. It enables invoking AI agents from within processes to extract structured, actionable results and automate user interactions.

![Agent Message Configurations](img/agent-message-configurations.png)

## Key features

- Invoke AI agents from Axon Ivy processes and receive structured results that can be mapped to dataclasses, enabling automated data extraction and downstream processing.
- Ready-to-run demo workflows (Shopping, Support, RAG, Agent demos) that showcase common integration scenarios and accelerate evaluation.
- Built-in RAG support and vector store integration for context-aware retrieval augmented generation.
- Provider-agnostic configuration supporting OpenAI, Azure OpenAI, Gemini, xAI, and Anthropic via `config/variables.yaml`.
- Observability and guardrails for safe AI usage: configurable logging, guardrails, and custom fields for auditability.
- Marketplace installers and Maven artifacts for straightforward deployment and integration.

## Demo

Check the demo implementations provided in the `smart-workflow-demo` module, which include user-facing examples and integrations.

### Demo workflows

#### Smart Workflow Demo (smart-workflow-demo)

##### Smart Workflow Agent Demo

1. Launch the "Smart Workflow Agent Demo" from the demo menu or dashboard.
2. A pre-filled input will appear allowing you to provide or review the query text to analyze (for example, invoice text).
3. Click the demo action to invoke the AI agent; the agent parses the input and returns a structured result.
4. Review the parsed data (for example, the generated Invoice object) displayed in the UI and confirm the result.
5. Optionally export or persist the structured output.

##### Create new product (smart-workflow-demo)

1. Open the Shopping demo and choose "Create new product".
2. A product creation dialog will appear with fields for product metadata.
3. Fill in the required fields and submit the form to create the product.
4. Review the created product entry in the shopping store list.

##### Shopping Store (smart-workflow-demo)

1. Open the Shopping demo and select "Shopping Store".
2. Browse available items and add them to the cart using the interactive dialog.
3. Proceed to checkout to simulate order and inventory updates.

## Setup

- **Roles:** Roles configuration not documented
- **OpenAPI:** No public OpenAPI specs delivered by this extension.

### Variables

```yaml
Variables:
  AI:
    # [enum: OpenAI, AzureOpenAI, Gemini, xAI, Anthropic]
    DefaultProvider: ""
    # Guardrails designed to ensure that AI operate safely ethical, and legal
    Guardrails:
      # Default input guardrails. Separated by comma. Available: PromptInjectionInputGuardrail, PiiMaskingGuardrail
      DefaultInput: ""
      # Default output guardrails. Separated by comma. Available: SensitiveDataOutputGuardrail, PiiMaskingGuardrail
      DefaultOutput: ""
    Tool:
      WebSearch:
        # Name of the search engine to use. Must match the name() of a registered SmartWebSearchEngine. Example: "duckduckgo"
        Engine: "duckduckgo"
        # Maximum number of search results returned per query.
        MaxResults: ""
        # Whitelist of allowed domains for web search results. Separated by comma. Example: "stackoverflow.com, github.com, docs.oracle.com"
        # If empty, all domains are allowed.
        WhitelistDomains: ""
    RAG:
      # Default number of document segments returned per query.
      MaxResults: "5"
      # Cosine similarity threshold (0.0 - 1.0). Segments below this score are excluded.
      MinScore: "0.6"
      # Number of tokens per document chunk when splitting.
      ChunkSize: "300"
      # Number of overlapping tokens between consecutive chunks.
      ChunkOverlap: "20"
      EmbeddingModel:
        # Provider used to generate embeddings. Only providers that support embedding are valid.
        # When blank, falls back to AI.DefaultProvider.
        Provider: ""
        # Embedding model name. When blank, defaults to the provider's DefaultEmbeddingModel variable.
        # Example: "text-embedding-3-small" (OpenAI), "gemini-embedding-001" (Gemini)
        Name: ""
        # Optional separate API key for embedding calls (billed separately from chat).
        # When blank, the provider's own API key variable is used.
        #[password]
        ApiKey: ${decrypt:}
    Observability:
      CustomFields:
        # Enable marking of workflow custom fields to track AI usage provenance.
        Enabled: "true"
      Ivy:
        # Enable chat history recording for governance audit.
        Enabled: ""
      Openinference:
        # Enable logging of AI interactions for observability and debugging purposes.
        Enabled: ""
        HideInputMessages: ""
        HideOutputMessages: ""
```

- No information was delivered for this section.

## Components

### Connector Processes

#### SmartWorkflowAgent.p.json

- **invokeAgent(String query, String systemMessage, List<String> tools, Class resultType) -> resultObject: Object**
    - Input:
        - `query` (String) - The user query/message to send to the AI agent
        - `systemMessage` (String) - System message to define agent behavior
        - `tools` (List<String>) - List of tool names available to the agent. Keep empty to use all tools
        - `resultType` (Class) - Expected result type class for structured output. Keep null for String result
    - Result:
        - `resultObject` (Object) - The AI agent response result

### Form Components

#### SmartWorkflowAgentData — Data Class for AI agent invocation
- **Namespace:** Portal
- **Component type:** Data Class
- **Fields:**
   - `query` (String) — The user query/message to send to the AI agent
   - `systemMessage` (String) — System message to define agent behavior
   - `tools` (List<String>) — List of tool names available to the agent. Keep empty to use all tools
   - `resultType` (Class) — Expected result type class for structured output. Keep null for String result
   - `resultObject` (Object) — The AI agent response result
- **Where used:** `SmartWorkflowAgent.p.json` (invokeAgent CallSubStart)
- **Purpose:** Provides parameters and result mapping for invoking the Smart Workflow Agent from Portal processes

### Maven artifacts

1. smart-workflow

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

2. smart-workflow-demo

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-demo</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

3. smart-workflow-openai

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-openai</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

4. smart-workflow-azure-openai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-azure-openai</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

5. smart-workflow-gemini *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-gemini</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

6. smart-workflow-xai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-xai</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

7. smart-workflow-anthropic *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-anthropic</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```
