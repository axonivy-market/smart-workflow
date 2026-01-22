# Smart Workflow Features

## Callable Smart Workflow Agent

The Smart Workflow Agent is available as a **callable subprocess** that can be invoked from any Ivy project without requiring a direct dependency on the `smart-workflow` project. This allows developers to integrate AI agent capabilities into their applications with minimal coupling.

### Subprocess Signature

```
invokeAgent(String, String, List<String>, Class)
```

### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `query` | `String` | The user query/message to send to the AI agent |
| `systemMessage` | `String` | System message to define agent behavior |
| `tools` | `List<String>` | List of tool names available to the agent. Keep empty to use all tools |
| `resultType` | `Class` | Expected result type class for structured output. Keep `null` for String result |

### Return Values

| Parameter | Type | Description |
|-----------|------|-------------|
| `resultObject` | `Object` | The AI agent response result |
| `guardrailViolation` | `String` | Guardrail violation error message (if any) |

### Usage Example

Below is an example method demonstrating how to invoke the Smart Workflow Agent:

```java
public static Object invokeAgent(String query, String systemMessage, List<String> tools, Class<?> resultType) {
  Map<String, Object> params = new HashMap<>();
  params.put("query", query);
  params.put("systemMessage", systemMessage);
  params.put("tools", tools);
  params.put("resultType", resultType);

  Map<String, Object> response = IvyAdapterService.startSubProcessInSecurityContext(
      "invokeAgent(String,String,List<String>,Class)", params);

  // Check for guardrail violations
  if (response != null && response.get("guardrailViolation") != null) {
    String error = (String) response.get("guardrailViolation");
    Ivy.log().error("Smart Workflow Agent Guardrail Violation: " + error);
    return null;
  }

  // Return the result object if available and matches expected type
  if (response != null && response.get("resultObject") != null) {
    Object resultObject = response.get("resultObject");
    if (resultType.isInstance(resultObject)) {
      return resultType.cast(resultObject);
    }
  }
  return null;
}
```

### Basic Usage

```java
Object result = SmartWorkflowUtils.invokeAgent(
    "Create a summary of pending approvals",
    "You are a workflow assistant specialized in approvals",
    List.of("findTasks", "getTaskDetails"),  // specific tools only
    ApprovalSummary.class                     // custom result type
);
```

### Error Handling

The callable subprocess includes a boundary error event for guardrail violations with error code:
```
smartworkflow:guardrail:violation
```

When a guardrail violation occurs, the `guardrailViolation` return parameter contains the error message, and `resultObject` will be null.
