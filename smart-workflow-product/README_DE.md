# Smart Workflow

Smart Workflow integriert KI-Agenten-Funktionen in deine Axon Ivy-Prozesse. Du kannst KI-Agenten direkt aus deinen Prozessen aufrufen, strukturierte, verwertbare Ergebnisse erhalten und Benutzerinteraktionen automatisieren.

![Agenten-Nachrichten-Konfigurationen](img/agent-message-configurations.png)

## Wichtigste Funktionen

- Rufe KI-Agenten aus deinen Axon Ivy-Prozessen auf und erhalte strukturierte Ergebnisse, die in Datentypen gemappt werden können, um automatische Datenextraktion und nachgelagerte Verarbeitung zu ermöglichen.
- Fertige Demo-Workflows (Shopping, Support, RAG, Agent-Demos), die gängige Integrationsszenarien zeigen und die Evaluierung beschleunigen.
- Integrierte RAG-Unterstützung und Vector-Store-Integration für kontextsensitives Retrieval-augmented Generation.
- Anbieterunabhängige Konfiguration für OpenAI, Azure OpenAI, Gemini, xAI und Anthropic über `config/variables.yaml`.
- Beobachtbarkeit und Guardrails für sicheren KI-Einsatz: konfigurierbares Logging, Schutzregeln und benutzerdefinierte Felder zur Nachvollziehbarkeit.
- Marketplace-Installationen und Maven-Artefakte für einfache Bereitstellung und Integration.

## Demo

Schau dir die Demo-Implementierungen im Modul `smart-workflow-demo` an; sie enthalten nutzerorientierte Beispiele und Integrationen.

### Demo-Workflows

#### Smart Workflow Demo (smart-workflow-demo)

##### Smart Workflow Agent Demo

1. Starte die "Smart Workflow Agent Demo" über das Demo-Menü oder das Dashboard.
2. Ein vorausgefülltes Eingabefeld erscheint, mit dem du die zu analysierende Abfrage (z. B. Rechnungsdaten) prüfen oder anpassen kannst.
3. Klicke die Demo-Aktion an, um den KI-Agenten aufzurufen; der Agent analysiert die Eingabe und liefert ein strukturiertes Ergebnis zurück.
4. Prüfe das aufbereitete Ergebnis (z. B. das erzeugte Invoice-Objekt) in der UI und bestätige es.
5. Optional: Exportiere oder speichere das strukturierte Ergebnis.

##### Create new product (smart-workflow-demo)

1. Öffne die Shopping-Demo und wähle "Create new product".
2. Ein Produkt-Erstellungsdialog öffnet sich mit Feldern für die Produktmetadaten.
3. Fülle die erforderlichen Felder aus und sende das Formular, um das Produkt zu erstellen.
4. Prüfe den erstellten Produkteintrag in der Shopping-Store-Liste.

##### Shopping Store (smart-workflow-demo)

1. Öffne die Shopping-Demo und wähle "Shopping Store".
2. Durchsuche verfügbare Artikel und lege sie über den interaktiven Dialog in den Warenkorb.
3. Führe den Checkout durch, um Bestell- und Bestandsaktualisierungen zu simulieren.

## Einrichtung

- **Rollen:** Rollen-Konfiguration nicht dokumentiert
- **OpenAPI:** Keine öffentlich zugänglichen OpenAPI-Spezifikationen von dieser Erweiterung bereitgestellt.

### Variablen

