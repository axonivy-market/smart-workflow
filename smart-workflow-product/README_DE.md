# Intelligenter Workflow

**Smart Workflow** bringt KI direkt in Axon Ivy, sodass Entwickler KI-Agenten
innerhalb bestehender Axon-Prozesse erstellen, ausführen und verbessern können.
Damit können Geschäftsabläufe große Sprachmodelle nutzen, um natürliche Sprache
zu verstehen, autonome Entscheidungen zu treffen und sich an veränderte
Anforderungen anzupassen – und das alles ohne umfangreiche architektonische
Änderungen.

Die wichtigsten Vorteile von Smart Workflow:

- **Vertraute Einrichtung:** Fügen Sie KI-Agenten ohne strukturelle Änderungen
  in BPMN-Prozesse ein und konfigurieren Sie alles über die
  Standardschnittstellen von Axon Ivy.
- **Unternehmensfähig:** Entwickelt für Unternehmensanforderungen mit
  Protokollierung, Überwachung und Konfigurationskontrollen.
- **Flexible Tools:** Verwandeln Sie jeden aufrufbaren Prozess in ein
  KI-erkennbares Tool.
- **Unterstützung mehrerer Modelle:** Verwenden Sie je nach Aufgabe einfache
  oder erweiterte Modelle.
- **Typsichere Ausgaben:** Erstellen Sie strukturierte Java-Objekte aus
  KI-Antworten zur sofortigen Verwendung.
- **Umgang mit natürlicher Sprache:** Akzeptiert unstrukturierte Eingaben und
  gibt menschenfreundliche Ausgaben zurück.

**Haftungsausschluss**

Der Benutzer „ **“ ist allein verantwortlich** für die Konfiguration,
Bereitstellung und den Betrieb der KI und der damit verbundenen Agenten. Alle
Entscheidungen, Handlungen oder Ergebnisse, die sich aus der Verwendung dieses
Konnektors ergeben, liegen vollständig in der Verantwortung des Benutzers.

Wir stellen lediglich die technische Funktion „ **“ (** ) zur Verfügung, um
solche Konfigurationen zu ermöglichen, und lehnen ausdrücklich jegliche Haftung
für Missbrauch, Fehlkonfigurationen oder unbeabsichtigte Folgen, die sich aus
ihrer Verwendung ergeben, ab. Durch die Verwendung dieses Konnektors erkennen
Sie diese Einschränkungen an und akzeptieren sie.

## Demo

### Axon Ivy Support Agent Demo

Diese Demo zeigt, wie Sie den Axon Ivy Support Agent verwenden, einen
KI-gestützten Agenten, der in einen Geschäftsworkflow integriert ist. Der Agent
wurde entwickelt, um Supportprobleme zu klassifizieren, auf fehlende
Informationen zu prüfen und automatisch Supportaufgaben zu erstellen.

<details>
<summary><strong>Workflow Overview</strong></summary>

1. **Eingabe:** Der Agent erhält eine Support-Anfrage und den Benutzernamen des
   Meldenden.
2. **Klassifizierung:** Es analysiert das Problem, stellt fest, ob Informationen
   fehlen (z. B. die Version), und klassifiziert das Problem (Portal, Core oder
   Market-Produkt).
3. **Aufgabenerstellung:** Falls erforderlich, erstellt der Agent eine
   Support-Aufgabe mit dem Tool `createAxonIvySupportTask` und stellt einen Link
   zur erstellten Aufgabe bereit.
4. **Zusammenfassung und Antwort:** Der Agent fasst das Problem zusammen und
   antwortet dem Benutzer mit einer detaillierten Antwort.

</details>

<details>
<summary><strong>Technical Details</strong></summary>

- Der Agent ist als aufrufbarer Unterprozess implementiert
  (`AxonIvySupportAgent.p.json`) und verwendet die Java-Bean
  `com.axonivy.utils.smart.workflow.AgenticProcessCall`.
- Der Agent ist so konfiguriert, dass er ein bestimmtes Tool verwendet
  (`createAxonIvySupportTask`), mit dem er automatisch Support-Aufgaben
  innerhalb des Workflows erstellen kann. Dies wird durch die Angabe des
  Toolnamens in der Konfiguration des Agenten erreicht (siehe Beispiel unten).
