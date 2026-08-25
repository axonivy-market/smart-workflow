# Intelligenter Workflow RAG

Retrieval-Augmented Generation (RAG) verbessert KI-Antworten im Axon Ivy Smart Workflow, indem sie auf Ihren eigenen Dokumenten und Wissensdatenbanken verankert werden. Anstatt sich ausschließlich auf die Trainingsdaten des LLMs zu verlassen, ruft RAG relevante Inhalte aus einem Vektorspeicher ab und bindet diese als Kontext ein – und liefert so Antworten, die präzise, überprüfbar und spezifisch für Ihr Unternehmen sind.

Der Arbeitsablauf ist ganz einfach:

1. ** einlesen **— Teilen Sie Ihre Dokumente in Blöcke auf, generieren Sie Einbettungen und speichern Sie diese in einem Vektorspeicher.
2. ** durchsuchen** — Wenn eine Frage eingeht, wird die Suchanfrage eingebettet, die ähnlichsten Chunks werden ermittelt und zurückgegeben.
3. **Antwort** — Das LLM erhält die abgerufenen Chunks als Kontext und generiert eine kontextbezogene Antwort.

Smart Workflow stellt aufrufbare Teilprozesse und KI-Tools bereit, die die Schritte 1 und 2 übernehmen. Schritt 3 wird vom Element „ `“ („AgenticProcessCall“` ) übernommen, das die LLM- und Tool-Aufrufe automatisch koordiniert.

## OpenSearch

[OpenSearch](https://opensearch.org/) ist eine skalierbare Open-Source-Such- und Analyse-Engine, die die k-NN-Vektorsuche unterstützt – und sich somit ideal für RAG-Anwendungen eignet.

Das Modul „ `smart-workflow-opensearch-rag“` bietet einen aufrufbaren Unterprozess für die Einrichtung sowie ein KI-Tool, das ein Agent zur Laufzeit aufrufen kann.

#### Aufrufbar: `createVectorStore`

Verwenden Sie diesen aufrufbaren Unterprozess, um einen OpenSearch-Index zu erstellen und Dokumente einzulesen, bevor der Agent ausgeführt wird.

**Eingabeparameter**

| Parameter  | Typ                   | Beschreibung                                               |
| ---------- | --------------------- | ---------------------------------------------------------- |
| `Sammlung` | Zeichenkette          | Name des Index, in den die Daten eingelesen werden sollen. |
| `Quellen`  | Liste\<Zeichenkette\> | Zu indizierende Dokumente im Klartext.                     |

**Ergebnis**

| Parameter  | Beschreibung                                                                                                                                      |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Ergebnis` | Ergebnis der Einlesung. `answer` enthält die Anzahl der indizierten Segmente; `error` enthält Details zum Fehler, falls etwas schiefgelaufen ist. |

#### Tool: `openSearchSearch`

Den Smart-Workflow-Agenten steht ein semantisches Suchtool zur Verfügung. Der Agent ruft dieses Tool automatisch auf, wenn er relevante Inhalte aus einer Wissensdatenbank abrufen muss.

**Eingabeparameter**

| Parameter    | Typ          | Beschreibung                                                                                                 |
| ------------ | ------------ | ------------------------------------------------------------------------------------------------------------ |
| `Sammlung`   | Zeichenkette | Name des zu abfragenden Index.                                                                               |
| `Abfrage`    | Zeichenkette | Die Suchanfrage zum Auffinden relevanter Inhalte.                                                            |
| `maxResults` | Ganzzahl     | Maximale Anzahl der zurückzugebenden Segmente. Ist der Wert null, wird „ `“ (AI.RAG.MaxResults` ) verwendet. |
| `minScore`   | Doppelt      | Mindestähnlichkeitswert (0,0–1,0). Ist dieser Wert nicht angegeben, wird „ `“ (AI.RAG.MinScore` ) verwendet. |

**Ergebnis**

| Parameter  | Beschreibung                                                                                 |
| ---------- | -------------------------------------------------------------------------------------------- |
| `Ergebnis` | Suchergebnisse, die übereinstimmende Inhaltssegmente mit ihren Ähnlichkeitswerten enthalten. |

### Demo

Der Prozess „ `“ (RagChatBotDemo` ) unter `smart-workflow-demo` ist ein interaktiver vierstufiger Assistent, der eine vollständige RAG-Pipeline veranschaulicht:

1. **Konfigurations** en — Überprüfen Sie die URL des OpenSearch-Servers, die Authentifizierungsart und die Einstellungen für das Einbettungsmodell, die aus den Ivy-Variablen geladen wurden. Testen Sie die Verbindung, bevor Sie fortfahren.
2. **Hochladen und Einbetten von „** “ — Geben Sie einen Indexnamen ein, laden Sie Dateien im Format „ `.txt“, „` “ oder „ `.md“ sowie „` “ hoch und betten Sie die Dokumente als durchsuchbare Vektor-Chunks in OpenSearch ein.
3. **Ergebnisse** — Alle indizierten Chunks mit ihrer Quelldatei und einer Inhaltsvorschau anzeigen.
4. **Chat** — Stellen Sie Fragen, die von einem KI-Agenten beantwortet werden, der mithilfe des Tools „ `“ (openSearchSearch` ) kontextbezogene Informationen aus den indizierten Dokumenten abruft.

**Voraussetzungen:**

```properties
AI.DefaultProvider           = OpenAI          # oder AzureOpenAI / Gemini
AI.RAG.OpenSearch.Url        = https://my-opensearch.us-east-1.es.amazonaws.com
# AI.RAG.EmbeddingModel.Provider kann leer gelassen werden, wenn AI.DefaultProvider Embedding unterstützt
```

> **Tipp:** Unser [Devcontainer](dev/DEVCONTAINER.md) ist bereits mit einem OpenSearch-Dienst vorkonfiguriert, sodass Sie die Server-Einrichtung überspringen und die Konfiguration unter `AI.RAG.OpenSearch.Url` vornehmen können. In dieser Umgebung müssen Sie lediglich den API-Schlüssel des AI-Anbieters angeben.