```yaml
Variables:
  AI:
    # [enum: OpenAI, AzureOpenAI, Gemini, xAI, Anthropic]
    DefaultProvider: ""
    # Guardrails designed to ensure that AI operate safely ethical, and legal
    Guardrails:
      # Default input guardrails. Separated by comma. Available: PromptInjectionInputGuardrail, PiiMaskingGuardrail
      DefaultInput: ""
      # Default output guardrails. Separated by comma. Available: SensitiveDataOutputGuardrail, PiiMaskingGuardrail
      DefaultOutput: ""
    Tool:
      WebSearch:
        # Name of the search engine to use. Must match the name() of a registered SmartWebSearchEngine. Example: "duckduckgo"
        Engine: "duckduckgo"
        # Maximum number of search results returned per query.
        MaxResults: ""
        # Whitelist of allowed domains for web search results. Separated by comma. Example: "stackoverflow.com, github.com, docs.oracle.com"
        # If empty, all domains are allowed.
        WhitelistDomains: ""
    RAG:
      # Default number of document segments returned per query.
      MaxResults: "5"
      # Cosine similarity threshold (0.0 - 1.0). Segments below this score are excluded.
      MinScore: "0.6"
      # Number of tokens per document chunk when splitting.
      ChunkSize: "300"
      # Number of overlapping tokens between consecutive chunks.
      ChunkOverlap: "20"
      EmbeddingModel:
        # Provider used to generate embeddings. Only providers that support embedding are valid.
        # When blank, falls back to AI.DefaultProvider.
        Provider: ""
        # Embedding model name. When blank, defaults to the provider's DefaultEmbeddingModel variable.
        # Example: "text-embedding-3-small" (OpenAI), "gemini-embedding-001" (Gemini)
        Name: ""
        # Optional separate API key for embedding calls (billed separately from chat).
        # When blank, the provider's own API key variable is used.
        #[password]
        ApiKey: ${decrypt:}
    Observability:
      CustomFields:
        # Enable marking of workflow custom fields to track AI usage provenance.
        Enabled: "true"
      Ivy:
        # Enable chat history recording for governance audit.
        Enabled: ""
      Openinference:
        # Enable logging of AI interactions for observability and debugging purposes.
        Enabled: ""
        HideInputMessages: ""
        HideOutputMessages: ""
```

- Für diesen Abschnitt wurden keine Informationen geliefert.

## Komponenten

### Connector-Prozesse

#### SmartWorkflowAgent.p.json

- **invokeAgent(String query, String systemMessage, List<String> tools, Class resultType) -> resultObject: Object**
    - Eingaben:
        - `query` (String) - Die Nutzeranfrage/Nachricht, die an den KI-Agenten gesendet wird
        - `systemMessage` (String) - Systemnachricht zur Definition des Agentenverhaltens
        - `tools` (List<String>) - Liste der Tool-Namen; leer lassen, um alle Tools zu verwenden
        - `resultType` (Class) - Erwartete Ergebnis-Typklasse für strukturierte Ausgabe; für String-Ergebnis null lassen
    - Ergebnis:
        - `resultObject` (Object) - Das Antwortobjekt des KI-Agenten

### Formular-Komponenten

#### SmartWorkflowAgentData — Datenklasse für KI-Agenten-Aufrufe
- **Namespace:** Portal
- **Komponententyp:** Datenklasse
- **Felder:**
   - `query` (String) — Die Nutzeranfrage/Nachricht, die an den KI-Agenten gesendet wird
   - `systemMessage` (String) — Systemnachricht zur Definition des Agentenverhaltens
   - `tools` (List<String>) — Liste der Tool-Namen; leer lassen, um alle Tools zu verwenden
   - `resultType` (Class) — Erwartete Ergebnis-Typklasse für strukturierte Ausgabe; für String-Ergebnis null lassen
   - `resultObject` (Object) — Das Antwortobjekt des KI-Agenten
- **Verwendung:** `SmartWorkflowAgent.p.json` (invokeAgent CallSubStart)
- **Zweck:** Stellt Parameter und Ergebnisabbildung für den Aufruf des Smart Workflow Agent aus Portal-Prozessen bereit

### Maven-Artefakte

1. smart-workflow

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

2. smart-workflow-demo

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-demo</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

3. smart-workflow-openai

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-openai</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

4. smart-workflow-azure-openai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-azure-openai</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

5. smart-workflow-gemini *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-gemini</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

6. smart-workflow-xai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-xai</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```

7. smart-workflow-anthropic *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-anthropic</artifactId>
  <version>@version@</version>
  <type>iar</type>
</dependency>
```
