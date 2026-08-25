# Entwicklungscontainer

Auf unsere Smart-Workflow-Entwicklungsumgebung kann über einen Dev-Container zugegriffen werden. Der Container vereinfacht die Einrichtung Ihres Arbeitsbereichs und stellt Sidecar-Dienste wie [RAG](../RAG.md) (OpenSearch) oder [Tracing](../observe/OBSERVE.md) über Tools von Drittanbietern bereit.

Daher eignet sich der Dev Container ideal für:
- neue Nutzer, die die gesamten Funktionen von Smart-Workflow erkunden möchten
- Entwickler, die bei der Übergabe an den Q&A- und den Produktionsbereich die Enttäuschung vermeiden wollen, dass es „auf meinem Rechner läuft“

## Lokaler Rechner

Auf Ihrem lokalen Rechner lässt sich der Smart-Workflow-Dev-Container mit wenigen einfachen Schritten ausführen. Bei lokaler Ausführung entstehen keine Kosten, und Sie nutzen die Leistungsfähigkeit Ihrer Hardware.

### Anforderungen

- Docker muss installiert sein und laufen (Docker Desktop unter macOS/Windows oder Docker Engine unter Linux).
- Es wird empfohlen, VS Code mit der Erweiterung „ **Dev Containers“** (`ms-vscode-remote.remote-containers`) zu verwenden.

### Lokal in VS Code starten

1. Kopiere dieses Repository auf deinen Rechner.
2. Öffnen Sie den Repository-Ordner in VS Code.
3. Führen Sie „ **“ aus und starten Sie die Dev-Container: Öffnen Sie „** “ im Container über die Befehlspalette erneut.
4. Warten Sie, bis die Axon Ivy-Startseite angezeigt wird (dies kann einige Minuten dauern).
5. Geben Sie Ihren API-Schlüssel unter `smart-workflow-test/config/variables.yaml` über „Importieren“ `AI.Providers.OpenAI.APIKey` ein.

## Auf GitHub gehostet

Um eine Smart-Workflow-Entwicklungsumgebung zu nutzen, ist keine lokale Umgebung erforderlich. Sie können sie direkt im Browser ausführen, gehostet von GitHub.

![gh-init](../img/devcontainer-codespace-init.png)

### So fangen Sie an

1. Öffne das Repository auf GitHub.
2. Klicken Sie auf die Schaltfläche „ **-Code“** und öffnen Sie die Registerkarte „ **-Codespaces“**.
3. Klicken Sie auf das Menü „ ` “ …` (oder **„Codespace konfigurieren und erstellen“**), um vor dem Start Optionen auszuwählen.
4. Stellen Sie den Maschinentyp auf eine „ **“-Option mit 4 Kernen und „** “ ein.
5. Erstellen Sie den Codespace und warten Sie, bis VS Code gestartet ist.
6. Warten Sie, bis die Axon Ivy-Startseite angezeigt wird (dies kann 5–10 Minuten dauern).
7. Geben Sie Ihren API-Schlüssel unter `smart-workflow-test/config/variables.yaml ein.`
8. Führen Sie eine Demo aus dem Verzeichnis „smart-workflow-demos“ aus oder beginnen Sie mit der Entwicklung Ihrer Funktion.

### Kostentipp

Um unerwartete Kosten zu vermeiden, beenden Sie Ihren Codespace, sobald Ihre Sitzung beendet ist. Öffnen Sie in GitHub die Seite „ **“ unter „Codespaces“ (** ) und wählen Sie „ **“ („Codespace beenden“) (** ) für inaktive Umgebungen, anstatt diese im Hintergrund weiterlaufen zu lassen.

### Beifahrer-Dienste

Der Dev-Container startet diese Dienste automatisch:

| Service          | Bild                                          | Hafen  | Zweck                                                                                                                                                                |
| ---------------- | --------------------------------------------- | ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Arbeitsbereich` | `mcr.microsoft.com/devcontainers/base:trixie` | –      | Ihr VS Code-Arbeitsbereich mit Java, Maven und der Axon Ivy-Engine.                                                                                                  |
| `Phoenix`        | `arizephoenix/phoenix:nightly`                | `6006` | OpenInference-Tracing-Benutzeroberfläche – siehe [Tracing](../observe/OBSERVE.md).                                                                                   |
| `opensearch`     | `opensearchproject/opensearch:2.11.0`         | `9200` | Vektorspeicher für [RAG](../RAG.md). Startete mit deaktivierter Sicherheit und „ `“ sowie „discovery.type=single-node“` – ausschließlich für die lokale Entwicklung. |


> **Hinweis:** Der OpenSearch-Speicher ist auf 512 MB begrenzt (`OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m`), um den Entwicklungscontainer schlank zu halten. Erhöhen Sie diesen Wert in `.devcontainer/compose.yml`, wenn Sie große Datensätze einlesen.
