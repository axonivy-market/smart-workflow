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

The suspended conversation is stored under `aiMemoryId` and read back the next time the flow enters the element. Once the agent completes, both the stored conversation and the `aiMemoryId` value are cleared.

### About `aiMemoryId`

The field name is a hard-coded convention: add a `String` field called exactly `aiMemoryId` to the process data class and the framework finds it. No configuration, no annotation.

```json
{
  "name" : "aiMemoryId",
  "comment" : "name convention: field holding the memory id of an ongoing AI conversation"
}
```

Two things about it are easy to get wrong:

> **Important:** `aiMemoryId` is a suspend/resume handle, not a conversation history feature. Smart Workflow fills it in and clears it again on its own, so it is not a way to give an agent memory of past interactions.
>
> **Do not set it yourself.** An agent that starts with a value in `aiMemoryId` treats the call as a resumption: your system message and user message are both ignored, so the question you meant to ask is never sent.

Apart from a suspension, each agent call is self-contained: it starts fresh from your system and user messages, which keeps every run predictable and repeatable. [Concepts](concepts.md#memory) covers what an agent does and does not carry between calls.

## Building it

### 1. Add `aiMemoryId` to the data class

Add the `String` field described above. Nothing else works without it — `DecisionMaker` has no way to find the suspended conversation.

### 2. Write a tool that throws

Create a callable sub-process, tag its `CallSubStart` with `tool`, and end it in an `ErrorEnd` that throws an error and attaches the question:

```java
error.setAttribute("decision", in.decision);
```

The error code is **yours to choose**; all Smart Workflow requires is that a `BpmError` propagates out of the tool. Give each agent its own code rather than sharing one — name it after the agent, such as `invoiceAgent:waiting` or `supplierAgent:waiting`. A dedicated code keeps each boundary event catching only its own agent's question, so two agents that both ask for a decision never resume each other.

The tool's input parameter carries the question to the human, and its declared result is what the agent expects back. Both shapes are yours to define. Smart Workflow has no type of its own for the question.

### 3. Configure the agent element

In the agent element's **Configuration** tab:

- Select your tool under `Available tools`.
- In `System message`, tell the agent when to use it. Models do not pause on their own; if the instruction is vague they will answer instead of asking.

Then attach an **Error Boundary Event** to the agent element. On the boundary event, set your error code in the **Error** tab, and map the attached question onto a data field in the **Output** tab:

```
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

Smart Workflow never calls `resolve` for you — the boundary event and the return edge only get the flow back to the agent. Without this line the agent resumes to find its tool call still unanswered.

> **Important:** `resolve` does not fail quietly; it throws `IllegalStateException` naming which part of the chain is missing. [Error Codes](reference/error-codes.md#problems-without-an-error-code) lists the three messages and what each one means. What *does* fail quietly is writing `aiMemoryId` itself: if the field is missing from the data class, the framework swallows the error and the id is never stored.

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

- No `aiMemoryId` field. The id is never stored — the framework ignores the failure — and `resolve` later reports no pending memory.
- Pre-setting `aiMemoryId`. Replaces your user message and drops your system message. Leave it to the framework.
- The user task does not loop back to the same agent element. The agent never resumes.
- The `tool` tag is missing from the `CallSubStart`, so the agent cannot see the tool and answers the question itself.
- The system message does not tell the agent to ask. The most common cause of "it never pauses".
- Expecting the boundary event to catch a guardrail or circuit-breaker error. Those have their own codes; see [Guardrails](guardrails.md) and [Circuit Breaker](circuit-breaker.md).

## See also

- [Agent Setup](agent-setup.md) — the element's fields
- [Defining Tools](tools.md) — writing the callable tool
- [Circuit Breaker](circuit-breaker.md) — a different way to stop an agent
