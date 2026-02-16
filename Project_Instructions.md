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

### 1a. ⚠️ PROAKTIVE WARNUNG - User-Prompts mit sensiblen Daten

**WICHTIG**: Wenn der User in seinem Prompt sensible Daten erwähnt, SOFORT warnen:

```
⚠️ DSGVO-WARNUNG ⚠️

Ihr Prompt enthält potenziell DSGVO-sensitive Information:
- [KUNDENNAME / IP-ADRESSE / ETC.]

Diese Information sollte NICHT in:
- Dokumentation
- Code-Beispielen
- Commit-Messages
- oder anderen committed files

verwendet werden.

Soll ich stattdessen generische Begriffe verwenden?
(z.B. "der Kunde", "customer site", "customer IT team")
```

**Dann**: Erst auf Bestätigung warten, bevor sensible Daten verwendet werden.

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

## TASK-TOOL & SUB-AGENTS - KRITISCHES DSGVO-RISIKO

⚠️ **HÖCHSTE PRIORITÄT: Sub-Agents befolgen .gitignore NICHT automatisch!**

### Das Problem
- Task-Tool mit Sub-Agents (Explore, Plan, etc.) hat **KEINEN** automatischen DSGVO-Filter
- .gitignore Patterns werden **NICHT** automatisch respektiert
- Sub-Agents können **ALLE** Dateien lesen, auch DSGVO-sensible
- Dies führte bereits zu einem DSGVO-Verstoß (application-dev.properties wurde gelesen)

### Die Regel - NIEMALS Task-Tool ohne explizite Ausschlüsse!

**BEI JEDEM Task-Aufruf MUSS Claude:**

1. **Explizit ALLE ausgeschlossenen Bereiche im Prompt angeben:**
   ```
   CRITICAL DSGVO EXCLUSIONS - DO NOT READ THESE FILES:
   - ❌ application-*.properties (all files matching this pattern)
   - ❌ target/ directory and all subdirectories
   - ❌ build/ directory and all subdirectories
   - ❌ robotGripperBackend.Skeleton/ directory and all subdirectories
   - ❌ All files matching .gitignore patterns
   ```

2. **Explizit angeben, welche Dateien gelesen werden dürfen:**
   ```
   ONLY READ THESE SAFE FILES:
   - ✅ application.properties (WITHOUT dash/hyphen - the base config file)
   - ✅ Test files: *Test.java
   - ✅ Source files: *.java (in src/ directories only)
   ```

3. **Im Zweifel: KEIN Task-Tool verwenden**
   - Lieber direkte Tool-Aufrufe (Read, Grep, Glob)
   - Diese können gezielter gesteuert werden

### Beispiel - FALSCH ❌
```
Task: Find Circuit Breaker configuration
```
→ Sub-Agent liest ALLE application*.properties Dateien!

### Beispiel - RICHTIG ✅
```
Task: Find Circuit Breaker configuration

CRITICAL DSGVO EXCLUSIONS - DO NOT READ:
- ❌ application-*.properties
- ❌ target/, build/, robotGripperBackend.Skeleton/

ONLY READ:
- ✅ application.properties (base config only)
```
→ Sub-Agent liest nur die sichere Datei!

### Konsequenzen bei Verstößen
- DSGVO-sensible Daten werden an Anthropic Server übertragen
- Daten bleiben 30 Tage (oder kürzer bei Enterprise) gespeichert
- Mögliche Credentials-Rotation erforderlich
- DSGVO-Dokumentationspflicht

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