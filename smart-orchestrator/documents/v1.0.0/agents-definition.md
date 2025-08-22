# Agents Definition in Axon Ivy

> **Key Architectural Note**: The `goal` field is **IvyAgent-specific only**. TodoAgent does not use or require a goal field as its implicit goal is to complete all todos successfully.

## Agent Types: Conceptual Level of Agents in Axon Ivy

Axon Ivy provides two main conceptual levels of agents for automation and process orchestration, as defined in the `AgentType` enum:

### Steps-by-Step Agents (`STEP_BY_STEP`)

- **Implementation Class**: `IvyAgent`
- **Execution Strategy**: Sequential step execution with planned sequences
- **Use Cases**:
  - Process automation with strict dependencies between steps
  - Workflows where each step builds on the previous step's results
  - Scenarios requiring structured, predictable execution paths
- **Key Characteristics**: Uses `AiStep` objects that execute in a predefined order with adaptive reasoning capabilities

### Todo-List Agents (`TODO_LIST`)

- **Implementation Class**: `TodoAgent`
- **Execution Strategy**: Outcome-focused todo execution with completion criteria
- **Use Cases**:
  - Goal-oriented tasks where the path to completion may vary
  - Flexible workflows that can adapt based on intermediate results
  - Tasks where success is measured by outcomes rather than specific steps
- **Key Characteristics**: Uses `AiTodo` objects with success criteria and iterative execution until completion

## Agent Configuration via JSON File

Agents are configured using JSON files stored in the `AI.Agents` variable. The configuration follows the `AgentModel` class structure:

#### Base Configuration Structure

All agents share these common configuration fields:

```json
{
  "id": "unique-agent-identifier",
  "name": "Human-readable agent name",
  "usage": "Description of agent's intended use",
  "agentType": "STEP_BY_STEP | TODO_LIST",
  "maxIterations": 15,
  "planningModel": "gpt-4o",
  "planningModelKey": "${AI.OpenAI.APIKey}",
  "executionModel": "gpt-4o-mini",
  "executionModelKey": "${AI.OpenAI.APIKey}",
  "tools": ["tool-id-1", "tool-id-2"],
  "instructions": [
    {
      "type": "planning",
      "content": "Planning-specific instruction"
    },
    {
      "type": "execution",
      "content": "Execution-specific instruction"
    }
  ]
}
```

#### IvyAgent (STEP_BY_STEP) Configuration

```json
{
  "id": "unique-agent-identifier",
  "name": "Human-readable agent name",
  "usage": "Description of agent's intended use",
  "goal": "Primary objective of the agent",
  "agentType": "STEP_BY_STEP",
  "maxIterations": 15,
  "planningModel": "gpt-4o",
  "planningModelKey": "${AI.OpenAI.APIKey}",
  "executionModel": "gpt-4o-mini",
  "executionModelKey": "${AI.OpenAI.APIKey}",
  "tools": ["tool-id-1", "tool-id-2"],
  "instructions": [
    {
      "type": "planning",
      "content": "Planning-specific instruction"
    },
    {
      "type": "execution",
      "content": "Execution-specific instruction"
    }
  ]
}
```

#### TodoAgent (TODO_LIST) Configuration

```json
{
  "id": "unique-agent-identifier",
  "name": "Human-readable agent name",
  "usage": "Description of agent's intended use",
  "agentType": "TODO_LIST",
  "maxIterations": 15,
  "planningModel": "gpt-4o",
  "planningModelKey": "${AI.OpenAI.APIKey}",
  "executionModel": "gpt-4o-mini",
  "executionModelKey": "${AI.OpenAI.APIKey}",
  "tools": ["tool-id-1", "tool-id-2"],
  "instructions": [
    {
      "type": "planning",
      "content": "Planning-specific instruction"
    },
    {
      "type": "execution",
      "content": "Execution-specific instruction"
    }
  ]
}
```

## Abstract Agent

### Code Explanation of Abstract Agent

The `BaseAgent` class serves as the foundation for all agent implementations in Axon Ivy:

