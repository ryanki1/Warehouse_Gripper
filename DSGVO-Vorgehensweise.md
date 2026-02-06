# DSGVO-konforme Vorgehensweise: CircuitBreaker Projekt Review

## Kontext
**Problem:** CircuitBreaker öffnet sich wie erwartet (numberPermittedCall = 5, Threshold = 100%), aber es gibt noch Bugs zu lösen.

**Anforderung:** Claude soll das Projekt reviewen, aber DSGVO-konform ohne sensible Daten zu konsumieren.

---

## Die Diskussion: Entwicklung der Vorgehensweise

### Initiale Idee (Kebten)
> Was halten Sie für diese Idee: Sie dürfen von Visual Studio Code aus das Projekt reviewen um das Problem mit dem CircuitBreaker zu lösen - wie gesagt im Moment öffnet das Circuit-Breaker wie erwartet bzw mit numberPermittedCall = 5 und Threshhold=100 (%). Der Haken der Sache ist Sie dürfen keine DSVGO relevant Datei - shared secrets, application.properties - konsumieren bzw keine DSVGO relevant Infos - IP Adresse, Eigentum der Kunde - konsumieren.

### Claude's Reaktion
**Meinung:** Ausgezeichnete Idee! ✅

**Vorteile:**
1. Systematisch: Gesamter Code-Flow verstehbar
2. DSGVO-konform: Sensible Daten ausschließbar
3. Effizient: Komplettes Setup sichtbar
4. Nachhaltig: System-Verständnis verbessert

**Klarstellung:** Claude hat keinen direkten Zugriff auf Dateisystem - nur was im Chat gezeigt wird.

---

## Klärung: Claude Projects

### Kebten's Präzisierung
> Ich kann Ihnen mittels den Code Tab Ihnen volle Zugriff auf dem Projekt mit dem Circuit-Breaker geben

**Bedeutung:** Über Claude Projects "Code" Tab kann Zugriff auf das gesamte Projektverzeichnis gegeben werden.

### DSGVO-Bedingungen (vor Zugriff)
Folgendes muss entfernt/redaktiert werden:
- ❌ application.properties → URLs, IPs, Credentials
- ❌ Alle .env Dateien
- ❌ Secrets, API Keys
- ❌ Kundennamen in Kommentaren/Tests
- ❌ Interne System-IPs

---

## Der entwickelte Workflow

### Phase 1: Initialer Dateinamen-Scan

**Kebten's Vorschlag:**
> Wäre folgende Vorgehensweise möglich - Sie macht eine initiale Scan, welcher nur die Name der Dateien berücksichtigt und gibt mir Feedback welche Dateien könnte empfindliche DSVGO Information drin haben

**Claude's Antwort:** Ja! ✅

**Ablauf:**
1. Zugriff auf Projekt erhalten
2. Nur Verzeichnisstruktur scannen (keine Inhalte)
3. Warnung-Liste erstellen:
   ```
   🔴 HOCH RISIKO:
   - application*.properties
   - application*.yml (außer test configs)
   - .env, .env.*
   - *secret*, *credential*, *password*
   
   🟡 MEDIUM RISIKO:
   - pom.xml (interne Repo-URLs möglich)
   - docker-compose.yml (IPs/Ports möglich)
   
   🟢 SICHER:
   - *.java Dateien
   - *Test.java
   - application-test.yml
   ```
4. Kebten überprüft/redaktiert verdächtige Dateien

### Phase 2: Code Review mit Stop-Mechanismus

**Kebten's Anforderung:**
> Ab diesem Punkt, nachdem die Dateien von mir überprüft/redaktiert sind wäre folgende Vorgehensweise gewünscht: Sie schauen zwar den Code-Inhalt bei der Feature Entwicklung/ bei dem Bug-Fixen wie normal an, aber falls sie empfindliche DSVGO Information sehen Sie sagen unterbrechen das nachschauen Prozess ab und geben mir die Name der Datei Bescheid, sodass ich sie wieder überprüfen/redaktieren kann.

**Workflow:**
1. Code normal reviewen für CircuitBreaker-Problem
2. **SOFORT STOPPEN** wenn sensible Daten gesehen werden
3. Dateiname + Zeilennummer nennen
4. Warten bis redaktiert
5. Fortfahren

---

## DSGVO-Audit Bedenken & Antworten

### Claude's Risiken
1. ⚠️ Daten wurden bereits gesehen (auch wenn kurz)
2. ⚠️ Anthropic speichert Konversationen
3. ⚠️ Keine Garantie ALLE sensiblen Daten erkannt werden
4. ⚠️ Kein Ersatz für professionelle DSGVO-Beratung

