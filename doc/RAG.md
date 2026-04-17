# Smart Workflow RAG

Retrieval-Augmented Generation (RAG) enhances AI responses in Axon Ivy Smart Workflow by grounding them in your own documents and knowledge bases. Instead of relying solely on the LLM's training data, RAG retrieves relevant content from a vector store and includes it as context — producing answers that are accurate, verifiable, and specific to your organization.

The workflow is straightforward:

1. **Ingest** — Split your documents into chunks, generate embeddings, and store them in a vector store.
2. **Search** — When a question arrives, embed the query, find the most similar chunks, and return them.
3. **Answer** — The LLM receives the retrieved chunks as context and generates a grounded response.

Smart Workflow provides callable subprocesses and AI tools that handle steps 1 and 2. Step 3 is handled by the `AgenticProcessCall` element, which orchestrates the LLM and tool calls automatically.

## OpenSearch

[OpenSearch](https://opensearch.org/) is a scalable, open-source search and analytics engine that supports k-NN vector search — making it a natural fit for RAG workloads.

The `smart-workflow-opensearch-rag` module provides a callable subprocess for setup and two AI tools that an agent can invoke at runtime.

#### Callable: `createVectorStore`

Use this callable subprocess to create an OpenSearch index and ingest documents before the agent runs.

**Input parameters**

| Parameter | Type | Description |
|---|---|---|
| `collection` | String | Index name to ingest into. |
| `sources` | List\<String\> | Plain text documents to index. |

**Result**

| Parameter | Description |
|---|---|
| `result` | Ingestion result. `answer` contains the number of indexed segments; `error` contains failure details if something went wrong. |

#### Tool: `openSearchSearch`

Semantic search tool available to Smart Workflow agents. The agent calls this tool automatically when it needs to look up relevant content from a knowledge base.

**Input parameters**

| Parameter | Type | Description |
|---|---|---|
| `collection` | String | Index name to query. |
| `query` | String | The search query to find relevant content. |
| `maxResults` | Integer | Maximum segments to return. When null, `AI.RAG.MaxResults` is used. |
| `minScore` | Double | Minimum similarity score (0.0–1.0). When null, `AI.RAG.MinScore` is used. |

**Result**

| Parameter | Description |
|---|---|
| `result` | Search results containing matched content segments with their similarity scores. |

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
