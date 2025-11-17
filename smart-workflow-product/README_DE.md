# #Smart Workflow

**#Smart Workflow** bringt AI direkt hinein #Axon Efeu, so können Entwickler
bauen, gerannt, und verbessern AI Agenten innerhalb #existierend #Axon
Arbeitsgänge. Es lässt Geschäft workflows #aushebeln große Sprache Models zu
verstehen naturale Sprache, machen autonome Entscheide, und adaptieren zu
wechseln alle — Forderungen #ohne schwer architektonische Änderungen.

Wesentliche Nutzen von Pfiffig Workflow:

- **Bekannte Einrichtung:** Fallenlassen AI Agenten hinein BPMN Arbeitsgänge mit
  keinen strukturellen Änderungen und konfigurieren #alles durch #Axon
  Efeustarifliche Schnittstellen.

- **Unternehmen-bereit:** Gebaut für Unternehmen Notwendigkeiten mit #loggen,
  Überwachung, und Konfiguration Aufsichten.

- **Flexible Tools:** Drehen etwas callable verarbeiten hinein ein
  AI-discoverable Tool.

- **Multi-Modellieren unterstützen:** Benutzen leichtgewichtige oder
  fortgeschrittene Models #abhängen auf dem Task.

- **Tippen-zuverlässige Ausgaben:** Erzeugt #gegliedert #Java wendet ein von AI
  Antworten für sofortig Nutzung.

- **Naturales Sprache Handing:** Akzeptieren unstructured #einlesen und
  zurückkehren menschlich-freundliche Ausgabe.

**Disclaimer**

Dieser Anschluss ist versehen wie ein **Alpha Version** und ist #vorhaben für
testen und Evaluation Zwecke nur. Es darf Fehler zügeln, unvollständige
Charakterzüge, oder anderen Sachverhalte dass können angehen Stabilität,
Leistung, oder Funktionalität. Benutzen von diesem Anschluss ist an eure
besitzen Risiko.

Das **Nutzer ist einzig zuständig** für die Konfiguration, #Aufstellen, und
Operation von die AI und sein assoziierte Agenten. Irgendwelche Entscheide,
Aktionen, oder Ergebnisse resultieren von der Nutzung von diesem Anschluss ist
ganz die Verantwortung von dem Nutzer.

Wir versehen nur das **technische Fähigkeit** zu aktivieren solche
Konfigurationen und ausdrücklich disclaim irgendwelche Haftung für missbrauchen,
misconfiguration, oder unbeabsichtigte Konsequenzen entstehen von seiner
Nutzung. Mal benutzen diesen Anschluss, du quittierst und akzeptieren diese
Begrenzungen.

## Demo

### #Axon Efeu Unterstützt Agenten Demo

Diese Demo Vitrinen zu benutzen wie den #Axon Efeu Unterstützt Agenten, einen
AI-#ausrüsten Agenten integriert hinein ein Geschäft workflow. Der Agent ist
gestaltet zu klassieren unterstützen Probleme, überprüfen für verschollen
Auskunft, und schaffen unterstützen automatisch Tasks.

**Workflow Überblick:**

1. **#Einlesen:** Der Agent empfängt eine Unterstützung Frage und den
   Benutzernamen von dem Reporter.
2. **Einordnung:** Es analysiert das Problem, ermittelt ob Auskunft fehlt (wie
   Version), und klassiert den Sachverhalt (Portal, Innenteil, oder Vermarkten
   Produkt).
3. **Beschäftigt Kreation:** Notfalls, der Agent schafft einen Unterstützung
   Task benutzend den `createAxonIvySupportTask` Tool und versieht ein Band zu
   dem geschaffenen Task.
4. **Übersicht & Antwort:** Der Agent fasst zusammen das Problem und erwidert zu
   dem Nutzer mit einer detaillierten Antwort.

**Technische Details:**

- Der Agent ist implementiert wie ein callable Ersatz-verarbeiten
  (`AxonIvySupportAgent.p.json`) Und Nutzungen die
  `com.axonivy.utils.Pfiffig.workflow.AgenticProcessCall` #Java Bohne.
- Der Agent ist konfiguriert zu benutzen ein spezifisches Tool
  (`createAxonIvySupportTask`), welcher erlaubt ihm zu schaffen unterstützen
  automatisch Tasks #innerhalb die workflow. Dies ist erreicht mal den Tool
  Namen präzisieren in den AgentenKonfiguration (sieht unten Beispiel).
