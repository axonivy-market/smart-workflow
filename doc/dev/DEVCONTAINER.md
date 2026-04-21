# Devcontainer

Our Smart-Workflow development environment is accessible via a devcontainer.
The container removes the complexity of setting up your workspace and
provides sidecar services like RAG or Tracing via third-party tools.

Therefore the Devcontainer is perfect for: 
- new users that want to explore the full capabilities of Smart-Workflow
- developers that want to avoid the "runs on my machine" disappointment
  when going to Q&A and production

## Local machine

Your local machine can run the Smart-Workflow Devcontainer with a few simple steps. Locally run, this produces no costs and leverages the power of your hardware.

### Requirements

- Docker must be installed and running (Docker Desktop on macOS/Windows, or Docker Engine on Linux).
- VS Code with the **Dev Containers** extension (`ms-vscode-remote.remote-containers`) is recommended.

### Start locally in VS Code

1. Clone this repository to your machine.
2. Open the repository folder in VS Code.
3. Run **Dev Containers: Reopen in Container** from the command palette.


## GitHub hosted

To run a Smart-Workflow dev environment no local environment is required.
You can run it right in the browser, hosted by GitHub.

### How to start

1. Open the repository in GitHub.
2. Click the green **Code** button and open the **Codespaces** tab.
3. Click the `...` menu (or **Configure and create codespace**) to select options before launch.
4. Set the machine type to a **4-core** option.
5. Create the codespace and wait until VS Code starts.
6. The devcontainer is initialized automatically; once startup tasks finish, you can run and demo Smart-Workflow immediately.

### Cost tip

To avoid unexpected costs, stop your codespace as soon as your session is finished. In GitHub, open the **Codespaces** page and choose **Stop codespace** for inactive environments instead of leaving them running in the background.

