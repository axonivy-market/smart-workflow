# Freigabeprozess

In diesem Dokument wird der schrittweise Ablauf zur Erstellung einer neuen Version von Smart Workflow beschrieben.

## Schritte zur Freigabe

### Schritt 1: Abhängigkeiten aktualisieren

Überprüfen Sie, ob [Renovate-Bot-PR](https://github.com/axonivy-market/smart-workflow/pulls) vorhanden ist, und aktualisieren Sie gegebenenfalls die Abhängigkeiten.

### Schritt 2: Kompatibilität mit der neuesten Designer-Version prüfen

1. Laden Sie die neueste Version des Designers auf Ihren lokalen PC herunter
2. Importieren Sie Smart-Workflow-Projekte (einschließlich aller Anbieterprojekte im Ordner „ `models/` “) in die neueste Version von Designer.
3. Beheben Sie etwaige Probleme wie:
   - Kompilierungsfehler
   - Validierungsfehler
4. Alle Projekte auf die neueste Ivy-Version umstellen

### Schritt 3: Kompatibilität des KI-Anbieters prüfen

1. Führen Sie den [E2E-Build](https://github.com/axonivy-market/smart-workflow/actions/workflows/e2e.yml) aus, um alle Integrationstests für KI-Anbieter auszuführen:
   - **Zweig:** `master`
2. Beheben Sie alle Probleme, die während des Testens auftreten

### Schritt 4: Release-Entwurf aktualisieren

1. Führen Sie den [Release Drafter](https://github.com/axonivy-market/smart-workflow/actions/workflows/release-drafter.yml)-Build aus:
   - **Zweig:** `master`
2. Rufen Sie die [Seite „Veröffentlichungen“](https://github.com/axonivy-market/smart-workflow/releases) auf, um die Hinweise zur nächsten Version einzusehen.
3. Überprüfen, aktualisieren und neu erstellen, um qualitativ hochwertige Versionshinweise zu gewährleisten

### Schritt 5: Release-Build erstellen

1. Führen Sie den [Release](https://github.com/axonivy-market/smart-workflow/actions/workflows/release.yml)-Build mit den folgenden Parametern aus:
   - **Zweig:** `master`
   - **Version:** Die kommende Version finden Sie auf der Seite [Veröffentlichungen](https://github.com/axonivy-market/smart-workflow/releases)
   - **Formatbeispiel:** `14.0.0-b1` → Wird auf der Seite „Veröffentlichungen“ wie folgt angezeigt: `14.0.0-beta1`

### Schritt 6: Release-PR zusammenführen

1. Nachdem der Build aus Schritt 5 erfolgreich abgeschlossen wurde, wird ein „Release“-PR erstellt.
2. Überprüfen Sie alle „ `“-Dateien („pom.xml“ und „` “) und setzen Sie die Snapshot-Version zurück:
   - **Beispiel:** `14.0.0-b1-SNAPSHOT` → `14.0.0-SNAPSHOT`
3. Den PR in den Master-` -Zweig von `zusammenführen

### Schritt 7: Versionshinweise veröffentlichen

1. Wechseln Sie zur Seite [Veröffentlichungen](https://github.com/axonivy-market/smart-workflow/releases)
2. Bearbeiten Sie den Eintrag „Nächste Version“ wie folgt:
   - **Tag:** Die angestrebte Release-Version
   - **Titel der Veröffentlichung: „** “ – Die Veröffentlichung im Vollformat
   - **Beispiel:** Für die Version `14.0.0-b1` verwenden Sie den Titel `14.0.0-beta1`
3. Die Pressemitteilung veröffentlichen

## Anmerkungen

- Überprüfen Sie die Versionsnummern stets noch einmal, bevor Sie Releases erstellen.
- Stellen Sie sicher, dass alle Tests erfolgreich abgeschlossen sind, bevor Sie mit der Veröffentlichung fortfahren.
- Die Versionshinweise sollten für die Nutzer klar und umfassend sein
