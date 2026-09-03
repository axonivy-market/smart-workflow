# Troubleshooting

Start from the symptom. Each entry says what to check and where the detail lives.

**Check the log first.** Most problems leave a line there, and that line usually identifies the cause on its own.

## The agent did not answer

In rough order of how often it is the cause:

1. **The circuit breaker is on.** `AI.CircuitBreaker.Enabled` set to `"true"` stops every AI call in the application. See [Circuit Breaker](operate/circuit-breaker.md).
2. **The user message was blank, or failed to expand.** See [Messages and expressions](#messages-and-expressions).
3. **A guardrail blocked it.** Blank guardrail fields still inherit the application defaults, so an agent you never configured can still be blocked. See [Guardrails](operate/guardrails.md).
4. **The result did not map.** The agent ran, but the target field stayed empty. The mapping is a generated assignment, and a failure there is logged rather than thrown, so the process carries on. Look for `Failed to map result to <expression>` — usually a type mismatch between the declared output class and the target field.
5. **A file emptied the user message.** An unsupported CMS file extension takes the whole message down with it, producing the same skipped-call log line as an empty message. See [File Extraction](build/file-extraction.md#common-mistakes).
6. **No tools were selected**, so the agent could not do what you expected it to do.

## The agent answered, but not as expected

1. **It explains what it would do instead of doing it.** Usually no tools are selected — an empty `Available tools` field means the agent has no tools to use at all. See [Defining Tools](build/tools.md#common-mistakes).
2. **It never calls a tool you expected.** The tool is not selected on the element, the `tool` tag is missing from the `CallSubStart`, or the description does not tell the model when to use it.
3. **It ignores its own instructions**, or the reply contains a raw `<%=...%>` expression. See [Messages and expressions](#messages-and-expressions).
4. **It answers from general knowledge instead of your documents.** A RAG agent must be told the collection name in its system message, and told to always search. See [RAG](build/rag.md#common-mistakes).
5. **It never pauses for a human.** The system message does not tell it to ask, so the model decides it can answer alone. See [Human in the Loop](build/human-in-the-loop.md#common-mistakes).
6. **It behaves as though it never saw your document.** A file can be dropped from the message without a log entry — a CMS path that does not exist, or a `Path`, `File` or `IDocument` whose name has no `.png`, `.jpg`, `.jpeg` or `.pdf` extension. Check what the expression actually resolves to before suspecting the prompt. See [File Extraction](build/file-extraction.md#common-mistakes).
7. **The provider cannot read that file type.** Nothing is checked locally, so the request reaches the provider and fails there. See [Provider Capabilities](reference/capabilities.md#file-extraction).
8. **The agent cannot return a list.** A bare collection is not a supported output type, so setting `Expect result of type` to a list does not work. Declare a class with the list as one of its fields and use that class instead. See [Structured output](build/agent-setup.md#structured-output).
9. **On Ollama, a typed result came back as plain text.** The Ollama API cannot accept a JSON schema and tools in the same request, so when an agent has both, Smart Workflow keeps the tools and leaves the schema off. The agent still calls its tools and still answers — the reply is just not schema-constrained. Split the work across two agents if you need both, or use a provider that supports them together. See [Provider Capabilities](reference/capabilities.md#structured-output).

## Messages and expressions

Both messages accept `<%=...%>`, and four of their behaviours are easy to mistake for a bad prompt:

1. **A file in the system message is not read.** Only the user message handles files. An image or PDF expression in the system message is converted with the object's `toString()`, so the model receives something like `Binary@1a2b3c` instead of your document — and answers confidently about a document it never saw.
2. **An unresolved expression is inserted as its own raw `<%=...%>` text.** If the model echoes back `<%=in.invoiceText%>`, the expression is wrong, not the prompt.
3. **If expanding the user message throws, the call is skipped entirely** and the process continues as if nothing happened. Look for `Agent call was skipped, since there was no user query`. A blank user message does the same, which is deliberate — it keeps a half-configured element from failing while you build it.
4. **If expanding the system message throws, the agent runs without one.** It still answers, just with no rules. Check here first when an agent suddenly stops behaving.

See [Agent Setup](build/agent-setup.md#message) for how the two fields differ.

## Configuration

1. **`Unknown model provider <name>`.** `AI.DefaultProvider`, or the element's `Provider` field, names a provider whose project is not installed.
2. **An authentication error from the provider.** The key is wrong, or was never set in the Engine Cockpit.
3. **A model name will not compile.** `Model` is a script field, so the value must be quoted: `"gpt-4o"`.
4. **A variable change had no effect.** Variables are read on each call, so no restart is needed — check you set it on the right application in the Engine Cockpit.
5. **A guardrail blocks valid test data.** Blank guardrail fields inherit `AI.Guardrails.DefaultInput` and `DefaultOutput`, which catches people out in tests especially. See [Guardrails](operate/guardrails.md).

## Errors you can catch

Guardrail violations, the circuit breaker, and errors your own tools throw all raise BPM errors that an Error Boundary Event can handle. See [Error Codes](reference/error-codes.md).

## See also

- [Error Codes](reference/error-codes.md) — every BPM error Smart Workflow raises
- [Agent Setup](build/agent-setup.md) — the element's fields in full
- [Observability](operate/observability.md) — tracing a call to see what the model actually received
