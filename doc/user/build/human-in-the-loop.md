# Human in the Loop

Agents normally decide on their own. Sometimes they should not — an approval, a compliance gate, a judgment call that has to be made by a person and recorded.

Human in the Loop suspends an agent in the middle of its work, routes the open question to a real user as an Axon Ivy task, and resumes the same agent with the answer once the task is done. The agent keeps its full context across the suspension, including the tool call it was waiting on.

## How it works

The mechanism is a tool that refuses to answer. Instead of returning a value, it throws a `BpmError`, which suspends the agent. An error boundary event on the agent element catches it and routes to a user task. When the task completes you write the human's answer into the suspended conversation and connect the flow **back to the same agent element**, which picks up where it left off.

Three framework pieces make this work, and one convention ties them together.

| Piece | Role |
| --- | --- |
| `aiMemoryId` | A `String` field on your process data class, named exactly this. Holds the handle to the suspended conversation. |
| `DecisionMaker` | Public API. Writes the human's answer into the suspended conversation as the pending tool's result. |
| Error boundary event | Catches the error your tool threw and gets you to a user task. |

Under the hood, when a tool throws, the agent's messages are persisted to Ivy Business Data keyed by the LangChain4j invocation id, and that id is written to `aiMemoryId`. On the next entry to the element the messages are read back and the conversation continues. On successful completion, the stored messages **and** the `aiMemoryId` value are both cleared.

### About `aiMemoryId`

The field name is a hard-coded convention: add a `String` field called exactly `aiMemoryId` to the process data class and the framework finds it. No configuration, no annotation.

```json
{
  "name" : "aiMemoryId",
  "comment" : "name convention: field holding the memory id of an ongoing AI conversation"
}
```

Two things about it are easy to get wrong:

> **Important:** `aiMemoryId` is a suspend/resume handle, not a conversation history feature. It is written **only** when a tool throws a `BpmError`, and it is erased as soon as the agent completes normally. It is not a way to give an agent memory of past interactions.
>
> **Do not pre-populate it.** While `aiMemoryId` holds a value the element behaves as a *resumed* call: the user message is replaced with `continue with my selection from the tool` and the system message is skipped entirely, because both are expected to come from the restored conversation. Setting the field by hand to "continue a chat" therefore discards the query you meant to send.

There is no cross-call conversation memory in Smart Workflow today. Within a single agent call the message list grows unboundedly and there is no variable to cap it; across calls, nothing is retained unless a suspension is in progress.

## Building it

### 1. Add `aiMemoryId` to the data class

Add the `String` field described above. Nothing else works without it — `DecisionMaker` has no way to find the suspended conversation.

### 2. Write a tool that throws

Create a callable sub-process, tag its `CallSubStart` with `tool`, and end it in an `ErrorEnd` that throws an error and attaches the question:

```java
error.setAttribute("decision", in.decision);
```

The error code is **yours to choose** — the demo uses `human:decision`, but nothing in the framework knows that string. All the framework requires is that a `BpmError` propagates out of the tool. Pick a code and use it consistently in the boundary event.

The tool's input parameter carries the question to the human, and its declared result is what the agent expects back. In the demo the input is a `HumanDecision` (a question plus a list of options) and the result is the selected `Choice`.

> **Note:** `com.axonivy.utils.smart.workflow.human.HumanDecision` and `Choice` look like framework classes because of the package, but they are Ivy **data classes in the demo project**. There is no framework type for the question shape — define whatever suits your case.

### 3. Configure the agent element

- Select your tool under `Available tools`.
- In `System message`, tell the agent when to use it. Models do not pause on their own; if the instruction is vague they will answer instead of asking.
- Attach an **Error Boundary Event** with your error code, mapping the attached question onto a data field:

```java
out.decision = error.getAttribute("decision") as com.axonivy.utils.smart.workflow.human.HumanDecision
```

### 4. Route to a user task and back

Connect the boundary event to a `UserTask` whose dialog shows the question and collects the answer. Then connect the task's output **back to the same agent element**. That return edge is what resumes the agent; without it the process ends early.

### 5. Resolve the decision

In the user task's output code, hand the answer to `DecisionMaker` before the flow re-enters the agent:

```java
import com.axonivy.utils.smart.workflow.tools.human.DecisionMaker;

new DecisionMaker(in.aiMemoryId).resolve(result.first.title);
```

This appends the answer to the suspended conversation as the result of the pending tool call, so when the agent resumes, the tool it was waiting on has returned.

> **Important:** `resolve` throws `IllegalStateException` when it cannot find what it needs — `Found no pending ChatMemory for id`, `Found no pending AiMessage for id`, or `Found no pending ToolExecutionRequest for id`. It does not fail quietly, so these messages are your first diagnostic. What *does* fail quietly is writing `aiMemoryId` itself: if the field is missing from the data class, the framework swallows the error and the id is simply never stored, which produces the first of those three messages later.

Each `resolve` call answers exactly **one** pending tool request. An agent that fires several human-input tool calls in one turn is not supported.

## Example

The `Patterns/HumanInTheLoop` demo asks the user to pick an ice cream, which is a small stand-in for any open-goal decision:

- **System message:** `strictly ask the user instead of making bold decisions. present your users requests as HumanDecision (title+options)`
- **User message:** `Pick 3 nice ice creams and ask the user which one he likes to eat today`
- **Available tools:** `askUserDecision`
- **Expect result of type:** `String.class`

The agent proposes three options, calls `askUserDecision`, and suspends. A task named `Assisted: <the agent's question>` appears in the task list. The user picks one, `DecisionMaker` writes the choice back, and the agent resumes and finishes.

Because the tool is a *tool* and not a mandatory step, the agent decides whether it is needed. Give it a threshold in the system message — "ask the human when the amount exceeds 2000" — and the low-value path completes with no human task and no process changes at all.

See [Hibernation.p.json](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Patterns/HumanInTheLoop/Hibernation.p.json) and [HibernationTools.p.json](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Patterns/HumanInTheLoop/HibernationTools.p.json).

## Common mistakes

- **No `aiMemoryId` field.** The id is never stored — the framework ignores the failure — and `resolve` later reports no pending memory.
- **Pre-setting `aiMemoryId`.** Replaces your user message and drops your system message. Leave it to the framework.
- **The user task does not loop back to the same agent element.** The agent never resumes.
- **The `tool` tag is missing** from the `CallSubStart`, so the agent cannot see the tool and answers the question itself.
- **The system message does not tell the agent to ask.** The most common cause of "it never pauses".
- **Expecting the boundary event to catch a guardrail or circuit-breaker error.** Those have their own codes; see [Guardrails](../operate/guardrails.md) and [Circuit Breaker](../operate/circuit-breaker.md).

## See also

- [Agent Setup](agent-setup.md) — the element's fields
- [Defining Tools](tools.md) — writing the callable tool
- [Circuit Breaker](../operate/circuit-breaker.md) — a different way to stop an agent
