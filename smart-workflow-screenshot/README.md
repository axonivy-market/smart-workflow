# Smart Workflow Screenshot Tests

Capture screenshots of Smart Workflow processes in the Axon Ivy process editor, including their inscription panels.

Key pieces:
- Ivy `project-build-plugin` provisions a temporary engine for the integration-test run. 
- We modify the downloaded-engine; by adding process-editor-standalone.jar (which offers inscription capabilities) into the engine's `webapps/ivy/WEB-INF/lib`.
- We start the engine with dev-flags, to run like in vscode: `-Ddev.mode=true` and `-Dglsp.test.mode=true`.
