# Smart Workflow RAG

Retrieval-Augmented Generation (RAG) enhances AI responses in Axon Ivy Smart Workflow by grounding them in your own documents and knowledge bases. Instead of relying solely on the LLM's training data, RAG retrieves relevant content from a vector store and includes it as context — producing answers that are accurate, verifiable, and specific to your organization.

The workflow is straightforward:

1. **Ingest** — Split your documents into chunks, generate embeddings, and store them in a vector store.
2. **Search** — When a question arrives, embed the query, find the most similar chunks, and return them.
3. **Answer** — The LLM receives the retrieved chunks as context and generates a grounded response.

Smart Workflow provides callable subprocesses and AI tools that handle steps 1 and 2. Step 3 is handled by the `AgenticProcessCall` element, which orchestrates the LLM and tool calls automatically.

## OpenSearch

[OpenSearch](https://opensearch.org/) is a scalable, open-source search and analytics engine that supports k-NN vector search — making it a natural fit for RAG workloads.

The `smart-workflow-opensearch-rag` module provides a callable subprocess for setup and an AI tool that an agent can invoke at runtime.

### Callable: `createVectorStore`

Use this callable subprocess to create an OpenSearch index and ingest documents before the agent runs.

**Input parameters**

| Parameter | Type | Description |
| --- | --- | --- |
| `collection` | String | Index name to ingest into. |
| `sources` | List\<String\> | Plain text documents to index. |

**Result**

| Parameter | Description |
| --- | --- |
| `result` | Ingestion result. `answer` contains the number of indexed segments; `error` contains failure details if something went wrong. |

### Tool: `openSearchSearch`

Semantic search tool available to Smart Workflow agents. The agent calls this tool automatically when it needs to look up relevant content from a knowledge base.

**Input parameters**

| Parameter | Type | Description |
| --- | --- | --- |
| `collection` | String | Index name to query. |
| `query` | String | The search query to find relevant content. |
| `maxResults` | Integer | Maximum segments to return. When null, `AI.RAG.MaxResults` is used. |
| `minScore` | Double | Minimum similarity score (0.0–1.0). When null, `AI.RAG.MinScore` is used. |

**Result**

| Parameter | Description |
| --- | --- |
| `result` | Search results containing matched content segments with their similarity scores. |

### Wiring the tool to an agent

Select `openSearchSearch` in the agent's `Available tools` picker, then **tell the agent which collection to use in the system message**. The `collection` parameter is required and has no default, so an agent that is not told the index name cannot search — this is the most common reason a RAG agent answers from training data instead of your documents.

The demo does it by interpolating the index name:

```text
You are a RAG (Retrieval-Augmented Generation) assistant.
You MUST always call the openSearchSearch tool before answering any question.
Use the collection <%=in.indexName%>.
Base your answer strictly on the retrieved documents.
If no relevant documents are found, clearly state that no relevant information was found
in the knowledge base. Do not answer from your own knowledge.
```

Two things in that prompt are doing real work beyond the collection name: an explicit instruction to *always* search, because models otherwise answer directly when they think they know; and an explicit instruction not to fall back on training data, which is what makes an ungrounded answer visible instead of plausible.

### Configuration

Retrieval and chunking defaults are set in the **Engine Cockpit**, under **Variables**:

```yaml
Variables:
  AI:
    RAG:
      # Default number of document segments returned per query.
      MaxResults: "5"
      # Cosine similarity threshold (0.0 - 1.0). Segments below this score are excluded.
      MinScore: "0.6"
      ChunkSize: "300"
      ChunkOverlap: "20"
      EmbeddingModel:
        # When blank, falls back to AI.DefaultProvider. Must support embedding.
        Provider: ""
        # When blank, the provider's DefaultEmbeddingModel is used.
        Name: ""
        # Optional separate key for embedding calls, billed separately from chat.
        #[password]
        ApiKey: ${decrypt:}
```

`MaxResults` and `MinScore` are defaults that the agent can override per call, since both are tool parameters. `ChunkSize` and `ChunkOverlap` apply at ingestion time only — changing them has no effect on documents already indexed.

> **Note:** `ChunkSize` and `ChunkOverlap` are counted in **characters**, not tokens, despite what the comments in `variables.yaml` say. Documents are split with a recursive splitter that works on character counts.

Only providers that support embedding are valid for `EmbeddingModel.Provider` — currently OpenAI and Ollama. See the [Provider Capabilities](../reference/capabilities.md#embedding).

The OpenSearch connection itself is configured separately, in the `smart-workflow-opensearch-rag` module: `AI.RAG.OpenSearch.Url`, plus `ApiKey` or `UserName`/`Password` for authentication, and `TrustSelfSignedCertificates`.

> **Note:** All ingested segments are tagged with the source metadata value `inline`, regardless of which document they came from. Per-document provenance is not retained at this layer, so an instruction to "cite the source document" cannot be satisfied from segment metadata alone.

### Demo

The `RagChatBotDemo` process in `smart-workflow-demo` is an interactive four-step wizard that demonstrates a complete RAG pipeline:

1. **Configuration** — Review the OpenSearch server URL, authentication type, and embedding model settings loaded from Ivy variables. Test the connection before proceeding.
2. **Upload & Embed** — Enter an index name, upload `.txt` or `.md` files, and embed the documents into OpenSearch as searchable vector chunks.
3. **Results** — Inspect all indexed chunks with their source file and a content preview.
4. **Chat** — Ask questions answered by an AI agent that retrieves grounded context from the indexed documents using the `openSearchSearch` tool.

**Prerequisites:**

```properties
AI.DefaultProvider           = OpenAI          # or AzureOpenAI / Gemini
AI.RAG.OpenSearch.Url        = https://my-opensearch.us-east-1.es.amazonaws.com
# AI.RAG.EmbeddingModel.Provider can be left blank if AI.DefaultProvider supports embedding
```

> **Tip:** Our [dev container](https://github.com/axonivy-market/smart-workflow/blob/master/doc/dev/DEVCONTAINER.md) is pre-configured with an
> OpenSearch service, so you can skip the server setup and `AI.RAG.OpenSearch.Url`
> configuration. In that environment you only need to define the AI Provider API key.

## Common mistakes

- **Not telling the agent the collection name.** `collection` is required and has no default. This is the most common reason a RAG agent answers from training data instead of your documents.
- **Not instructing the agent to always search.** Models answer directly when they think they know. Say "you MUST always call the tool" and say not to fall back on training data.
- **An embedding provider that cannot embed.** `AI.RAG.EmbeddingModel.Provider` accepts only OpenAI or Ollama.
- **Changing `ChunkSize` and expecting existing documents to change.** Both chunking variables apply at ingestion time only; re-index to take effect.
- **Asking the agent to cite source documents.** Every ingested segment is tagged `inline`, so per-document provenance is not available from segment metadata.

## See also

- [Agent Setup](agent-setup.md) — configuring the agent that answers
- [Defining Tools](tools.md) — how tools are selected and described
- [Provider Capabilities](../reference/capabilities.md#embedding) — which providers support embedding
- [Variables](../reference/variables.md#rag) — every `AI.RAG.*` setting
