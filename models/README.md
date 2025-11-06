# Chat Models

In this directory we maintain projects that supply a ChatModelProvider.

## Contributing

We are open to support more ChatModels from any provider.
If you miss your favourite one, simply contribute it to this space.

Call the directory `models/smart-workflow-PROVIDER`, replacing PROVIDER with your concrete vendor.
For the project coordinates, please align to our existing workspace:

```xml
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-PROVIDER</artifactId>
  <packaging>iar</packaging>
```

## Variables

Every provider has its own set of variables. Please contribute your ChatModel provider variables to.

```yaml
Variables:
  AI:
    Providers:
      PROVIDER:
        #[password]
        APIKey: ${decrypt:}
        ...
```

Furthermore, please enrich the global enumeration of available providers [variables.yaml](../smart-workflow/config/variables.yaml) to list your provider.
See the enumeration called `AI.defaultProvider`.

## Libraries

Smart-workflow providers are built upon existing LangChain4j providers.
Please exclude dependencies from your pom.xml, which are already part of smart-workflow.
Classically this will be the 'langchain4j-core' and 'langchain4j-http-client'
