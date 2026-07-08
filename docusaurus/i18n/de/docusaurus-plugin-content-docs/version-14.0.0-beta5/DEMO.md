# Demo-Projekte

Die Demo-Projekte veranschaulichen praktische Anwendungsfälle, die auf Smart Workflow aufbauen. Jede Demo befindet sich im Ordner „ `demo/“ unter` und besteht aus zwei Modulen:

| Modul                             | Zweck                                                |
| --------------------------------- | ---------------------------------------------------- |
| `smart-workflow-<name>-demo`      | Die Demo-Anwendung (Prozesse, Dienste, Datenklassen) |
| `smart-workflow-<Name>-Demo-Test` | Tests für die Demo-Anwendung                         |

Verwenden Sie die „ **“-Demo zur Lieferanten-Onboarding-** als Referenzimplementierung: [`smart-workflow-supplier-demo`](../../demo/smart-workflow-supplier-demo) und [`smart-workflow-supplier-demo-test`](../../demo/smart-workflow-supplier-demo-test).

---

## Eine neue Demo erstellen

1. **Erstellen Sie das Demomodul „** “ unter `demo/`, z. B. `demo/smart-workflow-procurement-demo`. Registrieren Sie es im Stammverzeichnis `pom.xml` ` im Abschnitt <modules>`, damit es von der Haupt-CI-Pipeline erstellt wird.
2. **Erstellen Sie das Testmodul „** “, z. B. `demo/smart-workflow-procurement-demo-test`, und orientieren Sie sich dabei an der Struktur von `smart-workflow-supplier-demo-test`.
3. **Registrieren Sie das Testmodul „** “ im Profil „ `“ unter „demo.test“` in der Stammdatei „ `“ unter „pom.xml“` (fügen Sie es **nicht** zur Standardliste unter „ `<modules>“` hinzu):

```xml
<profiles>
  <profile>
    <id>demo.test</id>
    <modules>
      <module>demo/smart-workflow-supplier-demo-test</module>
      <module>demo/smart-workflow-procurement-demo-test</module> <!-- neu -->
    </modules>
  </profile>
</profiles>
```

1. **Fügen Sie das Testmodul zur Pipeline `demo-test.yml`** hinzu, indem Sie es an das Argument „ `-pl` “ in [`.github/workflows/demo-test.yml`](../../.github/workflows/demo-test.yml) anhängen:

```yaml
mvnArgs: -pl demo/smart-workflow-supplier-demo-test,demo/smart-workflow-procurement-demo-test -am -P demo.test
```

Dadurch bleibt die Pipeline schnell, da nur Demo-Testmodule und deren Abhängigkeiten erstellt werden und nicht relevante CI-Tests übersprungen werden.

---

## Demo-Tests ausführen

Demo-Tests sind nicht Teil des regulären CI-Builds. Führen Sie sie bei Bedarf aus:

- **GitHub Actions:** Starten Sie den Workflow [`Demo-Test-Build`](../../.github/workflows/demo-test.yml) manuell über die Registerkarte „Actions“.
- **Lokal:** `mvn clean verify -P demo.test`
