# Custom Guardrails

Beyond the [built-in guardrails](../operate/guardrails.md), you can implement your own — a domain rule, a compliance check, a redaction pass. A custom guardrail is a Java class discovered through SPI; once registered, its name appears in the agent element's guardrail pickers like any built-in.

The `BlockCompetitorMentionGuardrail` in the demo project is a complete worked example: a company policy that agents must never mention competitor products, enforced in one place instead of in every system prompt.

## The guardrail contract

Input and output guardrails share one interface; the two sub-interfaces are markers that only say which list a guardrail belongs in.

```java
public interface SmartWorkflowGuardrail {
  GuardrailResult evaluate(String message);

  default GuardrailResult evaluate(String message, String invocationId) {
    return evaluate(message);
  }

  default String name() {
    return getClass().getSimpleName();
  }
}
```

Implement the single-argument `evaluate` for a stateless check. Override the two-argument form only if you need to correlate the input and output halves of the same agent call — that is how `PiiMaskingGuardrail` pairs its masking with its restoration.

`GuardrailResult` offers four outcomes:

| Factory | Effect |
| --- | --- |
| `allow()` | Pass the message through unchanged. |
| `allowWithRewrite(String)` | Pass through, replacing the message with your version. Use for redaction or normalization rather than rejection. |
| `block(String reason)` | Reject, with the reason surfaced in the BPM error. |
| `block(String reason, Throwable cause)` | Reject, attaching a cause. The cause travels through the LangChain4j guardrail exception, letting callers distinguish *which* guardrail blocked without inspecting the reason text. |

## 1. Write the guardrail

An input guardrail implements `SmartWorkflowInputGuardrail`:

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

An output guardrail is the same shape against `SmartWorkflowOutputGuardrail`:

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

## 2. Group them in a provider

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

## 3. Register the provider via SPI

Create `src/META-INF/services/com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider`:

```text
com.example.guardrails.MyGuardrailProvider
```

> **Important:** This registration is required. Without a registered `GuardrailProvider`, Smart Workflow never discovers your guardrails and they do not appear in the pickers.

> **Note:** Only the **first line** of a services file is read. To register two providers, use two files — a second class name in the same file is silently ignored.

## 4. Use it

The guardrail's `name()` — the simple class name unless you override it — now appears in the `Input guardrails:` or `Output guardrails:` picker on any agent element. To apply it everywhere, add the name to `AI.Guardrails.DefaultInput` or `AI.Guardrails.DefaultOutput` in `variables.yaml`.

## Common mistakes

- **No SPI registration.** The class compiles, the guardrail never runs, and nothing warns you. This is the usual cause.
- **Two providers in one services file.** Only the first line is read.
- **Blocking on the reason text downstream.** Callers should branch on the error code, or on a typed cause passed to `block(reason, cause)`.
- **Expecting an output guardrail to retry.** It does not — the first block discards the whole response.
- **Holding state in a guardrail instance.** The instance is shared across all agents and concurrent calls. If you need per-call state, key it on the `invocationId` from the two-argument `evaluate`.

## See also

- [Guardrails](../operate/guardrails.md) — the built-ins and how they are configured
- [Java API](../reference/java-api.md) — the full interface reference
- [Error Codes](../reference/error-codes.md) — what a block turns into

For a working example, see the custom-guardrail start links in the [`GuardrailDemo`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-demo/process/Features/GuardrailDemo.p.json) process.
