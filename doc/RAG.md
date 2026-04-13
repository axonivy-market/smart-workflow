# Smart Workflow RAG

Retrieval-Augmented Generation (RAG) enhances AI responses in Axon Ivy Smart Workflow by grounding them in your own documents and knowledge bases. Instead of relying solely on the LLM's training data, RAG retrieves relevant content from a vector store and includes it as context — producing answers that are accurate, verifiable, and specific to your organization.

The workflow is straightforward:

1. **Ingest** — Split your documents into chunks, generate embeddings, and store them in a vector store.
2. **Search** — When a question arrives, embed the query, find the most similar chunks, and return them.
3. **Answer** — The LLM receives the retrieved chunks as context and generates a grounded response.

Smart Workflow provides callable subprocesses and AI tools that handle steps 1 and 2. Step 3 is handled by the `AgenticProcessCall` element, which orchestrates the LLM and tool calls automatically.

## Common Configuration

These Ivy variables control how documents are split and how search results are filtered. They apply to all vector store backends.

| Variable | Default | Description |
|---|---|---|
| `AI.RAG.ChunkSize` | `300` | Number of tokens per document chunk when splitting. |
| `AI.RAG.ChunkOverlap` | `20` | Number of overlapping tokens between consecutive chunks to preserve context across boundaries. |
| `AI.RAG.MaxResults` | `5` | Maximum number of document segments returned per search query. |
| `AI.RAG.MinScore` | `0.6` | Cosine similarity threshold (0.0–1.0). Segments scoring below this are excluded from results. |

### Embedding Model

An embedding model converts text into vector representations for semantic search.

| Variable | Default | Description |
|---|---|---|
| `AI.RAG.EmbeddingModel.Provider` | *(empty)* | Embedding provider. When blank, falls back to `AI.DefaultProvider`. |
| `AI.RAG.EmbeddingModel.Name` | *(empty)* | Model name override. When blank, uses the provider's default embedding model (e.g. `text-embedding-3-small` for OpenAI). |
| `AI.RAG.EmbeddingModel.ApiKey` | *(encrypted)* | Optional API key override for embedding calls. When blank, the provider's own key is used. |

**Minimal example** — when `AI.DefaultProvider` is already set, `AI.RAG.EmbeddingModel.Provider` can be left blank:

```properties
AI.DefaultProvider = OpenAI
# AI.RAG.EmbeddingModel.Provider left blank — falls back to AI.DefaultProvider
```

**Override example** — use a different provider for embeddings than for chat:

```properties
AI.DefaultProvider             = Anthropic
AI.RAG.EmbeddingModel.Provider = OpenAI
AI.RAG.EmbeddingModel.ApiKey   = <your-openai-key>
```

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

#### Tool: `openSearchIngest`

Ingest tool available to Smart Workflow agents. Allows an agent to index new documents on demand. The index is created automatically if it does not exist.

**Input parameters**

| Parameter | Type | Description |
|---|---|---|
| `collection` | String | Index name to ingest into. Created automatically if it does not exist. |
| `sources` | List\<String\> | Plain text documents to index. |

**Result**

| Parameter | Description |
|---|---|
| `result` | Ingestion result. `answer` contains the number of indexed segments; `error` contains failure details if something went wrong. |

### Configuration

| Variable | Default | Description |
|---|---|---|
| `AI.RAG.OpenSearch.Url` | *(empty)* | Base URL of the OpenSearch server. |
| `AI.RAG.OpenSearch.ApiKey` | *(encrypted)* | API key for authenticated access. Leave blank when using username/password or when security is disabled. |
| `AI.RAG.OpenSearch.UserName` | *(empty)* | Username for basic authentication. Leave blank when security is disabled. |
| `AI.RAG.OpenSearch.Password` | *(encrypted)* | Password for basic authentication. Leave blank when security is disabled. |

**Example:**

```
# Local Docker (security disabled, no auth)
AI.RAG.OpenSearch.Url = http://localhost:19600

# AWS OpenSearch Service (API key auth)
AI.RAG.OpenSearch.Url    = https://my-domain.us-east-1.es.amazonaws.com
AI.RAG.OpenSearch.ApiKey = <your-api-key>

# Self-hosted OpenSearch (basic auth)
AI.RAG.OpenSearch.Url      = https://opensearch.internal:9200
AI.RAG.OpenSearch.UserName = admin
AI.RAG.OpenSearch.Password = <your-password>
```

### Demo

The `ExternalRagDemo` process in `smart-workflow-demo` demonstrates a complete RAG pipeline:

1. Loads three HR documents (insurance plans, company rules, benefits) from the project CMS.
2. Ingests them into an OpenSearch index named `hr-knowledge` (skipped if the index already exists).
3. Asks three HR-related questions through an AI agent that uses the `openSearchSearch` tool.
4. Logs each answer along with the matched knowledge base segments and their similarity scores.

**Prerequisites:**

```properties
AI.DefaultProvider           = OpenAI          # or AzureOpenAI / Gemini
AI.RAG.OpenSearch.Url        = http://localhost:19600
# AI.RAG.EmbeddingModel.Provider can be left blank if AI.DefaultProvider supports embedding
```

### Others

The `doc/external-vector-stores/opensearch/` folder contains helper scripts to quickly initialize Docker images and start a local OpenSearch vector store:

- `start.ps1` — PowerShell (Windows)
- `start.sh` — Bash (Linux / macOS)

> **Warning:** These scripts are for **demo and development use only** and must not be used in production.