### Kebten's Gegenargumente
1. ✅ **30-Tage Löschung:** Anthropic löscht nach 30 Tagen
2. ✅ **Team/Enterprise:** Opt-out vom Training ist Standard
3. ✅ **Learning by doing:** Pragmatisch und realistisch
4. ✅ **Firma regelt DSGVO:** Typisches Vorgehen, oft erst nach Audit

**Fazit:** Solider, durchdachter Ansatz! ✅

---

## Problem: Context Window Drift

### Kebten's Sorge
> Kann es sein dass diese Anweisungen über Zeit langsam vergessen werden bzw weniger eine Rolle spielt - wenn Ja wie kann wir das verhindern, weil "der nächste Entwicklungs-Schritt" könnte theoretisch immer ein DSVGO-Audit-Fall triggern

**Antwort:** JA, bei sehr langen Konversationen können frühe Anweisungen weniger Gewicht bekommen.

### Lösung: Project Instructions

**Beste Lösung:** Permanente Anweisungen im Claude Project setzen!

Diese werden bei **jedem neuen Message** einbezogen.

---

## Finale Project Instructions

```markdown
# DSGVO-COMPLIANCE PROTOKOLL

## KRITISCHE REGEL - IMMER BEFOLGEN
Bei JEDEM Code-Review, jeder Feature-Entwicklung und jedem Bug-Fix:

### 1. SOFORTIGER STOPP bei sensiblen Daten
Wenn Sie folgendes sehen, UNTERBRECHEN Sie SOFORT:
- URLs, IP-Adressen, Hostnamen
- Passwörter, API Keys, Tokens, Secrets
- Kundennamen, Firmennamen (außer generische Beispiele)
- Interne System-Informationen
- Datenbank-Connection-Strings
- E-Mail-Adressen (außer Beispiele wie test@example.com)
- Zertifikate, Private Keys

### 2. STOPP-Protokoll
```
⚠️ DSGVO-STOPP ⚠️

Datei: [DATEINAME]
Zeile: [ZEILENNUMMER]
Typ: [URL/IP/Credential/etc]

Bitte Datei redaktieren, dann gebe ich Bescheid wenn fertig.
```

### 3. Nach Redaktion fortfahren
Erst wenn User bestätigt "redaktiert" → Weiter reviewen

### 4. Nie sensible Daten verwenden
NIEMALS sensible Daten in:
- Code-Beispielen
- Erklärungen
- Dokumentation
- Commit-Messages

## ERLAUBTE BEISPIELE
✅ localhost, 127.0.0.1, example.com
✅ user@example.com, test@test.com
✅ "your-api-key-here", "YOUR_SECRET"
✅ Generische Namen: "CustomerService", "Database"

## PROJEKT-KONTEXT
- Spring Boot 3.2.1 Projekt
- Resilience4j Circuit Breaker
- WCF Client Integration
- JUnit Integration Tests
- DSGVO-konforme Entwicklung (Deutschland)
```

---

## Implementierungsplan

### Kebten's Verantwortung
- ✅ Sensible Dateien identifizieren und redaktieren
- ✅ Project Instructions im Projekt Code Bereich uploaden

### Claude's Verantwortung
1. Dateinamen-Scan durchführen
2. Warnung-Liste erstellen
3. Nach Redaktion: CircuitBreaker-Problem lösen
4. Bei sensiblen Daten: SOFORT STOPPEN

---

## Status
**Bereit für Start!** 🚀

Sobald Kebten fertig ist mit:
1. Sensible Dateien redaktieren
2. Project Instructions hochladen
3. Zugriff gewähren

→ Dann startet der Review-Prozess.

---

## Lessons Learned

### Was funktioniert
✅ Schrittweise Workflow-Entwicklung
✅ Klare DSGVO-Bedingungen
✅ Technische UND organisatorische Maßnahmen
✅ Project Instructions für Persistenz
✅ Pragmatischer Ansatz (nicht perfekt, aber gut genug)

### Wichtige Erkenntnisse
- Claude kann Dateien nicht selbst löschen
- Aber: Claude kann bewusst ignorieren und warnen
- Team/Enterprise Plan wichtig für Training Opt-out
- Project Instructions verhindern "Vergessen"
- Learning by doing ist akzeptabler Ansatz

---

*Dokument erstellt: 2026-02-05*
*Zweck: Dokumentation DSGVO-konformer Workflow für Code-Review mit Claude*