```java
public abstract class BaseAgent {
  protected static final int DEFAULT_MAX_ITERATIONS = 20;
  
  // Core identification
  protected String id;
  protected String name;
  protected String usage;
  
  // Execution control
  protected int maxIterations = DEFAULT_MAX_ITERATIONS;
  
  // Dual AI Model Architecture
  protected AbstractAiServiceConnector planningModel;  // For plan generation
  protected AbstractAiServiceConnector executionModel; // For step execution
  
  // Instruction System
  protected List<Instruction> instructions;
  
  // Tools and variables
  protected List<IvyTool> tools;
  protected List<AiVariable> variables;
  protected List<AiVariable> results;
  
  // Execution tracking
  protected List<String> observationHistory;
  protected String originalQuery;
  protected HistoryLog historyLog;
  
  // Template methods - must be implemented by subclasses
  public abstract void start(String query);
  public abstract void execute();
  
  // Configuration loading
  public void loadFromModel(AgentModel model) {
    // Loads configuration from AgentModel
    // Initializes planning and execution models
    // Sets up tools and instructions
    // Note: goal field is loaded by IvyAgent subclass only
  }
  
  // Helper method to filter instructions by type
  protected List<String> getInstructionsByType(InstructionType type) {
    return instructions.stream()
        .filter(instruction -> instruction.getType() == type)
        .map(Instruction::getContent)
        .collect(Collectors.toList());
  }
}
```

### Fields in Agent Configuration

#### Core Fields (All Agent Types)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | Yes | Unique identifier for the agent instance |
| `name` | String | Yes | Human-readable name for the agent |
| `usage` | String | No | Description of agent's intended use |
| `agentType` | AgentType | No | Type of agent (defaults to STEP_BY_STEP) |
| `maxIterations` | Integer | No | Maximum execution iterations (default: 20) |
| `planningModel` | String | No | AI model name for planning phase |
| `planningModelKey` | String | No | API key for planning model. Accepts: <br/>• **Normal string**: Direct API key value <br/>• **Variable reference**: `${variable.path}` format to load from Axon Ivy variables.yaml (e.g., `${AI.OpenAI.APIKey}`) |
| `executionModel` | String | No | AI model name for execution phase |
| `executionModelKey` | String | No | API key for execution model. Accepts: <br/>• **Normal string**: Direct API key value <br/>• **Variable reference**: `${variable.path}` format to load from Axon Ivy variables.yaml (e.g., `${AI.OpenAI.APIKey}`) |
| `tools` | Array | Yes | List of tool IDs available to the agent. Contains the IDs of usable tools that the agent can invoke during execution. Tools must be pre-configured in the system and their IDs must match existing tool definitions |
| `instructions` | Array | No | List of instruction objects with type and content. Two instruction types affect different phases: <br/>• **`planning`**: Instructions that guide the AI during plan generation phase <br/>• **`execution`**: Instructions that guide the AI during step/todo execution phase |

#### IvyAgent-Specific Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `goal` | String | **Yes (IvyAgent only)** | Primary objective of the agent. This field critically affects both the planning and execution phases - it guides the AI in generating appropriate plans and making execution decisions aligned with the desired outcome. **Not used by TodoAgent** - TodoAgent's goal is implicit (complete all todos) |

## Step-by-Step Agent

### Introduction

The `IvyAgent` class implements step-by-step execution with adaptive reasoning capabilities. The agent follows a sophisticated workflow that combines upfront planning with dynamic adaptation during execution.

**Detailed Workflow:**

1. **Planning Phase** (`start()` method):
   - Receives user query and creates input variable
   - Builds enhanced planning prompt including goal, planning instructions, and available tools
   - Uses `planningModel` to generate high-level execution plan
   - Converts crude plan into structured `AiStep` objects using `DataMapping` and `executionModel`
   - Assigns specific tools to each step based on `toolId`

2. **Execution Phase** (`execute()` method):
   - Initializes execution history and logs input variables with goal
   - Enters main execution loop with iteration limit (`maxIterations`)
   - For each step:
     - **Step Execution**: Runs current step using assigned tool and `executionModel`
     - **Result Integration**: Adds step results to global variables list
     - **ReAct Reasoning**: Performs adaptive reasoning using execution instructions
     - **Decision Making**: AI analyzes progress and decides next action

