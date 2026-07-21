# Werkzeuge definieren

KI-Agenten in Smart Workflow nutzen Tools, um Aktionen auszuführen. Ein Tool ist eine benannte, aufrufbare Logikeinheit, die der Agent zur Laufzeit erkennt, auswählt und aufruft. Smart Workflow unterstützt zwei Arten von Tools.

---

## Aufrufbare Prozess-Tools

Wir empfehlen nachdrücklich, aufrufbare Unterprozesse als Werkzeuge zu nutzen. Dieser Ansatz passt sich nahtlos an die Arbeitsweise der Ivy-Entwickler an und bietet uneingeschränkten Zugriff auf alle Funktionen des Prozess-Designers – wie beispielsweise Fehlerbehandlung, Dialoge, Aufrufe von Unterprozessen und weitere Axon Ivy-Funktionen.

Sie können jeden aufrufbaren Unterprozess in ein Tool umwandeln, indem Sie einfach das Tag „ `“ des Tools „` “ hinzufügen.

**Schritte:**

1. Erstellen Sie in Ihrem Axon Ivy-Projekt einen aufrufbaren Unterprozess.
2. Fügen Sie dem Prozess das Tag „ `-Tool“` hinzu.
3. Verfassen Sie eine klare Beschreibung unter `` – diese liest der Agent, um zu entscheiden, ob er das Tool aufruft.

![Werkzeugkonfigurationen](./img/tool-configurations.png)

---

## Java-Tools

Für fortgeschrittene Anwendungsfälle kann die Tool-Logik auch direkt in Java implementiert werden. Dies ist jedoch selten erforderlich – nutzen Sie nach Möglichkeit lieber aufrufbare Prozesse. Ziehen Sie Java-Tools nur dann in Betracht, wenn die Logik keine Workflow-Schritte enthält und sich besser als einfache Java-Klasse darstellen lässt.

### Schritt 1 – Implementierung des „ `“ SmartWorkflowTool`

```java
public class MyTool implements SmartWorkflowTool {

  @Override
  public String name() {
    return "myTool"; // Name, unter dem der Agent dieses Tool aufruft
  }

  @Override
  public String description() {
    return "Beschreiben Sie, was dieses Tool tut und wann der Agent es verwenden sollte.";
  }

  @Override
  public List<ToolParameter> parameters() {
    return List.of(
        new ToolParameter("paramName", "Beschreibung dieses Parameters", "java.lang.String")
    );
  }

  @Override
  public Object execute(Map<String, Object> args) {
    String value = (String) args.get("paramName");
    // ... Ihre Logik
    return result;
  }
}
```

Der Typ ist eine Zeichenfolge, die den Java-Typ identifiziert. Folgende Typen werden unterstützt:

| Art         | Beispiel                                                                      |
| ----------- | ----------------------------------------------------------------------------- |
| Primitiv    | `"int"`, `"boolean"`, `"double"`                                              |
| Java-Klasse | `"java.lang.String"`, `"com.example.MyClass"`                                 |
| Liste       | `"java.util.List<java.lang.String>"`, `"java.util.List<com.example.MyClass>"` |

Arrays werden nicht unterstützt – verwenden Sie stattdessen „ `“ und „` “.

Das Framework deserialisiert die JSON-Argumente des Agenten automatisch in den deklarierten Java-Typ.

### Schritt 2 – Erstellen eines „ `“-SmartWorkflowToolsProvider`

Fassen Sie ein oder mehrere Werkzeuge in einer Anbieterklasse zusammen:

```java
public class MyToolProvider implements SmartWorkflowToolsProvider {
  @Override
  public List<SmartWorkflowTool> getTools() {
    return List.of(new MyTool());
  }
}
```

### Schritt 3 – Registrierung über SPI

Erstellen Sie die Datei „ `“ unter „src/META-INF/services/com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider“` “ sowie den Tool-Provider:

```text
com.example.MyToolProvider
```

Das Framework lädt beim Start alle registrierten Anbieter.

---

## Demo: TaxCalculatorTool

[`TaxCalculatorTool`](../smart-workflow-demo/src/com/axonivy/utils/smart/workflow/demo/tool/TaxCalculatorTool.java) zeigt ein vollständiges Java-Tool, das ein strukturiertes „ `“-Rechnungsobjekt (` ) entgegennimmt und Steuerberechnungen für die einzelnen Positionen zurückgibt.

Die wichtigsten Punkte aus der Demo:

- Verwendet einen benutzerdefinierten Typ (`com.axonivy.utils.ai.Invoice`) als Parameter – das Framework deserialisiert diesen automatisch aus dem JSON-Aufruf des Agenten.
- Gibt einen typisierten Ergebnisdatensatz zurück (`TaxCalculationResult`), den das Framework als JSON an den Agenten zurücküberträgt.
- Über SPI in [`DemoToolProvider`](../smart-workflow-demo/src/com/axonivy/utils/smart/workflow/demo/tool/DemoToolProvider.java) registriert.

---

## Standardwerkzeuge

Smart Workflow verfügt über integrierte Tools, die die Mitarbeiter sofort nutzen können.

### webSearch

Durchsucht das Internet nach aktuellen Informationen und liefert eine Liste mit Ergebnissen, die Titel, URLs und Inhaltsausschnitte enthält. Agenten wählen dieses Tool automatisch aus, wenn sie aktuelle oder sachliche Informationen aus dem Internet benötigen.

**Konfigurations** en (festgelegt in `variables.yaml`):

| Variable                             | Zweck                                                                                                                                                                                                     | Standard                          |
| ------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| `AI.Tool.WebSearch.Engine`           | Name der zu verwendenden Suchmaschine. Muss mit dem Namen „ `“ (` ) einer registrierten „ `SmartWebSearchEngine“ (`) übereinstimmen. Ist das Feld leer, wird die erste verfügbare Suchmaschine verwendet. | _(leer – erst bei Verfügbarkeit)_ |
| `AI.Tool.WebSearch.MaxResults`       | Maximale Anzahl der pro Abfrage zurückgegebenen Suchergebnisse                                                                                                                                            | `5`                               |
| `AI.Tool.WebSearch.WhitelistDomains` | Durch Kommas getrennte Liste der zulässigen Domains (z. B. `, stackoverflow.com, github.com,`). Ist das Feld leer, sind alle Domains zulässig.                                                            | _(leer – alle Domänen)_           |

**Suchmaschinen-**: Standardmäßig verwendet das Tool DuckDuckGo. Benutzerdefinierte Suchmaschinen können eingebunden werden, indem [`SmartWebSearchEngine`](../smart-workflow/src/com/axonivy/utils/smart/workflow/tools/web/SmartWebSearchEngine.java) implementiert und ein [`SmartWebSearchEngineProvider`](../smart-workflow/src/com/axonivy/utils/smart/workflow/tools/web/SmartWebSearchEngineProvider.java) über SPI registriert wird.

**Verwendung des Tools in einem Prozess**: Weisen Sie `webSearch` dem Feld „ `-Tools“` eines Agentic-Prozessaufruf-Elements zu:

```
tools = ["webSearch"]
```

Ein vollständiges Beispiel finden Sie im [`WebSearchDemo`](../smart-workflow-demo/processes/Features/WebSearchDemo.p.json).
