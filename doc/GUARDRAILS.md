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

2. Create a `GuardrailProvider` to provide your custom guardrails:

> **Important:** Your project must register a `GuardrailProvider` via SPI for Smart Workflow to discover and load your custom guardrails. Without a registered provider, your guardrails will not be available to agents.

```java
package com.example.guardrails;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;

public class MyGuardrailProvider implements GuardrailProvider {

  @Override
  public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
    return List.of(
      new MyCustomGuardrail(),
      new AnotherCustomGuardrail()
    );
  }
}
```

3. Register the provider in `src/META-INF/services/com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider`:

   ```text
   com.example.guardrails.MyGuardrailProvider
   ```

   This SPI registration is **required** for Smart Workflow to discover and load your guardrails. The provider will be automatically loaded when agents request guardrails by name.

## Handling Guardrail Errors

When a guardrail blocks the input, an `InputGuardrailException` is thrown with the error code `smartworkflow:guardrail:violation`. You can handle this using an Error Boundary Event:

1. Add an **Error Boundary Event** to your `AgenticProcessCall` element.
2. Configure it to catch the error code: `smartworkflow:guardrail:violation`.
3. Implement your error handling logic (e.g., display a user-friendly message, log the incident, retry with different input).

For a working example, see the `GuardrailDemo` process in the `smart-workflow-demo` project.
