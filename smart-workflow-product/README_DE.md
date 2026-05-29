# Smart Workflow

Smart Workflow integriert KI-Agenten in Axon Ivy-Prozesse und ermöglicht dir, Routineaufgaben und Integrationen intelligent zu automatisieren.

![Agent message configurations](img/agent-message-configurations.png)

Verwende vorgefertigte Demo-Workflows und Connectoren, um Sprachmodelle und Tools direkt in deine Geschäftsprozesse zu integrieren.

**Wichtigste Funktionen**

- Rufe KI-Agenten aus deinen Prozessen auf, um Entscheidungen zu automatisieren und strukturierte Ergebnisse zu erhalten.
- Verbinde mehrere KI-Anbieter und Tools mit eingebauten Adaptern für flexible Integration.
- Probiere Demo-Workflows für typische Szenarien wie Ticket-Erstellung und Rechnungsextraktion aus.
- Erhalte strukturierte Ergebnisse, die sich für nachgelagerte Automatisierung und Reporting eignen.
- Konfiguriere Connectoren und Laufzeitverhalten über Repository-Variablen für einfache Bereitstellung.
- Beinhaltet wiederverwendbare Datentypen und UI-Komponenten, um Integration und Tests zu beschleunigen.

## Demo

Erkunde die Demo-Implementierungen im Modul `smart-workflow-demo`, um praktische Beispiele und lauffähige Szenarien zu sehen.

### Demo Workflows

#### Features (smart-workflow-demo/processes/Features)

##### File Extraction Demo (CMS)

1. Starte die File Extraction Demo über das Demo-Menü.
2. Lade ein Rechnungsbild aus dem CMS hoch oder wähle eine lokale Datei aus.
3. Der Agent analysiert das Dokument und extrahiert Rechnungsdaten.
4. Prüfe die extrahierten Rechnungen in den Logs oder der Oberfläche.

##### File Extraction Demo (Binary)

1. Starte die File Extraction (Binary) Demo über das Demo-Menü.
2. Lade ein PDF mit Rechnungen hoch.
3. Der Agent verarbeitet die Binärdaten und extrahiert Rechnungsinformationen.
4. Prüfe die Ergebnisse und exportiere die Daten bei Bedarf.

#### AgentDemo (smart-workflow-demo/processes/AgentDemo)

##### Support Agent with Tools Demo

1. Starte die Support Agent with Tools Demo über das Demo-Menü.
2. Gib eine Support-Anfrage ein oder wähle ein Beispiel aus.
3. Der Agent führt konfigurierte Tools aus und liefert ein Support-Ticket-Ergebnis.
4. Prüfe die Support-Ticket-Details und leite gegebenenfalls Maßnahmen ein.

## Einrichtung

- **Rollen:** Rollen-Konfiguration nicht dokumentiert
- **OpenAPI:** No information was delivered for this section.


### Variablen

```
@variables.yaml@
```

## Komponenten

### Aufrufbare Teilprozesse

#### SmartWorkflowAgent.p.json

- **invokeAgent -> resultObject: Object**

- Eingabe:
  - `query` (String) - Die Benutzereingabe / Anfrage an den KI-Agenten
  - `systemMessage` (String) - Systemnachricht zur Definition des Agentenverhaltens
  - `tools` (List<String>) - Liste von Tool-Namen, die dem Agenten zur Verfügung stehen. Leer = alle Tools
  - `resultType` (Class) - Erwarteter Ergebnistyp für strukturierte Ausgaben. `null` = String-Ergebnis
- Ergebnis:
  - `resultObject` (Object) - Antwort/Ergebnis des KI-Agenten

### Dialog-Komponenten

#### SupportAgentData — Datenstruktur für Support-Agent

- **Namespace:** AgentDemo
- **Komponententyp:** Data Class
- **Felder:**
  - `taskInfo` (com.axonivy.utils.smart.workflow.demo.dto.TaskInfo) — Task-Informationen
  - `query` (String) — Roh-Query-Text
  - `targetObject` (com.axonivy.utils.smart.workflow.demo.dto.SupportTicket) — Support-Ticket-Objekt
  - `customInstructions` (List<String>) — Benutzerdefinierte Anweisungen für den Agenten

### Webdienste

- Keine Informationen gefunden.

### Maven-Artefakte

1. smart-workflow

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow</artifactId>
  <type>iar</type>
</dependency>
```

2. smart-workflow-demo

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-demo</artifactId>
  <type>iar</type>
</dependency>
```

3. smart-workflow-openai

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-openai</artifactId>
  <type>iar</type>
</dependency>
```

4. smart-workflow-azure-openai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-azure-openai</artifactId>
  <type>iar</type>
</dependency>
```

5. smart-workflow-gemini *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-gemini</artifactId>
  <type>iar</type>
</dependency>
```

6. smart-workflow-xai *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-xai</artifactId>
  <type>iar</type>
</dependency>
```

7. smart-workflow-anthropic *(optional)*

```xml
<dependency>
  <groupId>com.axonivy.utils.ai</groupId>
  <artifactId>smart-workflow-anthropic</artifactId>
  <type>iar</type>
</dependency>
```
