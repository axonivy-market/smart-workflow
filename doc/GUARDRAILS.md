# Guardrails

Guardrails protect AI agents by validating both user input and AI output. Smart Workflow provides built-in guardrails for common safety concerns and supports custom guardrails via SPI.

**Built-in Guardrails:**

| Guardrail | Type | Description |
|-----------|------|-------------|
| `PromptInjectionInputGuardrail` | Input | Blocks common prompt injection attacks |
| `SensitiveDataOutputGuardrail` | Output | Blocks responses containing API keys or private keys |

## Configuring Default Guardrails

Set default guardrails in `variables.yaml`. These apply to every agent that does **not** explicitly configure its own guardrail list:

```yaml
Variables:
  AI:
    Guardrails:
      # Comma-separated list of guardrail names
      DefaultInput: PromptInjectionInputGuardrail
      DefaultOutput: SensitiveDataOutputGuardrail
```

## Using Guardrails in Agents

In the agent configuration, specify guardrails as a String array:

```java
// Input guardrails
["PromptInjectionInputGuardrail", "MyCustomInputGuardrail"]

// Output guardrails
["SensitiveDataOutputGuardrail", "MyCustomOutputGuardrail"]
```

If no guardrails are specified, the agent uses the default guardrails from `variables.yaml`.

## Implementing Custom Guardrails

### Custom Input Guardrail

1. Create a class implementing `SmartWorkflowInputGuardrail`:

```java
package com.example.guardrails;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

public class MyCustomInputGuardrail implements SmartWorkflowInputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    if (containsBadContent(message)) {
      return GuardrailResult.block("Message contains bad content");
    }
    return GuardrailResult.allow();
  }

  private boolean containsBadContent(String message) {
    // Your validation logic
    return false;
  }
}
```

### Custom Output Guardrail

1. Create a class implementing `SmartWorkflowOutputGuardrail`:

```java
package com.example.guardrails;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

public class MyCustomOutputGuardrail implements SmartWorkflowOutputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    if (containsSensitiveData(message)) {
      return GuardrailResult.block("Response contains sensitive data");
    }
    return GuardrailResult.allow();
  }

  private boolean containsSensitiveData(String message) {
    // Your validation logic
    return false;
  }
}
```

### Register Guardrails via Provider

2. Create a `GuardrailProvider` to provide your custom guardrails:

> **Important:** Your project must register a `GuardrailProvider` via SPI for Smart Workflow to discover and load your custom guardrails. Without a registered provider, your guardrails will not be available to agents.

```java
package com.example.guardrails;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;

public class MyGuardrailProvider implements GuardrailProvider {

  @Override
  public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
    return List.of(new MyCustomInputGuardrail());
  }

  @Override
  public List<SmartWorkflowOutputGuardrail> getOutputGuardrails() {
    return List.of(new MyCustomOutputGuardrail());
  }
}
```

3. Register the provider in `src/META-INF/services/com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider`:

   ```text
   com.example.guardrails.MyGuardrailProvider
   ```

   This SPI registration is **required** for Smart Workflow to discover and load your guardrails. The provider will be automatically loaded when agents request guardrails by name.

## Guardrail Observability

Smart Workflow records guardrail executions for both governance audit and external telemetry.

### Ivy History Recording

When `AI.Observability.Ivy.Enabled` is set to `true`, every guardrail execution is recorded in the agent conversation history. Each record includes:

- **guardrailName** - The guardrail class name
- **type** - `INPUT` or `OUTPUT`
- **result** - `SUCCESS`, `FAILURE`, or `FATAL`
- **message** - The validated content (user query for input guardrails, AI response for output guardrails)
- **failureMessage** - The reason when a guardrail blocks (null on success)
- **durationMs** - Execution time in milliseconds
- **executedAt** - Timestamp of execution

Guardrail records are stored alongside tool executions in the `AgentConversationEntry` and are visible in the agent history tree.

### OpenInference Tracing (Arize Phoenix)

When `AI.Observability.Openinference.Enabled` is set to `true`, each guardrail execution produces a dedicated span with `openinference.span.kind = "GUARDRAIL"`. These spans appear in Arize Phoenix alongside the LLM spans, providing a complete trace of the AI interaction including safety checks.

Guardrail span attributes:

| Attribute | Description |
|-----------|-------------|
| `openinference.span.kind` | `GUARDRAIL` |
| `validator_name` | The guardrail class name (Phoenix convention) |
| `validator_on_fail` | Behavior on failure — always `"exception"` |
| `guardrail.type` | `INPUT` or `OUTPUT` |
| `guardrail.result` | `SUCCESS`, `FAILURE`, or `FATAL` |
| `guardrail.failure_message` | Failure reason (present only when blocked) |
| `input.value` | The validated content (user query or AI response) |
| `output.value` | `"pass"` or `"fail"` (Phoenix convention) |

## Handling Guardrail Errors

When a guardrail blocks the input or output, an exception is thrown with the error code `smartworkflow:guardrail:input:violation` or `smartworkflow:guardrail:output:violation`. You can handle this using an Error Boundary Event:

1. Add an **Error Boundary Event** to your `AgenticProcessCall` element.
2. Configure it to catch the error code: `smartworkflow:guardrail:input:violation` or `smartworkflow:guardrail:output:violation`.
3. Implement your error handling logic (e.g., display a user-friendly message, log the incident, retry with different input).

For a working example, see the `GuardrailDemo` process in the `smart-workflow-demo` project.
