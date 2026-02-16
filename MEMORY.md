# Auto Memory - Robot Warehouse Gripper Project

## ⚠️ VOR jedem Read/Grep/Glob/Task:
**Ist Pfad in .gitignore? → NIEMALS lesen**

## 📋 ERSTE AKTION JEDER SESSION
**IMMER zu Beginn Project_Instructions.md lesen!**
- Enthält vollständiges DSGVO-Protokoll
- Task-Tool Sicherheitsregeln
- Git-Workflow Regeln
- Ausgeschlossene Verzeichnisse

## Project Context (Schnellreferenz)

### Architecture
- Spring Boot 3.2.1 REST API → .NET WCF SOAP service
- Circuit breaker: Resilience4j (already implemented)
- WSDL integration: active, generated code in target/generated-sources/jaxws/

### Current Phase
End-phase development, preparing for pilot deployment at customer site in 2-3 weeks

### Known Issues & Fixes
- JAXBElement unwrapping: SOAP Fault error messages must use `unwrapJAXBElement()` helper
  - Fixed in: WcfGripperServiceClient.java (all fault handlers)
