# Smart Workflow 🪄️

Lass KI-Agenten deine Prozesse dynamisch steuern.

Das Projekt integriert KI-Agenten, Tools und Retriever, um automatisierte, beobachtbare und erweiterbare Workflows zu ermöglichen.

![Agenten-Nachrichten-Konfigurationen](img/agent-message-configurations.png)

**Wichtigste Funktionen**

- Automatisiere Aufgaben und extrahiere strukturierte Daten, indem du KI-Agenten direkt aus deinen Prozessen aufrufst.
- Probiere vorgefertigte Demo-Workflows, um agentengesteuerte Szenarien schnell zu evaluieren.
- Integriere verschiedene Modellanbieter (OpenAI, Azure, Gemini, Anthropic) für flexible Modellwahl.
- Verbessere Antworten mit Retrieval-Augmented Generation (RAG) über OpenSearch-Connectoren für kontextbewusste Ergebnisse.
- Überwache KI-Interaktionen mit integrierter Beobachtbarkeit und Tracing (Integration mit Arize Phoenix).
- Erweitere Agentenfunktionalität sicher durch integrierte Tools (Websuche, Dateiextraktion, Guardrails).

## Demo

![Support-Ticket-Beispiel](img/support-ticket-example.png)

### Demo-Workflows

#### Features (smart-workflow-demo/Features)

##### Smart Workflow Agent Demo
1. Starte die Smart Workflow Agent Demo über das Demo‑Menü.
2. Gib eine Benutzeranfrage ein oder füge sie ein, die beschreibt, welche Aufgabe der Agent ausführen soll.
3. Bestätige und sende die Anfrage ab; der Agent führt die Aufgabe aus und liefert ein strukturiertes Ergebnis zurück.
4. Prüfe das Ergebnis und übernehme die strukturierten Daten bei Bedarf.

##### Web Search Demo
1. Starte die Web Search Demo aus der Features‑Demoauswahl.
2. Gib eine Suchanfrage im Dialog ein und bestätige sie.
3. Die Demo führt eine Websuche durch und zeigt eine kurze Zusammenfassung sowie einzelne Suchergebnisse an.
4. Prüfe die Zusammenfassung und öffne bei Bedarf die Quellen über die bereitgestellten Links.

#### Shopping (smart-workflow-demo/Business/ShoppingDemo)

##### Neues Produkt anlegen
1. Starte den Workflow „Create new product“ im Demo‑Menü.
2. Fülle die Produktdaten im Dialog aus (Name, Kategorie, Preis, Beschreibung).
3. Sende das Formular ab, um das Produkt anzulegen, und überprüfe die Bestätigungsmeldung.

##### Shopping Store
1. Starte den Workflow „Shopping Store“ im Demo‑Menü.
2. Durchsuche verfügbare Produkte und lege Artikel in den Warenkorb.
3. Gehe zur Kasse und bestätige die Bestellung.

## Einrichtung

- **Rollen:** Rollen-Konfiguration nicht dokumentiert
- **OpenAPI:** Keine Informationen für diesen Abschnitt geliefert.

### Variablen

![Tool-Konfigurationen](img/tool-configurations.png)

```
@variables.yaml@
```

1. Arize Phoenix
   1.1 Starte Arize Phoenix mit Docker: `docker run --rm -p 6006:6006 -p 4317:4317 arizephoenix/phoenix:nightly`
   1.2 Öffne die Tracing‑Plattform im Browser: [http://localhost:6006](http://localhost:6006)
2. Visual Studio Code
   2.1 Installiere die Axon Ivy Designer Erweiterung
   2.2 Öffne die Einstellungen und suche nach „Axon Ivy“, dort definiere:
      - `AxonIvy > Engine: VM args` : `-Dotel.traces.exporter=otlp -Dotel.exporter.otlp.endpoint=http://localhost:6006 -Dotel.resource.attributes=openinference.project.name=smart-workflow`
   2.3 Starte Visual Studio Code neu (Command > Developer: Reload Window)
   2.4 Setze die Variable `AI.Observability.Openinference.Enabled=true` in der Datei `config/variables.yaml` eines Projekts, das von `smart-workflow` abhängt.
   2.5 Führe einen KI‑unterstützten Prozess in `smart-workflow-demo` aus
3. Devcontainer
   3.1 Unser Devcontainer ist vorkonfiguriert, um Arize Phoenix innerhalb deines Codespaces auszuführen. Definiere den API‑Schlüssel des KI‑Anbieters, damit Traces an Arize Phoenix gesendet werden.
4. Abfragen
   4.1 Wähle das Projekt „smart-workflow“ aus
   4.2 Gib als Filterbedingung `span_kind == 'LLM'` ein
   4.3 Wechsle neben der Filterleiste von `Root Spans` zu `All`

## Komponenten

### Aufrufbare Unterprozesse

#### SmartWorkflowAgent.p.json

- **Signature**: invokeAgent(String query, String systemMessage, List<String> tools, Class resultType) -> resultObject: Object
    - Eingabe:
        - `query` (String) — Die Benutzeranfrage/die Nachricht, die an den KI‑Agenten gesendet wird
        - `systemMessage` (String) — Systemnachricht zur Definition des Agentenverhaltens
        - `tools` (List<String>) — Liste von Tool‑Namen, die dem Agenten zur Verfügung stehen. Leerlassen, um alle Tools zu nutzen
        - `resultType` (Class) — Erwarteter Ergebnis‑Typ für strukturierte Ausgaben. `null` bedeutet String‑Ergebnis
    - Ergebnis:
        - `resultObject` (Object) — Die Antwort des KI‑Agenten

### Dialogkomponenten

- No information was delivered for this section.

### Webservices

- No information was delivered for this section.

### Maven‑Artefakte

1. smart-workflow

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow</artifactId>
  <type>iar</type>
</dependency>
```

2. smart-workflow-openai

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-openai</artifactId>
  <type>iar</type>
</dependency>
```

3. smart-workflow-azure-openai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-azure-openai</artifactId>
  <type>iar</type>
</dependency>
```

4. smart-workflow-gemini *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-gemini</artifactId>
  <type>iar</type>
</dependency>
```

5. smart-workflow-xai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-xai</artifactId>
  <type>iar</type>
</dependency>
```

6. smart-workflow-anthropic *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-anthropic</artifactId>
  <type>iar</type>
</dependency>
```
