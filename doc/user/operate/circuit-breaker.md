# Circuit Breaker

The circuit breaker is an application-wide kill switch for AI. When it is on, Smart Workflow stops **every** agent call in the application — regardless of which guardrails an individual agent configures, and regardless of whether a user starts a brand new process.

Use it when AI must go offline immediately: a provider incident, a runaway cost situation, a leaked prompt, or a compliance hold.

It is controlled centrally by one Ivy variable, and it reports a stopped call to your processes as a dedicated BPM error so that you can define a fallback.

## Configuring the circuit breaker

Set `AI.CircuitBreaker.Enabled` in the **Engine Cockpit**, under **Variables**. The variable ships with this declaration and default:

```yaml
Variables:
  AI:
    # Emergency circuit breaker.
    CircuitBreaker:
      # "true" stops ALL AI agent calls in this application. "false" (or blank) allows them again.
      Enabled: "false"
```

| Value | Effect |
| --- | --- |
| `true` | All agent calls in the application are stopped |
| `false` | Agents run normally |

The switch can also be flipped from process code with `CircuitBreakerSignal.stopAll()` and `resumeAll()` — see [Java API](../reference/java-api.md#circuit-breaker).

## Handling a stopped agent

When the circuit breaker blocks a call, an exception is thrown with the error code `smartworkflow:stop`. Catch it with an **Error Boundary Event** on the agent element and route to a fallback — a human, a non-AI path, a message that AI is temporarily unavailable, or a parked case to retry later. The full recipe is in [Error Codes](../reference/error-codes.md#catching-one).

Every agent call should have such a fallback. That is the point of the circuit breaker: the business process keeps running without AI instead of failing.

### Returning a stopped flag from a subprocess

When your agent lives inside a reusable callable subprocess, the caller usually has to decide what to do. Catch the error inside the subprocess and return it as a result parameter instead of letting it escape:

1. In the subprocess, add an Error Boundary Event for `smartworkflow:stop` on the agent element.
2. From the boundary event, run a Script step that sets a flag — for example `in.isStopped = true`.
3. Add the flag to the subprocess result parameters, next to the agent result.
4. In the calling process, branch on that flag with an **Alternative** and take the non-AI path when it is `true`.

The `SelfContainedAgent` process in the `smart-workflow-demo` project implements exactly this pattern.

## Observability

The circuit breaker takes part in guardrail observability like any other guardrail, recorded under the guardrail name `CircuitBreakerGuardrail` with the stop reason as its failure message. Use it to confirm afterwards which calls were stopped and when — see [Guardrail records](observability.md#guardrail-records).

## Scope and limits

Know these boundaries before you rely on the circuit breaker:

- **A call already in flight is not cancelled.** The breaker takes effect at the guardrail boundaries around the LLM call. A request that already reached the provider finishes its exchange — including a long tool loop or a large response — and is stopped when its output is evaluated. Tokens for that call are still spent.
- **It is all or nothing, for the whole application.** You cannot stop only one agent, only one user's sessions, or only calls that have been running for a long time. Every agent in the application is affected.

## Demo

The `ApplicationScope` process in the `smart-workflow-demo` project (`Patterns/CircuitBreaker`) is a working example with two start links:

| Start | Does |
| --- | --- |
| `runApplicationAgentsDemo` | Loads an invoice image from the CMS, extracts its content with an agent, then analyses it through the `SelfContainedAgent` subprocess |
| `stopApplicationAgents` | Calls `CircuitBreakerSignal.stopAll()`, stopping all agent calls |

Start the demo and trigger `stopApplicationAgents` while it runs to see both fallback styles: the error boundary event on the extraction agent, and the `isStopped` result flag returned by the analysis subprocess.

## See also

- [Error Codes](../reference/error-codes.md) — catching `smartworkflow:stop`
- [Guardrails](guardrails.md) — the other way an agent call gets blocked
- [Observability](observability.md) — seeing stopped calls after the fact
- [Java API](../reference/java-api.md#circuit-breaker) — `CircuitBreakerSignal`
