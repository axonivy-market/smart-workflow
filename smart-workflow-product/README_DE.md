# Smart Workflow

**Smart Workflow** bringt KI direkt in Axon Ivy, sodass du KI-Agenten in bestehenden Axon-Prozessen erstellen, ausführen und verbessern kannst. Es ermöglicht Geschäftsprozessen, große Sprachmodelle zu nutzen, um natürliche Sprache zu verstehen, autonome Entscheidungen zu treffen und sich an verändernde Anforderungen anzupassen — alles ohne schwerwiegende architektonische Änderungen.

Hauptvorteile von Smart Workflow:

- **Vertraute Einrichtung:** Setze KI-Agenten in BPMN-Prozesse ein, ohne strukturelle Änderungen vorzunehmen, und konfiguriere alles über Axon Ivys Standardschnittstellen.

- **Unternehmenstauglich:** Entwickelt für Unternehmensanforderungen mit Protokollierung, Überwachung und Konfigurationskontrollen.

- **Flexible Tools:** Verwandle jeden aufrufbaren Prozess in ein KI-erkennbares Tool.

- **Multi-Modell-Unterstützung:** Verwende je nach Aufgabe leichtgewichtige oder erweiterte Modelle.

- **Typsichere Ausgaben:** Erzeuge strukturierte Java-Objekte aus KI-Antworten für die sofortige Verwendung.

- **Natürliche Sprachverarbeitung:** Akzeptiere unstrukturierte Eingaben und gib benutzerfreundliche Ausgaben zurück.

**Haftungsausschluss**

Dieser Connector wird als Alpha-Version bereitgestellt und dient ausschließlich zu Test- und Evaluierungszwecken. Er kann Fehler, unvollständige Funktionen oder andere Probleme enthalten, die die Stabilität, Leistung oder Funktionalität beeinträchtigen könnten. Die Nutzung dieses Connectors erfolgt auf dein eigenes Risiko.

**Du bist allein verantwortlich** für die Konfiguration, Bereitstellung und den Betrieb der KI und ihrer zugehörigen Agenten. Alle Entscheidungen, Handlungen oder Ergebnisse, die sich aus der Nutzung dieses Connectors ergeben, liegen vollständig in deiner Verantwortung.

Wir stellen lediglich die **technische Möglichkeit** für solche Konfigurationen bereit und lehnen ausdrücklich jegliche Haftung für Missbrauch, Fehlkonfiguration oder unbeabsichtigte Folgen ab, die sich aus der Nutzung ergeben könnten. Mit der Nutzung dieses Connectors erkennst du diese Einschränkungen an und akzeptierst sie.

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

### Shopping-Demo

Diese Demo zeigt, wie KI die Abläufe eines kleinen E-Commerce-Modegeschäfts transformieren kann. Sie ist etwas fortgeschrittener und kombiniert zwei Mini-Demos: eine zur Produkterstellung und eine zur semantischen Suche. Aufgrund der Komplexität gehen wir hier nicht auf den detaillierten Code oder Schritt-für-Schritt-Anleitungen ein. Wenn du die Implementierung näher kennenlernen möchtest, schau dir bitte das Demo-Projekt `smart-workflow-demo` an.

**Produkterstellung**

Traditionell muss der Shopbetreiber beim Hinzufügen eines Produkts viele Felder manuell ausfüllen und abhängige Datensätze (Lieferant, Marke, Kategorie) prüfen oder erstellen. Für einen kleinen Shop kann dieser Prozess Stunden oder sogar einen ganzen Tag dauern: manuelle Dateneingabe, Suche nach fehlenden Informationen und wiederholtes Prüfen auf Fehler.

Mit den Smart-Workflow-Agenten importierst du einfach die Produktspezifikation und die Bilddateien. Die Agenten übernehmen das Parsen, die Validierung, die Auflösung von Abhängigkeiten und die Produkterstellung – und reduzieren so den manuellen Aufwand und die Zeit bis zur Veröffentlichung erheblich.

Entwickler müssen vier Agenten erstellen:

1. Produkt-Agent

Input: geparste Produktspezifikation

- Tools:
  - Produkt finden: Findet ein Produkt im System
  - Produkt erstellen: Erstellt ein neues Produkt anhand der bereitgestellten Spezifikation
  - Produktabhängigkeiten prüfen: Ruft andere Agenten auf, um Abhängigkeiten (Lieferant, Marke, Kategorie) zu finden und zu validieren

2. Lieferanten-Agent

- Input: Lieferanteninformationen
- Tools:
  - Lieferant finden: Findet einen Lieferanten im System
  - Lieferant erstellen: Erstellt einen neuen Lieferanten mit den bereitgestellten Informationen

3. Kategorie-Agent

- Input: Produktkategorie-Informationen
- Tools:
  - Kategorie finden: Findet eine Kategorie im System
  - Kategorie erstellen: Erstellt eine neue Kategorie mit den bereitgestellten Informationen

4. Marken-Agent

- Input: Markeninformationen
- Tools:
  - Marke finden: Findet eine Marke im System
  - Marke erstellen: Erstellt eine neue Marke mit den bereitgestellten Informationen

Demo-Ablauf