3. **ReAct Reasoning Process**:
   - Builds reasoning prompt with goal, execution instructions, current situation, and observation history
   - Uses `EXECUTION_PROMPT_TEMPLATE` for structured analysis
   - AI makes decision: `YES` (update plan), `NO` (continue with plan), or `COMPLETE` (goal achieved)
   - **Plan Adaptation**: Can dynamically create new steps or modify execution path
   - **Adaptive Steps**: Creates high-numbered steps (stepNo + 1000) to avoid conflicts

4. **Completion**:
   - Stops when AI determines goal is achieved or reaches final step (`FINALIZE_STEP`)
   - Logs execution completion with reasoning

Key characteristics:

- **Adaptive Planning**: Dynamic plan modification based on intermediate results
- **ReAct Methodology**: Observation → Reasoning → Action cycle between steps
- **Dual AI Models**: Separate models for planning and execution phases
- **Instruction-Guided**: Uses planning and execution instructions to guide AI behavior

### Code Explanation

```java
public class IvyAgent extends BaseAgent {
  
  // Execution template for adaptive reasoning
  private static final String EXECUTION_PROMPT_TEMPLATE = """
      GOAL: {{goal}}
      
      {{executionInstructions}}CURRENT SITUATION:
      Original Query: {{originalQuery}}
      Current Step: {{currentStepName}}
      Latest Result: {{latestResult}}
      
      ANALYSIS REQUIRED:
      1. Is the goal achieved?
      2. Should we continue with current plan or adapt it?
      3. Are there execution instruction triggers?
      
      Decision: [YES - update plan | NO - continue | COMPLETE - goal achieved]
      """;

  // Ordered list of steps
  private List<AiStep> steps;

  @Override
  public void start(String query) {
    this.originalQuery = query;
    this.observationHistory = new ArrayList<>();
    
    // Create input variable
    AiVariable inputVariable = new AiVariable();
    inputVariable.setName("query");
    inputVariable.setContent(query);
    getVariables().add(inputVariable);

    // Generate plan using planning model
    Planning.Builder planningBuilder = Planning.getBuilder()
        .addTools(tools)
        .useService(planningModel)
        .withQuery(buildPlanningPrompt(query));
    
    // Add planning instructions
    List<String> planningInstructions = getInstructionsByType(InstructionType.PLANNING);
    for (String instruction : planningInstructions) {
      planningBuilder.addCustomInstruction(instruction);
    }
    
    String crudePlan = planningBuilder.build().execute().getContent();

    // Convert plan to AiStep objects using DataMapping
    String stepString = DataMapping.getBuilder()
        .useService(executionModel)
        .withObject(new AiStep())
        .addFieldExplanations(Arrays.asList(
            new FieldExplanation("stepNo", "Incremental integer, starts at 1"),
            new FieldExplanation("name", "Name of the step"),
            new FieldExplanation("analysis", "Analysis of the step"),
            new FieldExplanation("toolId", "Tool ID to execute the step"),
            new FieldExplanation("next", "ID of next step, -1 if final"),
            new FieldExplanation("previous", "ID of previous step, 0 if initial"),
            new FieldExplanation("resultName", "Expected result name"),
            new FieldExplanation("resultDescription", "Expected result description")
        ))
        .withQuery(crudePlan)
        .asList(true)
        .build()
        .execute()
        .getContent();

    List<AiStep> plannedSteps = BusinessEntityConverter.jsonValueToEntities(stepString, AiStep.class);

    // Assign tools to steps
    steps = new ArrayList<>();
    for (AiStep step : plannedSteps) {
      IvyTool tool = tools.stream()
          .filter(t -> t.getId().equals(step.getToolId()))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + step.getToolId()));
      step.useTool(tool);
      steps.add(step);
    }

    execute();
  }

  @Override
  public void execute() {
    // Execute steps with adaptive reasoning
    // Monitor progress and adjust plan if needed
    // Log execution history
  }
}
```

Fields in Agent Configuration

**AgentModel Fields (specific to Step-by-Step)**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `agentType` | AgentType | No | Must be `STEP_BY_STEP` |
| `instructions` | Array | No | Planning and execution instructions |

