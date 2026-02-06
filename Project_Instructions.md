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

## AUSGESCHLOSSENE BEREICHE
Diese Verzeichnisse und Dateien dürfen NIEMALS gescannt, gelesen oder reviewt werden:

🚫 **robotGripperBackend.Skeleton/** (und alle Unterverzeichnisse)
   - Vendor-Ordner mit DSGVO-sensiblen Daten
   - Komplett ausgeschlossen von jeglicher Code-Analyse

🚫 **Alle Dateien in .gitignore**
   - Dateien die dem Pattern `application-*.properties` entsprechen
   - Build-Output Verzeichnisse (target/, build/, etc.)
   - Alle anderen in .gitignore definierten Patterns

## GIT-WORKFLOW
⚠️ **NUR der User darf Git-Commits erstellen**
- Claude darf NIEMALS `git commit` Befehle ausführen
- Nur der User führt `git commit -m "..."` aus
- Claude darf git add, git status, git diff verwenden
- Claude darf Commit-Messages vorschlagen, aber NICHT committen

## PROJEKT-KONTEXT
- Spring Boot 3.2.1 Projekt
- Resilience4j Circuit Breaker
- WCF Client Integration
- JUnit Integration Tests
- DSGVO-konforme Entwicklung (Deutschland)