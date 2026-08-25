# Demo Projects

Demo projects showcase real-world use cases built on top of Smart Workflow.
Each demo lives under the `demo/` folder and consists of two modules:

| Module | Purpose |
| ------ | ------- |
| `smart-workflow-<name>-demo` | The demo application (processes, services, data classes) |
| `smart-workflow-<name>-demo-test` | Tests for the demo application |

Use the **Supplier Onboarding** demo as the reference implementation:
[`smart-workflow-supplier-demo`](../../demo/smart-workflow-supplier-demo) and
[`smart-workflow-supplier-demo-test`](../../demo/smart-workflow-supplier-demo-test).

---

## Creating a New Demo

1. **Create the demo module** under `demo/`, e.g. `demo/smart-workflow-procurement-demo`.
   Register it in the root `pom.xml` `<modules>` section so it is built by the main CI pipeline.
2. **Create the test module**, e.g. `demo/smart-workflow-procurement-demo-test`,
   following the same structure as `smart-workflow-supplier-demo-test`.
3. **Register the test module** in the `demo.test` profile in the root `pom.xml`
   (do **not** add it to the default `<modules>` list):

```xml
<profiles>
  <profile>
    <id>demo.test</id>
    <modules>
      <module>demo/smart-workflow-supplier-demo-test</module>
      <module>demo/smart-workflow-procurement-demo-test</module> <!-- new -->
    </modules>
  </profile>
</profiles>
```

1. **Add the test module to the `demo-test.yml` pipeline** by appending it to the `-pl` argument
   in [`.github/workflows/demo-test.yml`](../../.github/workflows/demo-test.yml):

```yaml
mvnArgs: -pl demo/smart-workflow-supplier-demo-test,demo/smart-workflow-procurement-demo-test -am -P demo.test
```

   This keeps the pipeline fast by building only demo test modules and their dependencies,
   skipping unrelated CI tests.

---

## Running Demo Tests

Demo tests are not part of the regular CI build. Run them on demand:

- **GitHub Actions:** Trigger the [`Demo-Test-Build`](../../.github/workflows/demo-test.yml) workflow manually from the Actions tab.
- **Locally:** `mvn clean verify -P demo.test`
