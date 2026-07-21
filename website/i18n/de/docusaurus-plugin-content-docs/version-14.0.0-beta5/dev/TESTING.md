# 🧪 Testen

Die Funktionen von smart-workflow werden umfassend getestet, um ihre Stabilität zu gewährleisten und eine einfache Weiterentwicklung zu ermöglichen. Alle Testquellen sind unter `smart-workflow-test` zu finden.

## 📂 Testkategorien

### ⚡ Unit-Tests

Kleine, schnell ausgeführte Tests. Keiner von ihnen sollte über das Netzwerk Dienste von Drittanbietern aufrufen oder lange Ausführungszeiten verursachen.

- Einfaches JUnit 5 – reine Java-Logik, keine Ivy-Laufzeitumgebung erforderlich
- `@IvyTest` — Der Code liest Ivy-Variablen aus oder nutzt das SPI-Laden
- `@IvyProcessTest` — Der Code benötigt einen vollständigen Ivy-Prozesskontext
- `@RestResourceTest` — schlanke REST-Ressourcentests; vorzuziehen, wenn externe LLM-Interaktionen simuliert werden, um die Tests isoliert und schnell zu halten. Siehe den Abschnitt [Mocking](#-mocking).

### 🐳 Integrationstests

Tests mit umfangreicher Infrastruktur, die automatisch über Docker/Testcontainers gestartet werden. Verwenden Sie `@Testcontainers` zusammen mit `@IvyTest`, um beispielsweise einen OpenSearch-Knoten zu starten.

- Diese Klassen haben die Endungen „ `“, „ContainerTest“` “ oder „ `“, „IT“` “.
- Nicht Teil des regulären CI-Builds – wird nächtlich über den Workflow [`IT-Build`](../../.github/workflows/it.yml) ausgeführt.

### 🌐 End-to-End-Tests

Tests, die über das Netzwerk echte LLM-Modelle aufrufen. Dazu sind API-Schlüssel erforderlich, die über Systemeigenschaften übergeben werden (z. B. `-DOPEN_AI_API_KEY=…`).

- Die Klassen tragen die Bezeichnungen „ ` “ und „E2E` “ (z. B. „ `“ und „OpenAiModelE2E“`).
- Ist nicht Teil des regulären CI-Builds – wird wöchentlich (sonntags) über den Workflow [`E2E-Build`](../../.github/workflows/e2e.yml) ausgeführt.

Beispiel – Aufruf eines echten OpenAI-Modells und Überprüfung der strukturierten Ausgabe:

```java
@IvyProcessTest
class OpenAiModelE2E {
  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, OpenAiModelProvider.NAME);
    fixture.var(OpenAiConf.API_KEY, TestUtils.getSystemProperty("OPEN_AI_API_KEY"));
  }

  @Test
  void structuredOutput_e2e(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var res = client.start().process(AGENT_TOOLS.elementName("structuredOutput"))
        .as().session(Ivy.session()).execute();
    assertThat(res.data().last().getPerson().getFirstName()).isEqualTo("James");
  }
}
```

---

## 🎭 Spott

CI-Unit-Tests müssen schnell und unabhängig voneinander ablaufen, doch der Code interagiert naturgemäß mit langsamen Remote-LLMs. Anstatt echte Dienste aufzurufen, werden statische Antworten, die aus echten Modellinteraktionen erfasst wurden, wiedergegeben. Befolgen Sie diese drei Schritte, um einen solchen Test zu schreiben:

**1. Erstellen Sie einen Demo- oder Testprozess, der einen Agenten bzw. ein LLM aufruft.**

Der Prozess sollte einen Agenten genau so aufrufen, wie Sie es testen möchten. Definieren Sie „Nachrichten“, „Werkzeuge“, „Ausgabe“ usw. nach Bedarf.

**2. Zeichnen Sie die tatsächliche REST-Antwort auf.**

Aktivieren Sie das Laufzeitprotokoll auf Filterebene `Trace`, führen Sie den Prozess für ein Live-Modell aus und kopieren Sie anschließend den protokollierten Antworttext in eine JSON-Datei neben Ihrer Testklasse (z. B. `response1.json`, `response2.json`):

```json
{
  "id": "tool-demo-1",
  "model": "gpt-4o-mini",
  "choices": [{
    "message": {
      "role": "assistant",
      "content": null,
      "tool_calls": [{ "id": "call_tax001", "type": "function",
        "function": { "name": "calculateTax", "arguments": "{\"invoiceNumber\":\"INV-2024-042\", ...}" }
      }]
    },
    "finish_reason": "tool_calls"
  }],
  "usage": { "prompt_tokens": 500, "completion_tokens": 100, "total_tokens": 600 }
}
```

**3. Schreiben Sie einen `@RestResourceTest`, der den Prozess steuert und LLM-Antworten simuliert.**

Der Test startet den eigentlichen Ivy-Prozess über `BpmClient` und führt eine Überprüfung der Ausgabedaten durch. `MockOpenAI.defineChat(handler)` fängt jeden ausgehenden LLM-HTTP-Aufruf ab, bevor dieser das Netzwerk erreicht, und gibt stattdessen die vorab aufgezeichnete Datei zurück – der zu testende Prozess bemerkt diesen Austausch nicht. Bei einem mehrrundigen Gespräch werden je nach Fortschritt des Gesprächs unterschiedliche Dateien zurückgegeben:

```java
@RestResourceTest
class TestToolDemo {

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool-demo"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(request -&gt; respond(request, responder));
  }

  private Response respond(JsonNode request, ResourceResponder responder) {
    var messages = (ArrayNode) request.get("messages");
    return messages.size() &lt;= 2
        ? responder.send("response1.json")  // erster Durchgang: Tool-Aufruf-Anfrage
        : responder.send("response2.json"); // zweiter Durchgang: endgültige Antwort
  }

  @Test
  void calculateInvoiceTax(BpmClient client) {
    var result = client.start().process(TOOL_DEMO.elementName("start")).execute();
    assertThat(result.data().last().getTaxedInvoice()).isNotNull();
  }
}
```

### 📋 Log-Ausgabe anzeigen

Verwenden Sie „ `“ (LoggerAccess` ) als JUnit-5-Erweiterung, um während eines Tests ausgegebene Protokollmeldungen zu erfassen und zu überprüfen:

```java
@RegisterExtension
LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

@Test
void httpCallIsLogged(BpmClient client) {
  // ... Aktion auslösen
  assertThat(log.infos()).anyMatch(line -&gt; line.contains("POST /v1/chat/completions"));
}
```
