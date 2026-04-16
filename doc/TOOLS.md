# Defining Tools

AI agents in Smart Workflow use tools to take action. A tool is a named, callable unit of logic that the agent discovers, selects, and invokes at runtime. Smart Workflow supports two kinds of tools.

---

## Callable Process Tools

We strongly encourage using callable subprocesses as tools. This approach aligns naturally with how Ivy developers already work and provides full access to the power of the process designer—such as error handling, dialogs, subprocess calls, and other Axon Ivy capabilities.

You can turn any callable subprocess into a tool by simply adding the `tool` tag.

**Steps:**

1. Create a callable sub-process in your Axon Ivy project.
2. Add the tag `tool` to the process.
3. Write a clear `description` — this is what the agent reads to decide whether to call the tool.

![Tool configurations](../smart-workflow-product/img/tool-configurations.png)

---

## Java Tools

For advanced use cases, tool logic can also be implemented directly in Java. This is rarely needed — prefer callable processes whenever possible. Consider Java Tools only when the logic has no workflow steps and is better expressed as a plain Java class.

### Step 1 — Implement `SmartWorkflowTool`

```java
public class MyTool implements SmartWorkflowTool {

  @Override
  public String name() {
    return "myTool"; // name the agent uses to call this tool
  }

  @Override
  public String description() {
    return "Describe what this tool does and when the agent should use it.";
  }

  @Override
  public List<ToolParameter> parameters() {
    return List.of(
        new ToolParameter("paramName", "description of this param", "java.lang.String")
    );
  }

  @Override
  public Object execute(Map<String, Object> args) {
    String value = (String) args.get("paramName");
    // ... your logic
    return result;
  }
}
```

The type is a string identifying the Java type. The following kinds are supported:

| Kind | Example |
| --- | --- |
| Primitive | `"int"`, `"boolean"`, `"double"` |
| Java class | `"java.lang.String"`, `"com.example.MyClass"` |
| List | `"java.util.List<java.lang.String>"`, `"java.util.List<com.example.MyClass>"` |

Arrays are not supported — use `List` instead.

The framework deserializes the agent's JSON arguments into the declared Java type automatically.

### Step 2 — Create a `SmartWorkflowToolsProvider`

Group one or more tools in a provider class:

```java
public class MyToolProvider implements SmartWorkflowToolsProvider {
  @Override
  public List<SmartWorkflowTool> getTools() {
    return List.of(new MyTool());
  }
}
```

### Step 3 — Register via SPI

Create the file `src/META-INF/services/com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider` and the tool provider:

```text
com.example.MyToolProvider
```

The framework loads all registered providers at startup.

---

## Demo: TaxCalculatorTool

[`TaxCalculatorTool`](../smart-workflow-demo/src/com/axonivy/utils/smart/workflow/demo/tool/TaxCalculatorTool.java) shows a complete Java Tool that accepts a structured `Invoice` object and returns per-item tax calculations.

Key points from the demo:

- Uses a custom type (`com.axonivy.utils.ai.Invoice`) as a parameter — the framework deserializes it automatically from the agent's JSON call.
- Returns a typed result record (`TaxCalculationResult`) which the framework serializes back to the agent as JSON.
- Registered in [`DemoToolProvider`](../smart-workflow-demo/src/com/axonivy/utils/smart/workflow/demo/tool/DemoToolProvider.java) via SPI.
