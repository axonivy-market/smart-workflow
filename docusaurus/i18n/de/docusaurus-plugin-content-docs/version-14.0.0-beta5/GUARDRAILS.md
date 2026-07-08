# Leitplanken

Sicherheitsvorkehrungen schützen KI-Agenten, indem sie sowohl Benutzereingaben als auch KI-Ausgaben überprüfen. Smart Workflow bietet integrierte Sicherheitsvorkehrungen für gängige Sicherheitsbedenken und unterstützt benutzerdefinierte Sicherheitsvorkehrungen über SPI.

**Integrierte Leitplanken:**

| Leitplanke                                       | Typ     | Beschreibung                                                                                                                                                                                                                                                 |
| ------------------------------------------------ | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `PromptInjectionInputGuardrail`                  | Eingabe | Blockiert gängige Prompt-Injection-Angriffe mithilfe von Regex-Mustern. Geringe Latenz, keine LLM-Kosten. Als grundlegende erste Verteidigungslinie einsetzbar.                                                                                              |
| `AiPromptInjectionInputGuardrail`                | Eingabe | LLM-basierter Klassifikator, der subtile Injektionen erkennt, die von regulären Ausdrücken übersehen werden – Roleplay-Jailbreaks, Autoritäts-Spoofing, narrative Payloads, schrittweise Abweichungen. Einsatz, wenn ein strengerer Schutz erforderlich ist. |
| `Schutzmaßnahme für die Ausgabe sensibler Daten` | Ausgabe | Blockiert Antworten, die API-Schlüssel oder private Schlüssel enthalten                                                                                                                                                                                      |

### Auswahl einer Eingabeschutzbarriere

|                                         | `PromptInjectionInputGuardrail`  | `AiPromptInjectionInputGuardrail`                                                                                                     |
| --------------------------------------- | -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| **Nachweismethode**                     | Regex-Muster                     | LLM-Klassifikator                                                                                                                     |
| **Fänge**                               | Schlüsselwortbasierte Angriffe   | All das oben Genannte + Rollenspiele, Autoritätsbehauptungen, narrative Botschaften, Verschleierungstaktiken                          |
| **Falsch-positive Ergebnisse**          | Niedrig (verengte Muster)        | Sehr gering (absichtsorientiert)                                                                                                      |
| **Latenz**                              | ~0 ms                            | +LLM-Aufruf pro Nachricht                                                                                                             |
| **Kosten**                              | Kostenlos                        | Token-Kosten (verwenden Sie `AI.Guardrails.PromptInjection.Classifier.Provider` + `Model`, um ein kostengünstiges Modell festzulegen) |
| **Wann ist die Verwendung angebracht?** | Standard / allgemeine Verwendung | Hochsichere Implementierungen, Chatbots mit Kundenkontakt                                                                             |

### `konfigurieren: AiPromptInjectionInputGuardrail`

Vier Variablen beeinflussen Kosten, Deckungsumfang und Klassifizierungsverhalten:

```yaml
Variablen:
  AI:
    Guardrails:
      PromptInjection:
        Classifier:
          # KI-Anbieter für den Klassifikator. Wenn leer, wird auf „AI.DefaultProvider“ zurückgegriffen.
          # Verwenden Sie einen Anbieter, der kostengünstige, schnelle Modelle anbietet (z. B. OpenAI für gpt-4.1-nano).
          Provider: ""
          # Ein kostengünstigeres Modell für den Klassifikator festlegen, um die Token-Kosten zu senken.
          # Wenn das Feld leer ist, wird das Standardmodell des Anbieters verwendet.
          Model: "gpt-4.1-nano"
          # Benutzerdefinierte System-Eingabeaufforderung für den JA/NEIN-Klassifikator.
          # Wenn das Feld leer ist, wird die integrierte Eingabeaufforderung verwendet (deckt 8 Angriffskategorien und 5 sichere Kategorien ab).
          # Das Modell muss angewiesen werden, nur mit JA oder NEIN zu antworten.
          SystemPrompt: ""
          # Erlaubt Nachrichten, die kürzer als diese Zeichenanzahl sind, ohne einen LLM-Aufruf.
          # Der Standardwert ist 0 (alle Nachrichten werden ausgewertet). Erhöhen Sie diesen Wert, um das LLM
          # bei sehr kurzen Nachrichten zu überspringen, sobald Sie Ihre Verkehrsmuster verstanden haben.
          MinLength: "0"
```

