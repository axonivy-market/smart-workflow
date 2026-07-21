# Chat-Models

Im Verzeichnis „ `-Modelle“ (` ) verwalten wir Projekte, die einen „ChatModelProvider“ bereitstellen.

## Beiträge leisten

Wir sind offen dafür, weitere ChatModels von beliebigen Anbietern zu unterstützen. Falls Ihr bevorzugtes Modell fehlt, fügen Sie es einfach hier hinzu.

Erstellen Sie ein Verzeichnis „ `models/smart-workflow-PROVIDER“` und ersetzen Sie dabei „PROVIDER“ durch den Namen Ihres konkreten Anbieters. Passen Sie die Projektkoordinaten bitte an unseren bestehenden Arbeitsbereich an:

```xml
<groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-PROVIDER</artifactId>
  <packaging>iar</packaging>
```

Stellen Sie sicher, dass Ihr Projekt in den Build einbezogen wird, indem Sie Ihren Provider im Hauptmodul [module build](../pom.xml) hinzufügen.

## Umsetzung

Implementieren Sie Ihren benutzerdefinierten [ChatModelProvider](../smart-workflow/src/com/axonivy/utils/smart/workflow/model/spi/ChatModelProvider.java) in Ihrem Projekt.

Sie müssen Ihre Implementierung in einer Datei registrieren: `src/META-INF/services/com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider` Die Datei muss eine einzige Zeile enthalten, in der der Name Ihres Implementierungstyps angegeben ist.

## Variablen

Jeder Anbieter hat seine eigenen Variablen. Bitte tragen Sie die Variablen Ihres ChatModel-Anbieters in die Datei „ `Variables.AI.Providers.PROVIDER“ unter` ein.

```yaml
Variablen:
  AI:
    Anbieter:
      PROVIDER:
        #[Passwort]
        APIKey: ${decrypt:}
        ...
```

Ihre benutzerdefinierte Datei „ `variables.yaml“` sollte ebenfalls kopiert und in der Einrichtungsbeschreibung in der Datei „README.md“ aufgeführt werden, die Nutzer dazu einlädt, diesen Anbieter zu nutzen.

Bitte ergänzen Sie außerdem die globale Auflistung der verfügbaren Anbieter [variables.yaml](../smart-workflow/config/variables.yaml), um Ihren Anbieter aufzunehmen. Siehe dazu die Auflistung „ `“ unter AI.DefaultProvider`.

### Checkliste

- [ ] „custom variables.yaml“ in Ihrem Provider
- [ ] Tragen Sie Ihren Provider unter „ `“ in „AI.DefaultProvider“` aus [variables.yaml](../smart-workflow/config/variables.yaml) ein.
- [ ] Trage dein Modell im Abschnitt „Modell“ der Datei [README.md](../smart-workflow-product/README.md) des Produkts ein
- [ ] Erweitern Sie das Produkt [build](../smart-workflow-product/pom.xml), um Ihre Variablen in die Datei „README.md“ einzufügen

## Unterstützung bei der Dateiextraktion

| Anbieter         | Modell(e)                                                                                 | PNG / JPEG | PDF |
| ---------------- | ----------------------------------------------------------------------------------------- | :--------: | :-: |
| **OpenAI**       | `gpt-4o`, `gpt-4.1`, `gpt-4.1-mini`, `gpt-4.1-nano`, `gpt-5`                              |     ✓      |  ✓  |
| **Azure OpenAI** | Jede Implementierung mit Bildverarbeitungsfunktionen (z. B. `gpt-4o`, `gpt-4.1` -Familie) |     ✓      |  ✓  |
| **Zwillinge**    | Alle Modelle (`gemini-1.5-*`, `gemini-2.0-*`, `gemini-2.5-*`)                             |     ✓      |  ✓  |
| **xAI**          | Alle Modelle der Baureihen „ `“, „grok-4-1-*“ und „` “                                    |     ✓      |  —  |
| **Anthropic**    | Alle Modelle (`claude-opus-*`, `claude-sonnet-*`, `claude-haiku-*`)                       |     ✓      |  ✓  |
| **Ollama**       | Nur Modelle mit Bildverarbeitungsfunktionen (`llava`, `llama3.2-vision`, `gemma3`, ...)   |     ✓      |  —  |

**Hinweis für Azure OpenAI-**: Die Fähigkeit zur Dateiextraktion hängt vom zugrunde liegenden Modell Ihrer Bereitstellung ab, nicht vom Namen der Bereitstellung selbst. Stellen Sie sicher, dass Ihre Bereitstellung ein visuellfähiges Modell verwendet.

**Hinweis für xAI-**: PDF-Dateien werden von der xAI-API nicht nativ unterstützt. Um PDF-Dateien mit Grok-Modellen zu verarbeiten, konvertieren Sie diese zunächst in Bilder, bevor Sie sie an Axon Ivy Smart Workflow übergeben.

**Hinweis zu Anthropic**: Bilder und PDF-Dateien können über eine URL oder als Base64-kodierte Daten übermittelt werden; beide Formate unterstützen die Textextraktion und das visuelle Verständnis (Diagramme, Grafiken, Layouts).

**Hinweis zu Ollama**: Für die Bildunterstützung muss ein visuellfähiges Modell abgerufen werden (z. B. `ollama pull llava`); reine Textmodelle lehnen Bildeingaben ab. PDFs werden nicht nativ unterstützt – konvertieren Sie diese daher zunächst in Bilder.

Wenn Sie einen neuen Anbieter hinzufügen, tragen Sie dessen multimodale Unterstützung in diese Tabelle ein.

### Beschränkungen hinsichtlich der Nutzlastgröße

Der Dateiinhalt wird vor dem Versand an den Anbieter Base64-kodiert. Die unten aufgeführten Beschränkungen gelten für die kodierte Nutzlast und variieren je nach Anbieter. Axon Ivy Smart Workflow selbst legt keine Größenbeschränkung fest – die Entwickler sind dafür verantwortlich, sicherzustellen, dass die Dateien die vom jeweiligen Anbieter festgelegten Grenzen einhalten.

## Bibliotheken

Smart-Workflow-Provider basieren auf bestehenden LangChain4j-Providern. Bitte schließen Sie in Ihrer „ `“-pom.xml` diejenigen Abhängigkeiten aus, die bereits Teil von Smart-Workflow sind. In der Regel sind dies „ `“, „langchain4j-core“` und „ `“ sowie „langchain4j-http-client“.`

## Testen

Tests für Ihren Modell-Provider sollten im gemeinsamen Projekt „ `smart-workflow-test“ (` ) geschrieben werden. Providerspezifische Funktionen sollten in „ `src_test/com/axonivy/utils/smart/workflow/model/PROVIDER“ (`) untergebracht werden.

Daher ist es in Ordnung, eine Abhängigkeit vom gemeinsamen Testprojekt zu Ihrem neuen Modell-Provider hinzuzufügen.

## Demo

Wir gehen davon aus, dass alle Anbieter auf die gleiche Weise arbeiten; daher muss im Demoprojekt kein zusätzlicher Demonstrationsprozess hinzugefügt werden.

Fügen Sie dem Projekt „ `smart-workflow-demo“ (` ) keine Abhängigkeiten zu weiteren Modellanbietern hinzu.
