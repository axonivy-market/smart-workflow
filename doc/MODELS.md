# Chat Models

In the `models` directory we maintain projects that supply a ChatModelProvider.

## Contributing

We are open to support more ChatModels from any provider.
If you miss your favourite one, simply contribute it to this space.

Create a directory `models/smart-workflow-PROVIDER`, replacing PROVIDER with your concrete vendor.
For the project coordinates, please align to our existing workspace:

```xml
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-PROVIDER</artifactId>
  <packaging>iar</packaging>
```

Make sure to include your project in the build by adding your provider 
in the main [module build](../pom.xml).

## Implementation

Implement your custom [ChatModelProvider](../smart-workflow/src/com/axonivy/utils/smart/workflow/model/spi/ChatModelProvider.java) within your project.

You need to register your implementation in a file:
`src/META-INF/services/com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider`
The file must contain a single line, stating your implementation type name.

## Variables

Every provider has its own set of variables. Please contribute your ChatModel provider variables to
the `Variables.AI.Providers.PROVIDER`.

```yaml
Variables:
  AI:
    Providers:
      PROVIDER:
        #[password]
        APIKey: ${decrypt:}
        ...
```

Your custom `variables.yaml` should also be copied and listed into the README.md setup description,
that invites users to use this provider.

Furthermore, please enrich the global enumeration of available providers [variables.yaml](../smart-workflow/config/variables.yaml) to list your provider.
See the enumeration called `AI.DefaultProvider`.

### Checklist

- [ ] custom variables.yaml in your provider
- [ ] list your provider in `AI.DefaultProvider` of [variables.yaml](../smart-workflow/config/variables.yaml)
- [ ] list your model in the Model section of the product [README.md](../smart-workflow-product/REAMDE.md)
- [ ] extend the product [build](../smart-workflow-product/pom.xml) to interpolate your variables into README.md

## File Extraction Support

| Provider | Model(s) | PNG / JPEG | PDF |
| --- | --- | :---: | :---: |
| **OpenAI** | `gpt-4o`, `gpt-4.1`, `gpt-4.1-mini`, `gpt-4.1-nano`, `gpt-5` | ✓ | ✓ |
| **Azure OpenAI** | Any vision-capable deployment (e.g. `gpt-4o`, `gpt-4.1` family) | ✓ | ✓ |
| **Gemini** | All models (`gemini-1.5-*`, `gemini-2.0-*`, `gemini-2.5-*`) | ✓ | ✓ |
| **xAI** | All `grok-4-1-*` models | ✓ | — |

**Note for Azure OpenAI**: file extraction capability depends on the underlying model of your deployment, not the deployment name itself. Make sure your deployment uses a vision-capable model.

**Note for xAI**: PDF files are not natively supported by the xAI API. To process PDFs with Grok models, convert them to images first before passing them to Axon Ivy Smart Workflow.

When contributing a new provider, document its multimodal support in this table.

### Payload Size Limits

File content is base64-encoded before being sent to the provider. The limits below apply to the encoded payload and vary by provider. There is no size cap enforced by Axon Ivy Smart Workflow itself — developers are responsible for ensuring files stay within the bounds of their chosen provider.

## Libraries

Smart-workflow providers are built upon existing LangChain4j providers.
Please exclude dependencies from your `pom.xml`, which are already part of smart-workflow.
Classically this will be the `langchain4j-core` and `langchain4j-http-client`

## Testing

Tests for your model provider should be written in the common `smart-workflow-test` project. Provider specific functionality
should be enclosed in `src_test/com/axonivy/utils/smart/workflow/model/PROVIDER`.

Therefore it's ok to add a dependency from the commont test project
to your new model provider.

## Demo

We expect all providers to work in the same manor, therefore no extra demonstration process needs to be added in the demo project.

Do not add dependencies to additional model providers to the `smart-workflow-demo` project.