#### Anpassen der Systemaufforderung

Die integrierte Eingabeaufforderung deckt allgemeine Muster für Eingabeaufforderungs-Injektionen ab. Für domänenspezifische Einsatzszenarien müssen Sie sie möglicherweise erweitern – beispielsweise bei einem Finanz-Chatbot, der auch Versuche blockieren soll, den „Beratermodus“ ohne Compliance-Prüfungen aufzurufen, oder bei einem Support-Bot, der Versuche zurückweisen soll, sich als interne Mitarbeiter auszugeben.

Setzen Sie „ `“ und „SystemPrompt` “ auf Ihren eigenen Text. Die Eingabeaufforderung „ **“ muss** mit der Anweisung enden, nur mit „ `YES` “ oder „ `NO` “ zu antworten:

```
Du bist ein Klassifikator mit sofortiger Eingabe für einen Chatbot im Finanzdienstleistungsbereich.
[... deine benutzerdefinierten Regeln ...]
Antworte NUR mit JA oder NEIN.
```

Lassen Sie das Feld leer, um die integrierte Eingabeaufforderung zu verwenden.

> **Wichtig:** Der Klassifikator muss mit `„YES“` oder `„NO“` antworten. Wenn das Modell etwas anderes zurückgibt (z. B. einen Satz), blockiert die Schutzmaßnahme **die Nachricht vorsorglich** und protokolliert eine Warnung, um Sie auf die Fehlkonfiguration hinzuweisen.

## Standard-Sicherheitsgrenzen konfigurieren

Legen Sie Standard-Guardrails in der Datei „variables.yaml“ unter `` fest. Diese gelten für jeden Agenten, der **nicht** seine eigene Guardrail-Liste explizit konfiguriert:

```yaml
Variablen:
  AI:
    Guardrails:
      # Durch Kommas getrennte Liste von Guardrail-Namen
      DefaultInput: PromptInjectionInputGuardrail
      DefaultOutput: SensitiveDataOutputGuardrail
```

## Verwendung von Guardrails in Agenten

Geben Sie in der Agentenkonfiguration die Guardrails als String-Array an:

```java
// Eingabe-Sicherheitsgrenzen
["PromptInjectionInputGuardrail", "MyCustomInputGuardrail"]

// Ausgabe-Sicherheitsgrenzen
["SensitiveDataOutputGuardrail", "MyCustomOutputGuardrail"]
```

Wenn keine Guardrails angegeben sind, verwendet der Agent die Standard-Guardrails aus der Datei „variables.yaml“ unter `` .

## Implementierung benutzerdefinierter Sicherheitsvorkehrungen

### Maßgefertigte Eingabeschutzleiste

1. Erstellen Sie eine Klasse, die „ `“ und „SmartWorkflowInputGuardrail“ implementiert:`:

```java
Paket com.example.guardrails;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

public class MyCustomInputGuardrail implements SmartWorkflowInputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    if (containsBadContent(message)) {
      return GuardrailResult.block("Nachricht enthält unzulässigen Inhalt");
    }
    return GuardrailResult.allow();
  }

  private boolean containsBadContent(String message) {
    // Ihre Validierungslogik
    return false;
  }
}
```

### Maßgefertigte Leitplanke

1. Erstellen Sie eine Klasse, die „ `“ und „SmartWorkflowOutputGuardrail“ implementiert:`:

```java
Paket com.example.guardrails;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

public class MyCustomOutputGuardrail implements SmartWorkflowOutputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    if (containsSensitiveData(message)) {
      return GuardrailResult.block("Antwort enthält sensible Daten");
    }
    return GuardrailResult.allow();
  }

  private boolean containsSensitiveData(String message) {
    // Ihre Validierungslogik
    return false;
  }
}
```

### Leitplanken über den Anbieter registrieren

2. Erstellen Sie einen „ `“ GuardrailProvider`, um Ihre benutzerdefinierten Leitplanken bereitzustellen:

> **Wichtig:** Ihr Projekt muss einen „ `“ GuardrailProvider` über SPI registrieren, damit Smart Workflow Ihre benutzerdefinierten Guardrails erkennen und laden kann. Ohne einen registrierten Provider stehen Ihre Guardrails den Agenten nicht zur Verfügung.

```java
Paket com.example.guardrails;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;

public class MyGuardrailProvider implements GuardrailProvider {

  @Override
  public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
    return List.of(new MyCustomInputGuardrail());
  }

  @Override
  public List<SmartWorkflowOutputGuardrail> getOutputGuardrails() {
    return List.of(new MyCustomOutputGuardrail());
  }
}
```

