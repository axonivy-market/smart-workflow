# Defining Tools

AI agents in Smart Workflow use tools to take action. A tool is a named, callable unit of logic that the agent discovers, selects, and invokes at runtime. Smart Workflow supports two kinds of tools.


## Callable process tools

We strongly encourage using callable subprocesses as tools. This approach aligns naturally with how Ivy developers already work and provides full access to the power of the process designer—such as error handling, dialogs, subprocess calls, and other Axon Ivy capabilities.

You can turn any callable subprocess into a tool by simply adding the `tool` tag.

**Steps:**

1. Create a callable sub-process in your Axon Ivy project.
2. Add the tag `tool` to the `CallSubStart`.
3. Write a clear `description` — this is what the agent reads to decide whether to call the tool.

![Tool configurations](img/tool-configurations.png)

Discovery covers the current project **and all required projects**, so a tool defined in a shared base project is available to agents in every project that depends on it.

### What the agent actually sees

Only two things from the process definition reach the model:

| Source | Becomes |
| --- | --- |
| The `CallSubStart` description | The tool description — what it does, when to use it |
| Each **input** parameter's name, type and description | The tool's parameter schema |

> **Important:** Descriptions on **result** parameters are not sent to the model. They are useful documentation for the next developer, but they play no part in the tool contract — the agent only learns what a tool returns by receiving the result, which is handed back as raw JSON.

That makes the input parameter descriptions the highest-leverage text you write. Describe `id` as "the supplier number as printed on the invoice, without the country prefix" and the agent knows exactly what to pass.

### Selecting tools on the agent

Tools are discovered globally but granted per agent, via the `Available tools` picker in the element's **Tools** group.

> **Important:** An empty `Available tools` field means the agent has **no tools to use at all**. This is the usual reason an agent explains what it would do instead of doing it.

Keep the list tight. Every tool you grant costs tokens in the request and gives the model one more way to pick wrong.


## Java tools

Tool logic can also be implemented in Java. This is rarely needed — prefer callable processes whenever possible, and reach for Java only when the logic has no workflow steps and is better expressed as a plain class.

A Java tool implements `SmartWorkflowTool` — a description, its input parameters, and an `execute` method — and is exposed through a `SmartWorkflowToolsProvider` registered via SPI, both under `com.axonivy.utils.smart.workflow.tools.provider`.

The easiest way in is to look at [`TaxCalculatorTool`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/src/com/axonivy/utils/smart/workflow/demo/tool/TaxCalculatorTool.java) in the demo project. It takes a structured `Invoice` object, returns per-item tax calculations, and is registered in [`DemoToolProvider`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/src/com/axonivy/utils/smart/workflow/demo/tool/DemoToolProvider.java) — a complete example you can adapt to your own case.

A few things are handy to know as you do. Conversion runs in both directions for you: arguments are deserialized into the declared type, and whatever you return is serialized back to the agent as JSON, custom types included. The `name()` method is optional and defaults to the simple class name, so you only need it when the agent-facing name should differ. And because only the first line of an SPI services file is read, two providers need two separate files.

Providers are resolved on each agent call, so a newly registered tool appears without a restart.

## Built-in tools

Smart Workflow ships with built-in tools that agents can use out of the box.

### webSearch

Searches the web for current information and returns a list of results with titles, URLs, and content snippets.
Agents select this tool automatically when they need up-to-date or factual information from the internet.

**Configuration** (set in the Engine Cockpit, under **Variables**):

| Variable | Purpose | Default |
| --- | --- | --- |
| `AI.Tool.WebSearch.Engine` | Name of the search engine to use. Must match the `name()` of a registered `SmartWebSearchEngine`. If empty, the first available engine is used. | `duckduckgo` |
| `AI.Tool.WebSearch.MaxResults` | Maximum number of search results returned per query. Empty falls back to `5`. | _(empty)_ |
| `AI.Tool.WebSearch.WhitelistDomains` | Comma-separated list of allowed domains (e.g. `stackoverflow.com, github.com`). If empty, all domains are allowed. | _(empty)_ |

**Search engine**: DuckDuckGo is the shipped default and the only built-in engine. Custom engines can be plugged in by implementing [`SmartWebSearchEngine`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow/src/com/axonivy/utils/smart/workflow/tools/web/SmartWebSearchEngine.java) and registering a [`SmartWebSearchEngineProvider`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow/src/com/axonivy/utils/smart/workflow/tools/web/SmartWebSearchEngineProvider.java) via SPI. Engine names are matched case-insensitively.

**Using the tool in a process**: select `webSearch` in the `Available tools` picker of the agent element.

Agents do not search unless the task calls for it, and a vague system message tends to produce an answer from training data instead. If you want the agent to look things up, say so — and if you want citations, ask for the source URLs explicitly, since they are in the result but the model will not volunteer them.

> **Note:** When the domain whitelist filters out every result, the tool returns an explanatory note alongside the empty result list, and that text goes to the model. An agent reporting that it found nothing may be hitting the whitelist rather than an empty web.

See the [`WebSearchDemo`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Features/WebSearchDemo.p.json) process for a complete example.


## Common mistakes

- Leaving `Available tools` empty. The agent gets no tools. This is the usual reason an agent explains what it would do instead of doing it.
- A parameter with no description. The description is the whole contract — an undescribed `id` is a guess.
- Documenting the result parameters. Their descriptions never reach the model. Only input parameters and the `CallSubStart` description do.
- Forgetting the `tool` tag on the `CallSubStart`, so the tool never appears in the picker.
- Two providers in one SPI services file. Only the first line is read; use two files.
- Granting every tool you have. Each one costs tokens in every request and gives the model another way to choose wrong.

## See also

- [Agent Setup](agent-setup.md) — selecting tools on the agent element
- [Human in the Loop](human-in-the-loop.md) — a tool that suspends the agent for a human decision
- [RAG](rag.md) — the built-in `openSearchSearch` retrieval tool
- [Variables](reference/variables.md#web-search) — configuring `webSearch`