**AiStep Object Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `stepNo` | Integer | Yes | Step sequence number (starts at 1) |
| `name` | String | Yes | Descriptive name of the step |
| `analysis` | String | No | Analysis or reasoning for this step |
| `toolId` | String | Yes | ID of the tool to execute this step |
| `previous` | Integer | No | Step number of previous step (0 if initial) |
| `next` | Integer | No | Step number of next step (-1 if final) |
| `resultName` | String | No | Expected name of the result variable |
| `resultDescription` | String | No | Description of expected result |

**Configuration Example**:

```json
{
  "id": "support-agent-steps",
  "name": "Step-by-Step Support Agent",
  "usage": "Creates support tickets using sequential step execution",
  "goal": "Create support ticket and assign approvers efficiently",
  "agentType": "STEP_BY_STEP",
  "maxIterations": 15,
  "planningModel": "gpt-4o",
  "planningModelKey": "${AI.OpenAI.APIKey}",
  "executionModel": "gpt-4o-mini",
  "executionModelKey": "${AI.OpenAI.APIKey}",
  "tools": ["create-support-task", "create-support-ticket", "choose-ticket-approvers"],
  "instructions": [
    {
      "type": "planning",
      "content": "Create detailed step-by-step plan to handle support request"
    },
    {
      "type": "execution",
      "content": "Execute each step in planned sequence and verify completion"
    }
  ]
}
```

## Todo List Agent

### Introduction

The `TodoAgent` class implements outcome-focused execution where tasks are defined by their success criteria rather than rigid steps. The agent uses a goal-driven approach with continuous assessment of progress and completion.

**Detailed Workflow:**

1. **Todo Planning Phase** (`start()` method):
   - Receives user query and creates input variable
   - Builds todo planning prompt with goal, planning instructions, and available tools
   - Uses `planningModel` to generate structured todo list via `DataMapping`
   - Each todo includes description, success criteria, available tool IDs, and max iterations
   - Sets default `maxIterationsPerTodo` (5) if not specified

2. **Sequential Todo Execution** (`execute()` method):
   - Initializes execution history and logs input variables with goal
   - Executes todos sequentially with global iteration limit (`maxIterations`)
   - For each todo:
     - **Individual Todo Execution**: Calls `executeTodo()` method
     - **Result Integration**: Adds todo results to global variables
     - **Goal Assessment**: Evaluates if overall goal is achieved after each todo
     - **Early Termination**: Stops if goal achieved, otherwise continues to next todo

3. **Individual Todo Execution Process** (`executeTodo()`):
   - While todo not completed and hasn't reached `maxIterationsPerTodo`:
     - **Execution**: Calls `todo.execute()` with current variables, tools, and `executionModel`
     - **Result Logging**: Records execution result in observation history
     - **Completion Assessment**: Uses AI-powered `todo.assessCompletion()` method
     - **Iteration Control**: Continues until completion or max iterations reached

4. **AI-Powered Assessments**:
   - **Todo Completion**: Uses `COMPLETION_ASSESSMENT_TEMPLATE` to evaluate individual todo success
   - **Goal Achievement**: Uses `GOAL_ASSESSMENT_TEMPLATE` to assess overall goal completion
   - **Decision Making**: AI analyzes completed todos and current variables to determine next actions

5. **Completion**:
   - Stops when overall goal is achieved or all todos are processed
   - Performs final goal verification and logs completion status

Key characteristics:

- **Success Criteria Driven**: Each todo has specific, measurable completion criteria
- **Flexible Tool Selection**: Todos can use multiple tools based on `availableToolIds`
- **Iterative Refinement**: Individual todos can iterate until success criteria are met
- **AI-Powered Assessment**: Continuous evaluation of todo completion and overall goal achievement
- **Adaptive Execution**: Can achieve goals through different paths based on intermediate results

### Code Explanation