- Die Ausgabe des Agenten wird einem strukturierten Java-Objekt zugeordnet
  (`AxonIvySupportResponse`), sodass das KI-generierte Ergebnis direkt in Axon
  Ivy-Prozessen verwendet werden kann. Dieses Objekt enthält in der Regel
  Details wie die Klassifizierung, den Link zur erstellten Aufgabe und eine
  Zusammenfassung des Support-Problems.

</details>

<details>
<summary><strong>Agent Configuration Example</strong></summary>

Um den Agenten zu konfigurieren, definieren Sie ein Programmelement mit den
folgenden Einstellungen:

![Support Ticket example](img/support-ticket-example.png)

Diese Konfiguration stellt sicher, dass der Agent nur das angegebene Tool
verwendet und dessen Ausgabe als strukturiertes Java-Objekt zurückgibt.

</details>

<details>
<summary><strong>Demo Run Example</strong></summary>

Angenommen, ein Benutzer reicht eine Support-Anfrage ein: „Ich habe einen NPE,
wenn ich Case Details in Portal 12.0.9 öffne.“

1. Der Agent erhält die Frage und den Benutzernamen.
2. Er überprüft, ob Informationen fehlen (z. B. Version), stuft das Problem als
   Portalproblem ein und legt fest, dass eine Support-Aufgabe erstellt werden
   sollte.
3. Der Agent ruft das Tool „ `createAxonIvySupportTask` “ auf, das eine neue
   Support-Aufgabe erstellt und einen Link dazu zurückgibt.
4. Der Agent fasst das Problem zusammen und gibt eine Antwort wie
   beispielsweise:

```text
Classification: Portal
Summary: The problem is a NullPointerException (NPE) occurring when opening Case Details in Portal version 12.0.9. Since the issue is related to the Portal product and the version is provided, a support task has been created to address this problem.
```

Diese Antwort wird dem Objekt „ `” „AxonIvySupportResponse” „` ” zugeordnet und
kann direkt in nachfolgenden Workflow-Schritten verwendet werden.

</details>

<details>
<summary><strong>How to Run the Demo</strong></summary>

