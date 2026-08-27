# Chat Models

In the `models` directory we maintain projects that supply a `ChatModelProvider`. This page is the guideline for adding one. To *use* an existing provider, see [Model Providers](../build/providers.md).

We are open to supporting more chat models from any vendor. If you miss your preferred one, contribute it to this space.

## Project setup

Create a directory `models/smart-workflow-PROVIDER`, replacing PROVIDER with your concrete vendor. Align the project coordinates with the existing workspace:

```xml
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-PROVIDER</artifactId>
  <packaging>iar</packaging>
```

Include the project in the build by adding your provider to the [main module build](https://github.com/axonivy-market/smart-workflow/blob/master/pom.xml).

## Implementation

Implement [`ChatModelProvider`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow/src/com/axonivy/utils/smart/workflow/model/spi/ChatModelProvider.java) within your project — see [Java API](../reference/java-api.md#model-providers) for the interface.

Register your implementation in `src/META-INF/services/com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider`. The file must contain a single line stating your implementation type name.

Two parts of `ModelOptions` decide how your provider behaves in edge cases, and both belong in [Provider Capabilities](../reference/capabilities.md):

- `structuredOutput()` — whether to apply a JSON schema. If your vendor cannot, log an error and build the model without it rather than failing the call.
- `hasTools()` — whether the agent also has tools. Ollama uses this to drop the schema, since the two are mutually exclusive there.

## Variables

Every provider has its own set of variables. Contribute yours under `Variables.AI.Providers.PROVIDER`:

```yaml
Variables:
  AI:
    Providers:
      PROVIDER:
        #[password]
        APIKey: ${decrypt:}
        ...
```

Also add your provider to the global enumeration in [`smart-workflow/config/variables.yaml`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow/config/variables.yaml), under `AI.DefaultProvider`.

## Libraries

Smart Workflow providers are built on the existing LangChain4j providers. Exclude any dependency from your `pom.xml` that is already part of smart-workflow — classically `langchain4j-core` and `langchain4j-http-client`.

## Testing

Tests for your model provider go in the common `smart-workflow-test` project, with provider-specific functionality under `src_test/com/axonivy/utils/smart/workflow/model/PROVIDER`. It is fine to add a dependency from the common test project to your new model provider.

## Demo

We expect all providers to work the same way, so no extra demonstration process is needed. **Do not** add dependencies to additional model providers to the `smart-workflow-demo` project.

## Checklist

- [ ] `ChatModelProvider` implemented and registered via SPI
- [ ] custom `variables.yaml` in your provider project
- [ ] provider listed in `AI.DefaultProvider` of [`smart-workflow/config/variables.yaml`](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow/config/variables.yaml)
- [ ] project added to the [main module build](https://github.com/axonivy-market/smart-workflow/blob/master/pom.xml)
- [ ] models, file-extraction support and structured-output behaviour documented in [Provider Capabilities](../reference/capabilities.md) — including any condition under which the schema is dropped
- [ ] provider configuration block added to [Model Providers](../build/providers.md#global-configuration)
- [ ] variables listed in [Variables](../reference/variables.md#providers)
- [ ] setup section added to the product [README.md](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-product/README.md), with the product [build](https://github.com/axonivy-market/smart-workflow/blob/master/smart-workflow-product/pom.xml) extended to interpolate your `@variables.PROVIDER@` token
- [ ] tests in `smart-workflow-test`

> **Important:** [Provider Capabilities](../reference/capabilities.md) is curated knowledge, not an enforced contract — nothing checks a capability before a request is sent. It is the only place a user can find out what your provider supports, so keep it honest.

## See also

- [Java API](../reference/java-api.md#model-providers) — the `ChatModelProvider` interface
- [Provider Capabilities](../reference/capabilities.md) — the matrices you must update
- [Model Providers](../build/providers.md) — the user-facing page for your provider
