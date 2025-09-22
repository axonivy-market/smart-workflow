# Smart Workflow

**Smart Workflow** bringt KI direkt in Axon Ivy, sodass du KI-Agenten in bestehenden Axon-Prozessen erstellen, ausführen und verbessern kannst. Es ermöglicht Geschäftsprozessen, große Sprachmodelle zu nutzen, um natürliche Sprache zu verstehen, autonome Entscheidungen zu treffen und sich an verändernde Anforderungen anzupassen — alles ohne schwerwiegende architektonische Änderungen.

Hauptvorteile von Smart Workflow:

- **Vertraute Einrichtung:** Setze KI-Agenten in BPMN-Prozesse ein, ohne strukturelle Änderungen vorzunehmen, und konfiguriere alles über Axon Ivys Standardschnittstellen.

- **Unternehmenstauglich:** Entwickelt für Unternehmensanforderungen mit Protokollierung, Überwachung und Konfigurationskontrollen.

- **Flexible Tools:** Verwandle jeden aufrufbaren Prozess in ein KI-erkennbares Tool.

- **Multi-Modell-Unterstützung:** Verwende je nach Aufgabe leichtgewichtige oder erweiterte Modelle.

- **Typsichere Ausgaben:** Erzeuge strukturierte Java-Objekte aus KI-Antworten für die sofortige Verwendung.

- **Natürliche Sprachverarbeitung:** Akzeptiere unstrukturierte Eingaben und gib benutzerfreundliche Ausgaben zurück.

## Demo

### Axon Ivy Support Agent Demo

Diese Demo zeigt dir, wie du den Axon Ivy Support Agent verwendest, einen KI-gestützten Agenten, der in einen Geschäftsprozess integriert ist. Der Agent ist darauf ausgelegt, Support-Probleme zu klassifizieren, fehlende Informationen zu überprüfen und automatisch Support-Aufgaben zu erstellen.

**Workflow-Übersicht:**

1. **Eingabe:** Der Agent erhält eine Support-Frage und den Benutzernamen des Meldenden.
2. **Klassifizierung:** Er analysiert das Problem, bestimmt, ob Informationen fehlen (wie die Version), und klassifiziert das Problem (Portal, Core oder Market-Produkt).
3. **Aufgabenerstellung:** Falls erforderlich, erstellt der Agent eine Support-Aufgabe mit dem `createAxonIvySupportTask`-Tool und stellt einen Link zur erstellten Aufgabe bereit.
4. **Zusammenfassung & Antwort:** Der Agent fasst das Problem zusammen und antwortet dem Benutzer mit einer detaillierten Antwort.

**Technische Details:**

- Der Agent ist als aufrufbarer Unterprozess (`AxonIvySupportAgent.p.json`) implementiert und verwendet die `com.axonivy.utils.smart.workflow.AgenticProcessCall` Java Bean.
- Der Agent ist so konfiguriert, dass er ein spezifisches Tool (`createAxonIvySupportTask`) verwendet, das es ihm ermöglicht, automatisch Support-Aufgaben innerhalb des Workflows zu erstellen. Dies wird erreicht, indem der Tool-Name in der Konfiguration des Agenten angegeben wird (siehe Beispiel unten).
- Die Ausgabe des Agenten wird auf ein strukturiertes Java-Objekt (`AxonIvySupportResponse`) abgebildet, was es einfach macht, das KI-generierte Ergebnis direkt in Axon Ivy-Prozessen zu verwenden. Dieses Objekt enthält typischerweise Details wie die Klassifizierung, den Link zur erstellten Aufgabe und eine Zusammenfassung des Support-Problems.

**Beispiel für Agent-Konfiguration:**

Um den Agenten zu konfigurieren, definiere ein Programmelement mit den folgenden Einstellungen:

![Support Ticket Beispiel](img/support-ticket-example.png)

Diese Konfiguration stellt sicher, dass der Agent nur das angegebene Tool verwendet und seine Ausgabe als strukturiertes Java-Objekt zurückgibt.

**Demo-Ausführungsbeispiel:**

Angenommen, ein Benutzer stellt eine Support-Frage: "Ich habe eine NPE beim Öffnen der Case Details im Portal 12.0.9"

1. Der Agent erhält die Frage und den Benutzernamen.
2. Er überprüft fehlende Informationen (z.B. Version), klassifiziert das Problem als Portal-Problem und bestimmt, dass eine Support-Aufgabe erstellt werden sollte.
3. Der Agent ruft das `createAxonIvySupportTask`-Tool auf, das eine neue Support-Aufgabe erstellt und einen Link dazu zurückgibt.
4. Der Agent fasst das Problem zusammen und gibt eine Antwort wie diese:

```text
Klassifizierung: Portal
Zusammenfassung: Das Problem ist eine NullPointerException (NPE), die beim Öffnen der Case Details in Portal Version 12.0.9 auftritt. Da das Problem mit dem Portal-Produkt zusammenhängt und die Version angegeben ist, wurde eine Support-Aufgabe erstellt, um dieses Problem zu behandeln.
```

Diese Antwort wird auf das `AxonIvySupportResponse`-Objekt abgebildet und kann direkt in nachfolgenden Workflow-Schritten verwendet werden.

So führst du die Demo aus:

1. Stelle sicher, dass du den Abschnitt [Konfigurationen](#konfigurationen) abgeschlossen hast.
2. Löse den Axon Ivy Support Agent-Prozess mit einer Support-Frage und einem Benutzernamen aus.
3. Überprüfe die Antwort des Agenten, die Klassifizierung, Aufgabenerstellung (falls erforderlich) und eine Zusammenfassung enthält.

## Setup

### Konfigurationen

Bevor du mit Smart Workflow arbeitest, musst du einige Konfigurationen mit Axon Ivy-Variablen bereitstellen:

- `AI.OpenAI.APIKey`: API-Schlüssel deines OpenAI-Kontos.
- `AI.OpenAI.Model`: Standard-OpenAI-Modell. Derzeit unterstützen wir die Modelle `gpt-4o`, `gpt-4.1`, `gpt-4.1-mini`, `gpt-4.1-nano` und `gpt-5`.

### Tools mit aufrufbaren Prozessen definieren

Um effektiv zu funktionieren, benötigen KI-Agenten Tools zur Ausführung von Aufgaben. Mit Smart Workflow ist das Erstellen eines Tools einfach: Definiere einfach einen aufrufbaren Prozess und füge das `tool`-Tag hinzu.

Um das geeignete Tool auszuwählen, verlassen sich KI-Agenten auf die Beschreibungen aufrufbarer Prozesse. Um eine effiziente Tool-Auswahl zu gewährleisten, beschreibe den Zweck des Tools klar im `description`-Feld.

![Tool-Konfigurationen](img/tool-configurations.png)

### KI-Agent definieren

Um einen KI-Agenten zu definieren, erstelle ein Programmelement, das von der `com.axonivy.utils.smart.workflow.AgenticProcessCall` Java Bean unterstützt wird. Im `Configuration`-Tab kannst du auf detaillierte Einstellungen für deinen KI-Agenten zugreifen und diese anpassen.

#### Nachricht

Im `Message`-Bereich kannst du die Benutzernachricht und Systemnachricht für den Agenten angeben. Durch die Möglichkeit, Code-Injection direkt in diese Felder zu ermöglichen, bietet Smart Workflow Entwicklern eine bequeme Möglichkeit, Nachrichten zu definieren, bevor sie an den KI-Service gesendet werden.

![Nachrichten-Konfigurationen](img/agent-message-configurations.png)

#### Tools

Unter dem `Messages`-Bereich befindet sich der `Tools`-Bereich, wo du die Menge der Tools definieren kannst, die der Agent als String-Array verwenden soll. Zum Beispiel:

```java
["findProduct","createProduct","checkProductDependencies", "createProductSearchCriteria"]
```

Standardmäßig, wenn keine Tools angegeben sind, geht Smart Workflow davon aus, dass der Agent alle verfügbaren Tools verwenden kann. Daher wird empfohlen, eine spezifische Menge von Tools für jeden Agenten zu definieren, um die Antwortgeschwindigkeit zu verbessern und die Verwendung ungeeigneter Tools zu verhindern.

#### Modell

Nicht alle KI-Agenten sind gleich. In Axon Ivy erkennen wir, dass KI-Agenten Aufgaben unterschiedlicher Komplexität bewältigen. Einige Agenten führen einfache Aufgaben aus, wie das Erstellen von Urlaubsanträgen oder das Sammeln von Benutzerinformationen, während andere Datenbanken nach Produkten durchsuchen und Abhängigkeiten wie Lieferanten und Marken bewerten müssen. Daher ermöglicht Smart Workflow Entwicklern, das zugrunde liegende KI-Modell basierend auf dem Anwendungsfall auszuwählen.

Gib dazu einfach das gewünschte KI-Modell im `Model`-Bereich ein. Standardmäßig, wenn kein Modell angegeben ist, verwendet Smart Workflow das in der Variable `AI.OpenAI.Model` definierte Modell.

#### Ausgabe

Für KI-Anwendungen auf Unternehmensebene ist es üblich, das Ergebnis des KI-Agenten in Form eines verwendbaren Objekts zu benötigen.
Um diesem Bedarf gerecht zu werden, kann der Smart Workflow KI-Agent Ausgaben als Java-Objekt erzeugen, das direkt von Axon Ivy-Prozessen verwendet werden kann.

Du kannst dies einfach konfigurieren, indem du sowohl den erwarteten Ergebnistyp als auch das Zielobjekt, auf das das Ergebnis abgebildet werden soll, im `Output`-Bereich angibst.

![Andere Konfigurationen](img/agent-other-configurations.png)

### KI-Aktivitäten-Protokollierung aktivieren

Um KI-Aktivitäten zu überwachen und zu verwalten, kannst du eine dedizierte Protokollierung für Smart Workflows KI-Agenten aktivieren.
Dies geschieht durch Änderung der Standard-`configuration\log4j2.xml`-Datei in deiner Designer- oder Engine-Installation wie folgt:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
  <Appenders>
    <RollingRandomAccessFile name="AIlog" fileName="logs/ai.log" filePattern="logs/ai-%d{yyyy-MM-dd}.log.gz" ignoreExceptions="false">
      <PatternLayout pattern="${pattern}" />
      <TimeBasedTriggeringPolicy />
    </RollingRandomAccessFile>
  </Appenders>

  <Loggers>
    <Logger name="dev.langchain4j.http.client.log.LoggingHttpClient" level="trace" includeLocation="false" additivity="false">
      <AppenderRef ref="AIlog" />
      <AppenderRef ref="ConsoleLog" />
    </Logger>
  </Loggers>
</Configuration>
```

Nach dem Neustart von Axon Ivy werden alle KI-Aktivitäten in der `logs/ai.log`-Datei zur einfachen Verfolgung und Analyse aufgezeichnet.