```java
public class TodoAgent extends BaseAgent {

  private static final String TODO_PLANNING_TEMPLATE = """
      GOAL: {{goal}}
      
      {{planningInstructions}}USER QUERY: {{query}}
      
      Available tools: {{availableTools}}
      
      TASK: Create todo list to achieve the goal. Each todo should be outcome-focused 
      with clear success criteria.
      """;

  private static final String GOAL_ASSESSMENT_TEMPLATE = """
      GOAL: {{goal}}
      COMPLETED TODOS: {{completedTodos}}
      CURRENT VARIABLES: {{currentVariables}}
      
      Has the goal been achieved?
      Decision: [GOAL_ACHIEVED | CONTINUE_NEEDED]
      """;

  private List<AiTodo> todos;
  private AiTodo currentTodo;
  private int currentTodoIndex = 0;

  @Override
  public void start(String query) {
    this.originalQuery = query;
    
    // Generate todo list using planning model
    String todoString = DataMapping.getBuilder()
        .useService(planningModel)
        .withObject(new AiTodo())
        .addFieldExplanations(Arrays.asList(
            new FieldExplanation("description", "What needs to be accomplished"),
            new FieldExplanation("successCriteria", "How to determine completion"),
            new FieldExplanation("availableToolIds", "Tool IDs that can work on this todo"),
            new FieldExplanation("analysis", "Why this todo is necessary"),
            new FieldExplanation("stepNo", "Sequential number starting from 1"),
            new FieldExplanation("resultName", "Expected result name"),
            new FieldExplanation("resultDescription", "What result should contain"),
            new FieldExplanation("maxIterationsPerTodo", "Max iterations per todo (default 5)")
        ))
        .withQuery(buildTodoPlanningPrompt(query))
        .asList(true)
        .build()
        .execute()
        .getContent();

    List<AiTodo> plannedTodos = BusinessEntityConverter.jsonValueToEntities(todoString, AiTodo.class);
    
    // Initialize todos with defaults
    todos = new ArrayList<>();
    for (AiTodo todo : plannedTodos) {
      if (todo.getMaxIterationsPerTodo() <= 0) {
        todo.setMaxIterationsPerTodo(5);
      }
      todos.add(todo);
    }

    execute();
  }

  @Override
  public void execute() {
    // Execute todos sequentially
    for (int i = 0; i < todos.size(); i++) {
      currentTodo = todos.get(i);
      
      // Execute todo until completion or max iterations
      boolean completed = executeTodo(currentTodo);
      
      if (completed && isGoalAchieved()) {
        break; // Goal achieved, stop execution
      }
    }
  }

  private boolean executeTodo(AiTodo todo) {
    while (!todo.isCompleted() && !todo.hasReachedMaxIterations()) {
      // Execute todo with current variables and tools
      List<AiVariable> results = todo.execute(getVariables(), tools, executionModel);
      
      // Assess completion using AI
      String resultStr = BusinessEntityConverter.entityToJsonValue(results);
      boolean completed = todo.assessCompletion(resultStr, executionModel);
      
      if (completed) return true;
    }
    return todo.isCompleted();
  }
}
```

### Fields in Agent Configuration

**AgentModel Fields (specific to Todo List)**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `agentType` | AgentType | No | Must be `TODO_LIST` |
| `instructions` | Array | No | Planning and execution instructions |

**AiTodo Object Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | String | Yes | What needs to be accomplished |
| `successCriteria` | String | Yes | Criteria for determining completion |
| `availableToolIds` | Array | No | List of tool IDs that can work on this todo |
| `analysis` | String | No | Why this todo is necessary |
| `stepNo` | Integer | Yes | Sequential number starting from 1 |
| `resultName` | String | No | Expected result name |
| `resultDescription` | String | No | Description of what result should contain |
| `maxIterationsPerTodo` | Integer | No | Maximum iterations for this todo (default: 5) |
| `isCompleted` | Boolean | No | Current completion status (runtime field) |
| `currentStatus` | String | No | Current status: pending/in_progress/completed/failed |

**Configuration Example**:

```json
{
  "id": "support-agent-todos",
  "name": "Todo-based Support Agent",
  "usage": "Creates support tickets using outcome-focused todo execution",
  "agentType": "TODO_LIST",
  "maxIterations": 15,
  "planningModel": "gpt-4o",
  "planningModelKey": "${AI.OpenAI.APIKey}",
  "executionModel": "gpt-4o-mini",
  "executionModelKey": "${AI.OpenAI.APIKey}",
  "tools": ["create-support-task", "create-support-ticket", "choose-ticket-approvers"],
  "instructions": [
    {
      "type": "planning",
      "content": "Create outcome-focused todos with clear success criteria"
    },
    {
      "type": "execution",
      "content": "Work on each todo until success criteria are fully met"
    }
  ]
}
```
