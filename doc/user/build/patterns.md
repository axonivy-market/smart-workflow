# Agent Patterns

Once a process has more than one agent, how you arrange them matters more than how you prompt them. Three patterns cover most cases. They are not exclusive — a large application usually contains all three.

Each has a working implementation under [`smart-workflow-demo/process/Patterns/`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Patterns/).

## Agent pipeline

A linear chain: each agent processes an input and hands its result to the next stage.

Use it when the work decomposes into ordered steps that each need a different instruction — extract, then classify, then summarize. Splitting one long prompt into three short ones almost always beats a single agent asked to do everything, because each step gets a focused system message and a result you can inspect.

**Give each agent its own task.** That is what makes the run tracked, resumable, and visible in the task history — and it is what lets a failed step be retried without re-running the whole chain.

A pipeline is also where [mixing providers](providers.md#mixing-providers-in-one-process) pays off: spend on the step where accuracy is critical, use a cheap model for the trivial classification, keep the sensitive step on hardware you own.

See the **Agent Pipeline Demo** process in `smart-workflow-demo`.

## Self-contained agent with co-located tools

The agent and the tools it uses live in one process file, with no cross-process references.

Use it when a capability should ship as a single unit — one callable interface in, one result out, nothing else in the project needs to know how it works. It is the easiest pattern to move between projects and the easiest to reason about, because the whole capability is visible on one canvas.

This is also the pattern that pairs with returning a stopped flag rather than letting an error escape: the caller gets a result object, never an exception. See [Returning a stopped flag from a subprocess](../operate/circuit-breaker.md#returning-a-stopped-flag-from-a-subprocess).

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

- **Keep each agent's tool list tight.** Every granted tool costs tokens in every request and gives the model another way to choose wrong.
- **Give every agent call a fallback.** A provider incident or a [circuit breaker](../operate/circuit-breaker.md) stop should route the process onto a non-AI path, not fail it.

## See also

- [Agent Setup](agent-setup.md) — configuring a single agent
- [Defining Tools](tools.md) — what a tool is and how it is discovered
- [Human in the Loop](human-in-the-loop.md) — suspending a pattern for a human decision
- [Circuit Breaker](../operate/circuit-breaker.md) — fallbacks when AI is switched off