1. Du lädst die Produktspezifikation und die Bilddateien hoch.
2. Smart Workflow parst die Dateien und extrahiert Produktattribute (Titel, SKU, Beschreibung, Preis, Lieferant, Marke, Kategorie, Bilder).
3. Validatoren prüfen Semantik und Anforderungen (Pflichtfelder, Formate, SKU-Eindeutigkeit, Bildanforderungen).
4. Für jede Abhängigkeit (Lieferant, Marke, Kategorie) fragt Smart Workflow den entsprechenden Agenten:
  Wenn die Entität existiert → ID zurückgeben
  Wenn nicht → mit der bereitgestellten Spezifikation erstellen
5. Der Produkt-Agent erstellt das Produkt mit den validierten Attributen und verknüpft die Abhängigkeits-IDs.
6. Das System gibt eine Zusammenfassung zurück und öffnet optional eine Review-Ansicht mit vorausgefüllten Feldern zur finalen Freigabe.

Der neue KI-gestützte Prozess führt zu weniger Fehlern, deutlich weniger manueller Arbeit und einer viel schnelleren Veröffentlichung.

**Semantische Suche**

Vor der KI haben Käufer Stichwortsuchen wie „rotes Kleid“ eingegeben, dann manuell Filter (Preis, Marke, Kategorie) gesetzt und die Ergebnisse durchsucht. Das war nicht nur langsam und starr, sondern hat oft Synonyme, Stile oder die Absicht (z. B. Party vs. Arbeit) nicht richtig erfasst.

Mit semantischer Suche kannst du einfach eine natürliche Anfrage sprechen oder eingeben. Die KI versteht Absicht und Einschränkungen (Farbe, Preis, Anlass, Dringlichkeit), wandelt sie in ein strukturiertes Kriterien-Objekt um. Das Backend übersetzt dieses Objekt in SQL-Filter und liefert passende Ergebnisse zurück. Das bietet Erklärbarkeit, vertraute Tools und eine einfache Implementierung.

Developers need to add an additional `Find product by criteria` tool to the `Product agent` with input is the search criteria.

Entwickler müssen dem Produkt-Agenten ein zusätzliches Tool `Find product by criteria` hinzufügen, dessen Input die Suchkriterien sind.

Demo-Ablauf

1. Käufer: Gibt ein oder sagt „Ich brauche ein rotes Kleid für 100 $ für eine Party heute Abend.“
2. Der `Product agent` extrahiert Attribute und erweitert die Anfrage (Synonyme, akzeptable Preisspanne: 80–120 $).
3. Axon Ivy Business Data wandelt die Kriterien in optimierte Filter um und sucht nach passenden Produkten.
4. Rückgabe der Top-Produkte, die den Kriterien entsprechen.

Um die Demodaten zu initialisieren, öffne die Prozessliste und starte `Create data for shopping demo`.

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

##### Provider

Smart-Workflow ist offen für die Verwendung mit jedem KI-Modell.
Die Auswahl deines Providers erfolgt über die Variable `AI.DefaultProvider`.

```yaml
@variables.yaml@
```

###### OpenAI-Modelle

OpenAI-Modelle werden nativ unterstützt. Wenn du sie verwenden möchtest, importiere das Projekt `smart-workflow-openai` und definiere deinen OpenAI-Schlüssel.

```yaml
@variables.openai@
```

###### Azure OpenAI-Modelle

Azure OpenAI-Modelle werden ebenfalls unterstützt. Um Azure OpenAI zu verwenden, importiere das Projekt `smart-workflow-azure-openai` und konfiguriere deinen Azure OpenAI-Endpunkt und deine Deployments.

Jedes Deployment in Azure OpenAI repräsentiert eine Modellinstanz mit eigenem API-Schlüssel. Du kannst mehrere Deployments konfigurieren, um verschiedene Modelle für unterschiedliche Aufgaben zu verwenden.

```yaml
@variables.azureopenai@
```

**Konfigurationshinweise:**

- `Endpoint`: Dein Azure OpenAI-Ressourcen-Endpunkt (z.B. `https://my-resource.openai.azure.com/`)
- `Deployments`: Definiere ein oder mehrere Modell-Deployments. Jedes Deployment benötigt:
  - Einen eindeutigen Deployment-Namen (verwende Kebab-Case: Kleinbuchstaben, Zahlen, Bindestriche)
  - `Model`: Den Modellnamen (z.B. `gpt-4o`, `gpt-4`)
  - `APIKey`: Den API-Schlüssel für dieses Deployment (verwende die `#[password]`-Annotation für Sicherheit)

Um weitere KI-Modell-Provider einzubinden, frage bitte auf Github danach oder reiche einen Pull-Request ein.

#### Ausgabe

Für KI-Anwendungen auf Unternehmensebene ist es üblich, das Ergebnis des KI-Agenten in Form eines verwendbaren Objekts zu benötigen.
Um diesem Bedarf gerecht zu werden, kann der Smart Workflow KI-Agent Ausgaben als Java-Objekt erzeugen, das direkt von Axon Ivy-Prozessen verwendet werden kann.

Du kannst dies einfach konfigurieren, indem du sowohl den erwarteten Ergebnistyp als auch das Zielobjekt, auf das das Ergebnis abgebildet werden soll, im `Output`-Bereich angibst.

![Andere Konfigurationen](img/agent-other-configurations.png)