1. Stellen Sie sicher, dass Sie den Abschnitt [Konfigurationen](#configurations)
   ausgefüllt haben.
2. Start **Axon Ivy Support** process with a support question and username.
3. Überprüfen Sie die Antwort des Agenten, die eine Klassifizierung, die
   Erstellung einer Aufgabe (falls erforderlich) und eine Zusammenfassung
   enthält.

</details>

---

### Shopping-Demo

Diese Demo zeigt, wie KI die Abläufe eines kleinen E-Commerce-Modegeschäfts
verändern kann. Sie ist fortgeschrittener und kombiniert zwei Mini-Demos: eine
zur Produktgestaltung und eine zur semantischen Suche. Aufgrund ihrer
Komplexität werden wir hier nicht auf den detaillierten Code oder
Schritt-für-Schritt-Anleitungen eingehen. Wenn Sie sich mit der Implementierung
befassen möchten, sehen Sie sich bitte das Demo-Projekt „ `” unter
smart-workflow-demo` an.

<details>
<summary><strong>Product creation</strong></summary>

Traditionell muss der Shopbetreiber beim Hinzufügen eines Produkts viele Felder
manuell ausfüllen und abhängige Datensätze (Lieferant, Marke, Kategorie)
validieren oder erstellen. Bei einem kleinen Shop kann dieser Vorgang Stunden
oder sogar einen ganzen Tag dauern: manuelle Dateneingabe, Suche nach fehlenden
Informationen und erneute Überprüfung auf Fehler.

Mit Smart Workflow-Agenten importiert der Bediener einfach die
Produktspezifikationen und Bilddateien. Die Agenten übernehmen das Parsen, die
Validierung, die Auflösung von Abhängigkeiten und die Produkterstellung –
wodurch der manuelle Aufwand und die Zeit bis zur Veröffentlichung erheblich
reduziert werden.

Entwickler müssen vier Agenten erstellen.

1. Produktagent

- Eingabe: geparste Produktspezifikation
- Tools:
  - Produkt suchen: Produkt im System suchen
  - Produkt erstellen: Erstellen Sie ein neues Produkt anhand der
    bereitgestellten Spezifikation.
  - Überprüfen Sie die Produktabhängigkeiten: Rufen Sie andere Agenten an, um
    Abhängigkeiten (Lieferant, Marke und Kategorie) zu finden und zu validieren.

2. Lieferantenvertreter

- Eingabe: Lieferanteninformationen
- Tools:
  - Lieferanten suchen: Lieferanten im System suchen
  - Lieferanten anlegen: Legen Sie anhand der bereitgestellten Informationen
    einen neuen Lieferanten an.

3. Kategorie-Agent

- Eingabe: Produktkategorie-Informationen
- Tools:
  - Kategorie suchen: Kategorie im System suchen
  - Kategorie erstellen: Erstellen Sie anhand der bereitgestellten Informationen
    eine neue Kategorie.

4. Markenvertreter

- Eingabe: Produktmarkeninformationen
- Tools:
  - Marke suchen: Marke im System suchen
  - Marke erstellen: Erstellen Sie anhand der bereitgestellten Informationen
    eine neue Marke.

Demo flow (start **Create new product** process)

1. Der Betreiber lädt Produktspezifikationen und Bilddateien hoch.
2. Smart Workflow analysiert die Dateien und extrahiert Produktattribute (Titel,
   SKU, Beschreibung, Preis, Lieferanteninformationen, Marke, Kategorie,
   Bilder).
3. Validatoren überprüfen Semantik und Einschränkungen (Pflichtfelder, Formate,
   Eindeutigkeit der SKU, Bildanforderungen).
4. Für jede Abhängigkeit (Lieferant, Marke, Kategorie) fragt Smart Workflow den
   entsprechenden Agenten: Wenn die Entität existiert → ID zurückgeben, wenn sie
   fehlt → anhand der bereitgestellten Spezifikation erstellen.
5. Der Produktagent erstellt das Produkt mit validierten Attributen und Links zu
   Abhängigkeits-IDs.
6. Das System gibt eine Zusammenfassung zurück und öffnet optional einen
   Bildschirm zur manuellen Überprüfung mit vorausgefüllten Feldern für die
   endgültige Freigabe.

Der neue KI-gestützte Prozess führte zu weniger Fehlern, deutlich weniger
manuellem Aufwand und einer wesentlich schnelleren Veröffentlichung.

</details>

<details>
<summary><strong>Semantic search</strong></summary>

Vor der Einführung der KI gaben Käufer Suchbegriffe wie „rotes Kleid“ ein,
wendeten dann manuell Filter (Preis, Marke, Kategorie) an und durchsuchten die
Ergebnisse. Dieser Prozess war nicht nur langsam und unflexibel, sondern
erfasste oft auch keine Synonyme, Stile oder Absichten (z. B. Party vs. Arbeit).

With semantic search the user speaks or types a natural request. AI understands
intent and constraints (color, price, occasion, urgency), converts that into a
structured criteria object. The backend then converts that object into SQL
predicates and returns matched results. Offers clear explanations, familiar
tooling, and easier deployment.

Entwickler müssen ein zusätzliches `Tool „Produkt nach Kriterien finden”` zum
`Produkt-Agenten` hinzufügen, wobei die Eingabe die Suchkriterien sind.

Demo flow (start **Shopping Store** process)

1. Käufer: tippt oder sagt „Ich brauche ein rotes Kleid für 100 Dollar für eine
   Party heute Abend.“
2. `Der Produktagent` extrahiert Attribute und erweitert die Abfrage (Synonyme,
   akzeptable Preisspanne: 80–120 $).
3. Axon Ivy Business Data wandelt Kriterien in optimierte Filter und
   Suchanfragen für die Produkte um.
4. Geben Sie die Top-Produkte zurück, die den Kriterien entsprechen.

To quickly set up the demo data, start **Create data for shopping demo** from
the process list.

</details>

---

### File Extraction Demo

This demo shows how to build a process that reads invoice data directly from
uploaded images and PDF files — with no manual data entry. Using multimodal
language models, the AI reads the document content and returns structured Java
objects that subsequent process steps can use immediately.

To extract from a file, include the file content in the agent's user message.
The AI reads it and maps the result to the specified Java class — no special
tooling or file-system access required.

<details>
<summary><strong>Demo flow</strong></summary>

- Start **File Extraction Demo (CMS)** or **File Extraction Demo (Binary)** from
  the process list.

  1. The process loads an invoice image and a PDF.
  2. The file contents are included in the agent's user message.
  3. The AI reads and extracts the invoice fields.
  4. The result is returned as a typed Java object ready for the next process
     step.

</details>

Not all providers support multimodal input — see the [Models Contribution
Guideline](../doc/MODELS.md#file-extraction-support) for supported providers and
file types.

---

### Guardrail Demo

This demo shows how built-in Smart Workflow guardrails protect AI agents from
prompt injection attacks and prevent sensitive data from leaking in AI
responses. Without protection, a malicious user can craft a message that
overrides the system prompt or tricks the agent into revealing internal data.

Two defense layers are configured in the agent's `inputGuardrails` /
`outputGuardrails` fields:

- `PromptInjectionInputGuardrail` — inspects user input before it reaches the AI
  model and blocks known injection patterns
- `SensitiveDataOutputGuardrail` — scans the AI response before it is returned
  and blocks output containing API keys or private keys

Default guardrails can be set globally in `variables.yaml` under
`AI.Guardrails.DefaultInput` and `AI.Guardrails.DefaultOutput` — any agent
without explicit guardrails inherits these defaults.

<details>
<summary><strong>Demo flow</strong></summary>

- **Prompt injection** (start **Prompt Injection Guardrail Demo** process)

  1. A crafted malicious message is submitted. The
     `PromptInjectionInputGuardrail` intercepts it before the AI is called and
     raises an error.
  2. The process catches the error via an `ErrorBoundaryEvent` and routes to a
     safe fallback path.

- **Sensitive data output** (start **Sensitive Data Output Guardrail Demo**
  process)

  1. A message instructs the agent to include sensitive data in its response.
     The `SensitiveDataOutputGuardrail` intercepts the response after the model
     returns and blocks it.
  2. The error boundary catches this violation and routes to the safe fallback
     path again.

</details>

---

### Custom Guardrail Demo

This demo shows how to implement and register a domain-specific business rule as
a reusable custom guardrail. A company policy requires that agents never mention
competitor products. The `BlockCompetitorMentionGuardrail` enforces this rule in
one place — once registered, it can be added to any agent by name without
touching individual system prompts.

Developers implement `SmartWorkflowInputGuardrail`, expose it through a
`GuardrailProvider`, and register the provider in
`META-INF/services/com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider`.
The guardrail name then appears automatically in the Available Input Guardrails
list. Each agent opts in via `inputGuardrails:
["BlockCompetitorMentionGuardrail"]`; to apply it to every agent, add it to
`AI.Guardrails.DefaultInput` in `variables.yaml`.

<details>
<summary><strong>Demo flow</strong></summary>

- **Blocked query** (start **Custom Guardrail Demo - Blocked** process)

  1. A user submits a query that mentions a competitor product.
  2. `BlockCompetitorMentionGuardrail` detects the mention and blocks the
     request before the AI model is called.
  3. The process catches the error and routes to a safe fallback path.

- **Allowed query** (start **Custom Guardrail Demo - Allowed** process)

  1. A user submits a query with no competitor mentions.
  2. `BlockCompetitorMentionGuardrail` finds nothing to block and allows the
     request through.
  3. The agent processes the query and responds normally.

</details>

---

## Best Practices

The demos below illustrate **best practices** for structuring Axon Ivy agents
and tools with Smart Workflow. Three complementary patterns are shown: one for
tightly scoping an agent's tool access, one for linear task-based orchestration,
and one for feature-grouped tool reuse.

### Agent Pipeline

A linear chain of agents where each one processes an input and passes the result
to the next stage. Best practice: assign a dedicated task to each agent so that
execution is tracked, resumable, and visible in the task history.

See the **Agent Pipeline Demo** process in `smart-workflow-demo`.

### Self-Contained Agent with Co-located Tools

The agent and its tools are self-contained in one file with no cross-process
references, making the full capability easy to ship and expose as a single
callable interface.

See the **Self-Contained Agent** process in `smart-workflow-demo`.

### Feature-Grouped Agents and Tools

This pattern shows how to organize agents and tools by business domain when
tools need to be shared across multiple agents. Rather than bundling everything
inside a single callable, each agent and each tool group lives in its own
process file under a common feature folder — making the domain boundary explicit
and allowing tool reuse.

See the **Shopping Demo** process in `smart-workflow-demo`.

## Setup

Um Ihre AI-Initiative zu starten, müssen wir die Modelle und Werkzeuge im Voraus
definieren.

### Models

Smart Workflow ist nicht an einen bestimmten KI-Anbieter gebunden. Sie können
Ihre bevorzugten Modellanbieter zum Installationszeitpunkt auswählen.

Nach der Installation wählen Sie bitte Ihren Standardmodellanbieter

Die Auswahl Ihres Anbieters erfolgt mit der Variablen `AI.DefaultProvider`.
Darüber hinaus benötigen die meisten Modellanbieter eine ApiKey oder eine andere
eindeutige Kennung. Überprüfen Sie Ihren Anbieter unten, um zu sehen, welche
Variablen zusätzlich gesetzt werden müssen.

To request support for additional AI model providers, please open an issue or
submit a pull request on GitHub. When contributing, make sure to follow the
[Models Contribution Guideline](../doc/MODELS.md) to keep your provider aligned
with the Smart Workflow ecosystem.

```yaml
@variables.yaml@
```

#### OpenAI-Modelle

<details>

<summary>OpenAI setup instructions</summary>
OpenAI models are natively supported. If you wish to use them import the `smart-workflow-openai` project and define your OpenAI key.

```yaml
@variables.openai@
```
</details>

#### Azure OpenAI-Modelle

<details>

<summary>Azure OpenAI setup instructions</summary>
Azure OpenAI models are supported. To use Azure OpenAI, import the `smart-workflow-azure-openai` project and configure your Azure OpenAI endpoint and deployments.

Jede Bereitstellung in Azure OpenAI stellt eine Modellinstanz mit einem eigenen
API-Schlüssel dar. Sie können mehrere Bereitstellungen konfigurieren, um
verschiedene Modelle für unterschiedliche Aufgaben zu verwenden.

```yaml
@variables.azureopenai@
```

Example Configuration:

```yaml
@variables.azureopenai.example@
```
</details>

#### Google Gemini-Modelle

<details>

<summary>Google Gemini setup instructions</summary>
Google Gemini models are supported. To use Google Gemini, import the `smart-workflow-gemini` project and configure your Gemini API key and default model.
This provider does not support the structured output feature because Google Gemini models do not support structured JSON responses.

```yaml
@variables.gemini@
```

Example Configuration:

```yaml
@variables.gemini.example@
```
</details>

#### x.AI Models

<details>

<summary>x.AI setup instructions</summary>
x.AI models are supported, import the `smart-workflow-xai` to work with these.

```yaml
@variables.xai@
```

Example Configuration:

```yaml
@variables.xai.example@
```

</details>

#### Anthropic Models

<details>

<summary>Anthropic setup instructions</summary>
Claude models (including Claude Opus, Sonnet and Haiku) from Anthropic are supported. Import the `smart-workflow-anthropic` project, configure your API key to get started.

> **Note:** Structured outputs are only supported on Claude Opus 4.6, Claude
> Sonnet 4.6, Claude Sonnet 4.5, Claude Opus 4.5, and Claude Haiku 4.5. Older
> models (e.g., Claude Sonnet 4, Claude Opus 4) do not support this feature.

```yaml
@variables.anthropic@
```

Example Configuration:

```yaml
@variables.anthropic.example@
```

</details>

### File Extraction

Axon Ivy Smart Workflow supports extracting content from PDF and image files
(PNG, JPG, and JPEG) using multimodal LLMs. This allows AI agents to read and
reason over uploaded documents and images directly within your workflows.

Not all providers and models support multimodal input. Refer to the [Models
Contribution Guideline](../doc/MODELS.md#file-extraction-support) for the full
list of supported providers and file types.

### Guardrails

Guardrails protect AI agents by validating user input before it reaches the
model and by checking model outputs before they are used. Smart Workflow
includes the following built-in guardrails:

| Guardrail                       | Type   | Description                                          |
| ------------------------------- | ------ | ---------------------------------------------------- |
| `PromptInjectionInputGuardrail` | Input  | Blocks common prompt injection attacks               |
| `SensitiveDataOutputGuardrail`  | Output | Blocks responses containing API keys or private keys |

#### Configuring Default Guardrails

Set default guardrails in `variables.yaml`:

```yaml
Variables:
  AI:
    Guardrails:
      # Comma-separated list of guardrail names
      DefaultInput: PromptInjectionInputGuardrail
      DefaultOutput: SensitiveDataOutputGuardrail
```

#### Using Guardrails in Agents

In the agent configuration, specify guardrails as a String array:

```java
// Input guardrails
["PromptInjectionInputGuardrail", "MyCustomInputGuardrail"]

// Output guardrails
["SensitiveDataOutputGuardrail", "MyCustomOutputGuardrail"]
```

If no guardrails are specified, the agent uses the default guardrails from
`variables.yaml`.

Smart Workflow also lets you implement custom guardrails and handle guardrail
errors. For more details, see the [Guardrails Guideline](../doc/GUARDRAILS.md).

### Defining Tools

To function effectively, AI agents require tools to perform tasks. Smart
Workflow supports two kinds of tools: **Callable Process Tools** (any tagged
callable sub-process) and **Java Tools** (implement `SmartWorkflowTool` and
register via SPI).

For step-by-step instructions on creating both tool types, see the [Tools
Guide](../doc/TOOLS.md).

### Definition des KI-Agenten

Um einen KI-Agenten zu definieren, erstellen Sie ein Programmelement, das auf
dem Java-Bean „ `com.axonivy.utils.smart.workflow.AgenticProcessCall` ” basiert.
Auf der Registerkarte „ `Configuration` ” können Sie detaillierte Einstellungen
für Ihren KI-Agenten aufrufen und anpassen.

#### Nachricht

Im Abschnitt „ `-Nachricht“ (` ) können Sie die Benutzernachricht und die
Systemnachricht für den Agenten festlegen. Durch die Möglichkeit, Code direkt in
diese Felder einzufügen, bietet Smart Workflow Entwicklern eine bequeme
Möglichkeit, Nachrichten zu definieren, bevor sie an den KI-Dienst gesendet
werden.

![Nachrichtenkonfigurationen](img/agent-message-configurations.png)

#### Tools

Unterhalb des Abschnitts „ `Messages“ (Nachrichten für „ “)` befindet sich der
Abschnitt „ `Tools“ (Tools für „ “)`, in dem Sie die Tools, die der Agent
verwenden soll, als String-Array definieren können. Beispiel:

```java
["findProduct","createProduct","checkProductDependencies", "createProductSearchCriteria"]
```

Wenn keine Tools angegeben sind, geht Smart Workflow standardmäßig davon aus,
dass der Agent alle verfügbaren Tools verwenden kann. Daher wird empfohlen, für
jeden Agenten einen bestimmten Satz von Tools zu definieren, um die
Reaktionsgeschwindigkeit zu verbessern und die Verwendung ungeeigneter Tools zu
verhindern.

#### Modell

Nicht alle KI-Agenten sind gleich. Bei Axon Ivy wissen wir, dass KI-Agenten
Aufgaben unterschiedlicher Komplexität bearbeiten. Einige Agenten führen
einfache Aufgaben aus, wie z. B. das Erstellen von Urlaubsanträgen oder das
Sammeln von Benutzerinformationen, während andere Datenbanken nach Produkten
durchsuchen und Abhängigkeiten wie Lieferanten und Marken bewerten müssen. Daher
ermöglicht Smart Workflow Entwicklern, das zugrunde liegende KI-Modell basierend
auf dem Anwendungsfall auszuwählen.

Geben Sie dazu einfach das gewünschte KI-Modell in den Abschnitt „ `-Modell“`
ein. Wenn kein Modell angegeben ist, verwendet Smart Workflow standardmäßig das
in der Variablen „ `“ definierte Modell AI.OpenAI.Model`.

#### Output

Bei KI-Anwendungen auf Unternehmensebene ist es üblich, dass das Ergebnis des
KI-Agenten in Form eines nutzbaren Objekts vorliegt. Um diesem Bedarf gerecht zu
werden, kann der Smart Workflow KI-Agent die Ausgabe als Java-Objekt erstellen,
das direkt von Axon Ivy-Prozessen verwendet werden kann.

Sie können dies ganz einfach konfigurieren, indem Sie sowohl den erwarteten
Ergebnistyp als auch das Zielobjekt, dem das Ergebnis zugeordnet werden soll, im
Abschnitt „ `-Ausgabe` “ angeben.

![Andere Konfigurationen](img/agent-other-configurations.png)