3. Registrieren Sie den Anbieter unter `src/META-INF/services/com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider`:

   ```text
   com.example.guardrails.MyGuardrailProvider
   ```

   Diese SPI-Registrierung ist erforderlich ( **), damit Smart Workflow Ihre Guardrails erkennen und laden kann (** ). Der Anbieter wird automatisch geladen, wenn Agenten Guardrails anhand ihres Namens anfordern.

## Beobachtbarkeit von Guardrail

Smart Workflow protokolliert die Ausführung von Sicherheitsmaßnahmen sowohl für Governance-Prüfungen als auch für die externe Telemetrie.

### Aufzeichnung der Geschichte von Ivy

Wenn „ `“ („AI.Observability.Ivy.Enabled“)` auf „ `“ („true“)` gesetzt ist, wird jede Guardrail-Ausführung im Konversationsverlauf des Agenten protokolliert. Jeder Datensatz enthält:

- **guardrailName** – Der Name der Leitplankenklasse
- **Geben Sie Folgendes ein:** - `EINGABE` oder `AUSGABE`
- **Ergebnis** – `ERFOLG`, `FEHLER` oder `FATAL`
- **Nachricht** – Der validierte Inhalt (Benutzerabfrage für Eingabevorgaben, KI-Antwort für Ausgabevorgaben)
- **failureMessage** – Der Grund, warum eine Schutzbarriere blockiert (null bei Erfolg)
- **durationMs** – Ausführungszeit in Millisekunden
- **executedAt** – Zeitstempel der Ausführung

Guardrail-Datensätze werden zusammen mit den Tool-Ausführungen im „ `“ unter „AgentConversationEntry“` gespeichert und sind im Agentenverlaufsbaum einsehbar.

### OpenInference-Tracing (Arize Phoenix)

Wenn `AI.Observability.Openinference.Enabled` auf `true` gesetzt ist, erzeugt jede Guardrail-Ausführung einen eigenen Span mit `openinference.span.kind = "GUARDRAIL"`. Diese Spans werden in Arize Phoenix neben den LLM-Spans angezeigt und bieten so einen vollständigen Ablaufverlauf der KI-Interaktion einschließlich der Sicherheitsprüfungen.

Eigenschaften der Leitplanken-Spannweite:

| Attribut                   | Beschreibung                                                 |
| -------------------------- | ------------------------------------------------------------ |
| `openinference.span.kind`  | `LEITPLANKE`                                                 |
| `validator_name`           | Der Name der Guardrail-Klasse (Phoenix-Konvention)           |
| `validator_on_fail`        | Verhalten bei Fehlern — immer eine „ ` “ auslösen`           |
| `Leitplankenart`           | `EINGABE` oder `AUSGABE`                                     |
| `Leitplanke.Ergebnis`      | `ERFOLG`, `FEHLER` oder `FATAL`                              |
| `Leitplanke.Fehlermeldung` | Fehlerursache (wird nur bei Sperrung angezeigt)              |
| `input.value`              | Der validierte Inhalt (Benutzeranfrage oder KI-Antwort)      |
| `output.value`             | `„bestanden“` oder ` „nicht bestanden“` (Phoenix-Konvention) |

## Umgang mit Leitplankenfehlern

Wenn eine Guardrail die Eingabe oder Ausgabe blockiert, wird eine Ausnahme mit dem Fehlercode „ `“ ausgelöst: „smartworkflow:guardrail:input:violation“` oder „ `“ „smartworkflow:guardrail:output:violation“`. Sie können dies mithilfe eines „Error Boundary“-Ereignisses behandeln:

1. Fügen Sie Ihrem Element „ `“ („AgenticProcessCall“` ) ein „ **“-Fehlergrenzenereignis („** “) hinzu.
2. Konfigurieren Sie es so, dass der Fehlercode erfasst wird: `smartworkflow:guardrail:input:violation` oder `smartworkflow:guardrail:output:violation`.
3. Implementieren Sie Ihre Fehlerbehandlungslogik (z. B. Anzeige einer benutzerfreundlichen Meldung, Protokollierung des Vorfalls, erneuter Versuch mit anderen Eingabedaten).

Ein funktionierendes Beispiel finden Sie im Prozess „ `GuardrailDemo“ (` ) im Projekt „ `smart-workflow-demo“ (` ).
