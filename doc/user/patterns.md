# Agent Patterns

Once a process has more than one agent, how you arrange them matters more than how you prompt them. Three patterns cover most cases. They are not exclusive — a large application usually contains all three.

Each has a working implementation under [`smart-workflow-demo/process/Patterns/`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Patterns/).

## Agent pipeline

A linear chain: each agent processes an input and hands its result to the next stage.

Use it when the work decomposes into ordered steps that each need a different instruction — extract, then classify, then summarize. Splitting one long prompt into three short ones almost always beats a single agent asked to do everything, because each step gets a focused system message and a result you can inspect.

Give each agent its own task, and you get:

- Visibility — each step has its own entry in the task history, its own conversation record and its own trace, so you can see what it received and returned instead of reading one long exchange.
- The right model per step — a strong reasoning model where the decision is hard, a vision-capable one for reading documents, a cheap one for a simple classification. See [mixing providers](providers.md#mixing-providers-in-one-process).
- Guardrails per step — each agent validates what that step actually needs.
- Failure isolation — a guardrail violation or a [circuit breaker](circuit-breaker.md) stop lands on one element, so you can retry it or route it to a fallback while the work already finished stays in your process data.

See the **Agent Pipeline Demo** process in `smart-workflow-demo`.

## Self-contained agent with co-located tools

The agent and the tools it uses live in one process file, with no cross-process references.

Use it when a capability should ship as a single unit — one callable interface in, one result out, nothing else in the project needs to know how it works.

Keep the agent and its tools in one callable process file, and you get:

- Portability — the whole capability is one file, so moving it into another project takes nothing else with it.
- Readability — the agent, its tools and their wiring are visible together, so you can follow what the capability does without opening anything else.
- A clean contract — callers see one callable, and a change inside stays inside.
- Errors that do not escape — catch a guardrail violation or a [circuit breaker](circuit-breaker.md) stop inside the process and return a flag instead, so the caller gets a result object rather than an exception. See [Returning a stopped flag from a subprocess](circuit-breaker.md#returning-a-stopped-flag-from-a-subprocess).

See the **Self-Contained Agent** process in `smart-workflow-demo`.

## Feature-grouped agents and tools

Agents and tools organized by business domain, each in its own process file under a common feature folder.

Use it when tools must be shared across several agents — a `findProduct` tool needed by both the product agent and the search agent. Bundling everything into one callable stops working at that point; the domain boundary becomes the organizing unit instead, and tools are reused across the agents inside it.

The cost is indirection: the capability is no longer visible on one canvas. Reach for this pattern when sharing forces it, not before.

See the **Shopping Demo** process in `smart-workflow-demo`.

## Choosing between them

| Situation | Pattern |
| --- | --- |
| Ordered steps, each needing different instructions | Agent pipeline |
| One capability, shipped and called as a unit | Self-contained agent |
| Several agents needing the same tools | Feature-grouped |
| A step that sometimes needs a person | Any of the three, plus [Human in the Loop](human-in-the-loop.md) |

Two rules apply whichever you pick:

- Keep each agent's tool list tight. Every granted tool costs tokens in every request and gives the model another way to choose wrong.
- Give every agent call a fallback. A provider incident or a [circuit breaker](circuit-breaker.md) stop should route the process onto a non-AI path, not fail it.

## See also

- [Agent Setup](agent-setup.md) — configuring a single agent
- [Defining Tools](tools.md) — what a tool is and how it is discovered
- [Human in the Loop](human-in-the-loop.md) — suspending a pattern for a human decision
- [Circuit Breaker](circuit-breaker.md) — fallbacks when AI is switched off
