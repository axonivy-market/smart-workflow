# Guardrails

Guardrails protect AI agents by validating both user input and AI output. Smart Workflow provides built-in guardrails for common safety concerns and supports custom guardrails via SPI.

**Built-in Guardrails:**

| Guardrail | Type | Description |
|-----------|------|-------------|
| `PromptInjectionInputGuardrail` | Input | Blocks common prompt injection attacks |
| `SensitiveDataOutputGuardrail` | Output | Blocks responses containing API keys or private keys |

## Configuring Default Guardrails

Set default guardrails in `variables.yaml`:

```yaml
Variables:
  AI:
    Guardrails:
      # Comma-separated list of input guardrail names
      DefaultInput: PromptInjectionInputGuardrail
      # Comma-separated list of output guardrail names
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

## Handling Guardrail Errors

When a guardrail blocks the input or output, an exception is thrown with the error code `smartworkflow:guardrail:input:violation` or `smartworkflow:guardrail:output:violation`. You can handle this using an Error Boundary Event:

1. Add an **Error Boundary Event** to your `AgenticProcessCall` element.
2. Configure it to catch the error code: `smartworkflow:guardrail:input:violation` or `smartworkflow:guardrail:output:violation`.
3. Implement your error handling logic (e.g., display a user-friendly message, log the incident, retry with different input).

For a working example, see the `GuardrailDemo` process in the `smart-workflow-demo` project.
