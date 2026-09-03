# Error Codes

Every BPM error Smart Workflow can raise on an `AgenticProcessCall` element. They are separate codes with separate causes, and a boundary event catches exactly one of them — mistaking one for another is a common source of "my error handler never fires".

| Error code | Raised when | Handled in |
| --- | --- | --- |
| `smartworkflow:guardrail:input:violation` | An input guardrail blocked the user message before it reached the model. | [Guardrails](../operate/guardrails.md#handling-guardrail-errors) |
| `smartworkflow:guardrail:output:violation` | An output guardrail blocked the model's response. | [Guardrails](../operate/guardrails.md#handling-guardrail-errors) |
| `smartworkflow:stop` | The circuit breaker is on; the call was refused. | [Circuit Breaker](../operate/circuit-breaker.md#handling-a-stopped-agent) |
| _your own code_ | A [human-in-the-loop](../build/human-in-the-loop.md) tool threw a `BpmError` to suspend the agent. The framework requires only that a `BpmError` propagates — **you choose the code**. The demo uses `human:decision`. | [Human in the Loop](../build/human-in-the-loop.md) |

## Catching one

The recipe is the same for all of them:

1. Add an **Error Boundary Event** to your `AgenticProcessCall` element.
2. Set its error code to the one you want to catch.
3. Implement the fallback — a user-friendly message, an audit entry, a non-AI path.

A minimal handler reads the reason off the error:

```java
in.result = in.error.getMessage();
```

> **Important:** Branch on the **error code**, never on the message text. For guardrail violations the message comes from the guardrail's own reason string, wrapped by Smart Workflow — the wrapper names an internal adapter class rather than your guardrail, and the format is not part of the public contract. Treat the message as human-readable detail only.

## Problems without an error code

Not every problem raises a BPM error — an agent can finish without having answered, leaving only a log line behind. Those cases are collected in [Troubleshooting](../troubleshooting.md#the-agent-did-not-answer).

`DecisionMaker.resolve` is the exception among the human-in-the-loop paths: it throws `IllegalStateException` with `Found no pending ChatMemory for id`, `Found no pending AiMessage for id`, or `Found no pending ToolExecutionRequest for id`. Those messages are diagnostics, not BPM errors, and cannot be caught with a boundary event.

## See also

- [Troubleshooting](../troubleshooting.md) — diagnosing an agent that did not answer
- [Guardrails](../operate/guardrails.md) — what blocks a message
- [Circuit Breaker](../operate/circuit-breaker.md) — the application-wide stop switch
