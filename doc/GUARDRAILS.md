# Guardrails

Guardrails protect AI agents by validating user input before it reaches the model. Smart Workflow includes a built-in `PromptInjectionGuardrail` that blocks common prompt injection attacks.

## Configuring Default Guardrails

Set default guardrails in `variables.yaml`:

```yaml
Variables:
  AI:
    Guardrails:
      # Comma-separated list of guardrail names
      DefaultInput: PromptInjectionGuardrail
```

## Using Guardrails in Agents

In the agent configuration, specify guardrails as a String array:

```java
["PromptInjectionGuardrail", "MyCustomGuardrail"]
```

If no guardrails are specified, the agent uses the default guardrails from `variables.yaml`.

## Implementing Custom Guardrails

1. Create a class implementing `SmartWorkflowInputGuardrail`:

```java
package com.example.guardrails;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

public class MyCustomGuardrail implements SmartWorkflowInputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    if (containsSensitiveData(message)) {
      return GuardrailResult.block("Message contains sensitive data");
    }
    return GuardrailResult.allow();
  }

  private boolean containsSensitiveData(String message) {
    // Your validation logic
    return false;
  }
}
```

2. Register the guardrail in `src/META-INF/services/com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail`:

```
com.example.guardrails.MyCustomGuardrail
```

## Handling Guardrail Errors

When a guardrail blocks the input, an `InputGuardrailException` is thrown with the error code `smartworkflow:guardrail:violation`. You can handle this using an Error Boundary Event:

1. Add an **Error Boundary Event** to your `AgenticProcessCall` element.
2. Configure it to catch the error code: `smartworkflow:guardrail:violation`.
3. Implement your error handling logic (e.g., display a user-friendly message, log the incident, retry with different input).

For a working example, see the `GuardrailDemo` process in the `smart-workflow-demo` project.