- Der Agenthat ausgegeben ist #kartographieren zu #ein #gegliedert #Java wendet
  ein (`AxonIvySupportResponse`), machend ihm leicht zu benutzen #der
  AI-generiert resultiert direkt in #Axon #Ivy verarbeitet. Dieses Objekt zügelt
  typischerweise Details wie die Einordnung, geschaffenes Task Band, und eine
  Übersicht von den Unterstützung Sachverhalt.

**Agent Konfiguration Beispiel:**

Zu konfigurieren den Agenten, definier ein Programmheft Element mit den
folgenden Lagen:

![Unterstützt Karte exanmple](img/support-ticket-example.png)

Diese Konfiguration sichert nur die Agenten Nutzungen die präzisiert Tool und
kehrt zurück wie seine Ausgabe #ein #gegliedert #Java wendet ein.

**Demo Gerannt Beispiel:**

Vermute einen Nutzer unterzieht eine Unterstützung Frage: "Ich habe NPE als
öffne Fall Details in Portal 12.0.9"

1. Der Agent empfängt die Frage und Benutzernamen.
2. Es überprüft für verschollen Auskunft (#z.B., Version), klassiert den
   Sachverhalt da ein Portal Problem, und ermittelt dass einen Unterstützung
   Task sollte sein geschafft.
3. Die Agent Anrufe die `createAxonIvySupportTask` Tool, welcher schafft eine
   neue Unterstützung Task und kehrt zurück ein Band zu ihm.
4. Der Agent fasst zusammen das Problem und versieht wie eine Antwort:

```text
Classification: Portal
Summary: The problem is a NullPointerException (NPE) occurring when opening Case Details in Portal version 12.0.9. Since the issue is related to the Portal product and the version is provided, a support task has been created to address this problem.
```

Diese Antwort ist #kartographieren zu die `AxonIvySupportResponse` wendet ein
und kann sein benutzt direkt herein folgend workflow Stufen.

Wie zu Rennen die Demo:

1. Sicher dir vervollständigt hast das [Konfigurationen](#configurations)
   Sektion.
2. Lös aus den #Axon Efeu Unterstützt Agenten verarbeitet mit eine Unterstützung
   Frage und Benutzernamen.
3. Überprüf den AgentenAntwort, welcher schließt ein Einordnung, beschäftigen
   Kreation (#erforderlichenfalls), und eine Übersicht.

### Shoppend Demo

Diese Demo Vitrinen wie AI können umsetzen die Operationen von einem kleinen
#E-Commerce Mode lagert. Es ist fortgeschrittener und verquickt zwei mini-Demos:
#Man auf Produkt Kreation und anderer auf semantisch Suche. Punkto seinen
Umfang, wir wollen nicht hinein den detaillierten Code springen oder
schreiten-mal-schreiten hier Weisungen. Ob du wolltest mögen zu erkunden die
Ausführung, bitte auschecken das Demo Projekt `pfiffig-workflow-Demo`.

**Produkt Kreation**

Traditionell, #zufügen ein Produkt bedürft den Vorrat Bediener zu manuell füllen
viele Felder und zu validieren oder schaffen abhängige Schallplatten
(Zulieferer, #einbrennen, Kategorie). Für einen kleinen Vorrat dieser
Arbeitsgang kann Stunden oder einen vollen Tag nehmen: Manueller #Daten Eintrag,
jagen für verschollen info, und re-überprüfen für Fehler.

Mit Pfiffig Workflow Agenten, der Bediener importiert einfach die Produkt
Beschreibung und Image Dateien. Der Agenten Henkel #Syntaxanalyse, Bestätigung,
Kolonie Resolution, und Produkt Kreation — #heruntersetzen bedeutsam manuelle
Arbeit und messen-zu-verlegen.

Entwickler brauchen zu schaffen vier Agenten

1. Produkt Agent

- Input: Zerlegt Produkt Beschreibung
- Tools:
  - Finde Produkt: Finde Produkt in dem System
  - Schaff Produkt: Schaff ein neues Produkt benutzend das versehen Beschreibung
  - Überprüfen Produkt Kolonien: Ruf anderen Agenten zu finden und validieren
    Kolonien (Zulieferer, #einbrennen, und Kategorie)

2. Zulieferer Agent

- Input: Zulieferer Auskunft
- Tools:
  - Finde Zulieferer: Finde Zulieferer in dem System
  - Schaff Zulieferer: Schaff einen neuen Zulieferer benutzend den versehen
    Auskunft

3. Kategorie Agent

- Input: Produktkategorie Auskunft
- Tools:
  - Finde Kategorie: Finde Kategorie in dem System
  - Schaff Kategorie: Schaff eine neue Kategorie benutzend die versehen Auskunft

4. #Einbrennen Agenten

- Input: Produkt #einbrennen Auskunft
- Tools:
  - #Finden #einbrennen: #Finden #einbrennen in dem System
  - #Schaffen #einbrennen: Schaff ein neues Brandzeichen benutzend das versehen
    Auskunft

Demo mündet

1. Bediener #Hochladen Produkt Beschreibung und Image Dateien.
2. #Smart Workflow zerlegt die Dateien, gewinnen Produkt Attribute (benennen,
   SKU, Steckbrief, #auspreisen, Zulieferer info, #einbrennen, Kategorie,
   Images).
3. Prüfer überprüfen Semantiken und Zwangsbedingungen (bedürft #auffangen,
   Formate, SKU Einmaligkeit, Image Forderungen).
4. Für jede Kolonie (Zulieferer, #einbrennen, Kategorie), #Smart Workflow fragt
   der bereitstellen Agenten: Ob die Entität existiert → #zurückkehren die ID,
   ob verschollen → schaffen ihm benutzend die versehen spec.
5. Produkt Agent schafft das Produkt mit validiert #beimessen und verbindet zu
   Kolonie IDs.
6. System kehrt zurück eine Übersicht und gegebenenfalls öffnet ein
   menschliches-überprüfen #durchsieben mit prefilled Felder für endgültig
   Genehmigung.

Das neues AI-#ausrüsten verarbeiten resultiert in #wenige Fehler, weit #kleine
manuelle Arbeit, und eine #viel #schnell Zeit-zu-verlegen.

**Semantische Suche**

Bevor AI, Käufer getippt Stichwort Anfragen mögen “rotes Kleid,” dann manuell
#anwendungsbezogen Filter (#auspreisen, #einbrennen, Kategorie) und scannte die
Resultate. Dieser Arbeitsgang war nicht nur hemmen und rigide aber auch oft
gescheitert zu einfangen Synonyme, Stile, oder Absicht (#z.B., Party vs.
Arbeit).

Mit semantisch Suche der Nutzer spricht oder tippt eine naturale Bitte. AI
Versteht Absicht und Zwangsbedingungen (färben, #auspreisen, #Anlass sein,
Dringlichkeit), rechnet um jener hinein #ein #gegliedert Kriterien einwenden.
Das backend dann rechnet um jenes Objekt hinein SQL Satzaussagen und Rückgaben
passten Resultate. Bietet an explainability, bekannte #Werkzeugbereitstellung,
und #leicht #Aufstellen.

Entwickler brauchen zu zufügen ein zuzügliches `Finden Produkt bei Kriterien`
Tool zu das `Produkt Agenten` mit #einlesen ist die Suche Kriterien.

Demo mündet

1. Käufer: Tippt oder sagt “ich brauche ein $100 rotes Kleid für eine Party
   heute Abend.”
2. `Produkt Agent` gewinnt #beimessen und expandiert die Anfrage (Synonyme,
   akzeptabler Preis Bereich: $80–$120).
3. #Axon Efeu Dienstliche #Daten Drehungen Kriterien hinein #ein optimiert
   filtern und #suchen die Produkte.
4. Zurückkehren die #oberste Produkte passten Kriterien.

Zu schnell aufstellen #der Demo #Daten, renn den Arbeitsgang `Schafft #Daten für
shoppen Demo` von die Arbeitsgang Liste.

## Einrichtung

### Konfigurationen

Vor Start arbeiten mit Pfiffig Workflow, du brauchst zu versehen einige
Konfigurationen benutzend #Axon Efeu Variablen:

- `AI.OpenAI.APIKey`: API #Eintasten von eure OpenAI verrechnen.
- `AI.OpenAI.Model`: #Voreinstellen OpenAI modellieren. Zurzeit unterstützen wir
  `gpt-4o`, `gpt-4.1`, `gpt-4.1-mini`, `gpt-4.1-nano`, und `gpt-5` Models.

### Definierend Tools mit Callable Arbeitsgänge

Zu funktionieren effektiv, AI Agenten bedürfen Tools zu aufführen Tasks. Mit
Pfiffig Workflow, schaffend ein Tool ist direkt: Einfach definier ein callable
verarbeiten und zufügen das `Tool` markiert zu ihm.

Zu auswählen das bereitstellen Tool, AI Agenten vertrauen auf den Steckbriefen
von callable Arbeitsgänge. Zu sichern leistungsstarke Tool Auslese, deutlich
beschreiben das ToolsZweck herein den `Steckbrief` #auffangen.

![Tool Konfigurationen](img/tool-configurations.png)

### Definierend AI Agenten

Zu definieren ein AI Agenten, schaff ein Programmheft Element unterstützte mal
das `com.axonivy.utils.Pfiffig.workflow.AgenticProcessCall` #Java Bohne. Herein
das `Konfiguration` Deckel, du können zugreifen und anpassen detaillierte Lagen
für eure AI Agenten.

#### Meldung

Herein das `Meldung` Sektion, du kannst die Nutzer Meldung und System Meldung
präzisieren für den Agenten. Mal erlauben Code Injektion direkt hinein diese
Felder, #Smart Workflow bietet an einen angemessenen Weg für Entwickler zu
definieren Meldungen vor sie sind gesandt zu die AI #bespringen.

![Meldung Konfigurationen](img/agent-message-configurations.png)

#### Tools

Unten das `Meldungen` Sektion ist die `Tools` Sektion, #wo kannst du definieren
den Apparat Tools den Agenten sollte da einen Schnur Bereich benutzen.
Beispielsweise:

```java
["findProduct","createProduct","checkProductDependencies", "createProductSearchCriteria"]
```

Bei #voreingestellt, ob keine Tools sind präzisiert, #Smart Workflow vermutet
den Agenten kann benutzen jede verfügbare Tools. Deswegen, es ist
weiterempfohlen zu definieren einen spezifischen Apparat von Tools für jeden
Agenten zu verbessern Antwort Geschwindigkeit und #zurückhalten die Nutzung von
unpassend Tools.

#### Model

Nicht jede AI Agenten sind geschafft #Gleichgestellter. In #Axon Efeu, wir
erkennen jener AI Agenten Henkel Tasks von verschieden Umfang. Einige Agenten
aufführen simple Tasks, wie schaffen Abschied Bitten oder sammelnd Nutzer
Auskunft, #während müssen andere suchen Datenbanken für Produkte und auswerten
Kolonien wie Zulieferer und Brandzeichen. Deswegen, #Smart Workflow erlaubt
Entwickler zu auswählen das zugrundeliegendes AI Model gegründet auf den Nutzung
Fall.

Zu tun dieses, einfach betreten das gewünscht AI Model herein das `Model`
Sektion. Bei #voreingestellt, ob kein Model ist präzisiert, #Smart Workflow
benutzt das Model definiert in der Variable `AI.OpenAI.Model`.

##### Provider

#Smart-Workflow ist öffnet zu rennen mit #irgendein AI Model. Die Auslese von
eurem Provider ist getan mit der Variable `AI.DefaultProvider`.

```yaml
@variables.yaml@
```

###### OpenAI Models

OpenAI Models sind #einheimisch unterstützt. Ob du wünschst zu benutzen jene
importieren das `pfiffig-workflow-openai` projizieren und definieren eure OpenAI
Schlüssel.

```yaml
@variables.openai@
```

###### Himmelblau OpenAI Models

Himmelblau OpenAI Models sind unterstützt. Zu benutzen Himmelblau OpenAI,
importieren das `pfiffig-workflow-Azur-openai` projiziert und konfigurieren eure
Himmelblaues OpenAI Endpunkt und #Aufstellen.

Jedes #Aufstellen in Himmelblau OpenAI vertritt eine Model Instanz mit sein
eigenes API Schlüssel. Du kannst mehrfache #Aufstellen konfigurieren zu benutzen
verschiedene Models für verschieden Tasks.

```yaml
@variables.azureopenai@
```

**Beispiel Konfiguration:**

```yaml
@variables.azureopenai.example@
```

###### #Google #Zwillinge Models

#Google #Zwillinge Models sind unterstützt. Zu benutzen #Google #Zwillinge,
importiert den `pfiffig-workflow-gemini` projiziert und konfigurieren euren
#Zwillinge API Schlüssel und #voreingestellt Model.

```yaml
@variables.gemini@
```

**Beispiel Konfiguration:**

```yaml
@variables.gemini.example@
```

Zu einschreiben #andere AI Model Provider, bitte erbitten ihm weiter Github oder
Datei benutzt einen Zug-Forder auf.

#### Ausgabe

Für Unternehmen-passt an AI Anträge, es ist allgemein zu bedürfen die AI Agenten
Resultat in der Form von einem nutzbaren Objekt. Zu adressieren diese
Notwendigkeit, der #Smart Workflow AI Agent kann erzeugen ausgegeben da wendet
ein #ein #Java, bereit zu sein benutzt direkt bei #Axon #Ivy verarbeitet.

Du kannst sicher konfigurieren dies mal präzisieren sowohl das voraussichtliche
Resultat Typ und das Soll Objekt zu #kartographieren das Resultat zu herein das
`Ausgabe` Sektion.

![Anderen Konfigurationen](img/agent-other-configurations.png)
