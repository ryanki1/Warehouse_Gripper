# Quick Start - robot WCF Integration

## 🚀 In 5 Minuten loslegen

### Terminal 1: SQL Server starten

```bash
cd robotGripperBackend.Skeleton
docker-compose up -d
```

### Terminal 2: .NET Backend mit WCF starten

```bash
cd robotGripperBackend.Skeleton

# WICHTIG: ProgramWithWcf.cs verwenden
mv Program.cs Program.old.cs
mv ProgramWithWcf.cs Program.cs

# Starten
dotnet restore
dotnet run
```

**Warten bis erscheint:**
```
WCF SOAP Service:    http://localhost:8080/WarehouseGripperService.svc
WCF WSDL:            http://localhost:8080/WarehouseGripperService.svc?wsdl
```

### Terminal 3: Java Client generieren & starten

```bash
cd JavaSpringBootClient

# WSDL-Stubs generieren (einmalig)
mvn clean compile

# Java Code aktivieren (siehe unten)

# Starten
mvn spring-boot:run
```

---

## 📝 Java Code aktivieren (einmalig)

Öffne: `JavaSpringBootClient/src/main/java/com/robot/warehouse/service/WcfGripperServiceClient.java`

**1. Imports aktivieren** (Zeile ~6):
```java
// VOR:
// import jakarta.xml.ws.BindingProvider;

// NACH:
import jakarta.xml.ws.BindingProvider;
import javax.xml.namespace.QName;
import java.net.URL;
import java.util.stream.Collectors;
```

**2. `getServicePort()` aktivieren** (Zeile ~32):
```java
// Block kommentieren einschalten:
private IWarehouseGripperService getServicePort() {
    // ... Code aktivieren
}
```

**3. MOCK-Code durch WCF-Calls ersetzen**:

In **jeder Methode** den `/* GENERATED CODE */` Block aktivieren und MOCK-Code entfernen.

Beispiel - `getGripperStatus()`:
```java
// VOR:
public GripperStatusResponse getGripperStatus(int gripperId) {
    // TEMPORARY MOCK DATA
    return GripperStatusResponse.builder()
            .gripperId(gripperId)
            .build();
}

// NACH:
public GripperStatusResponse getGripperStatus(int gripperId) {
    try {
        IWarehouseGripperService port = getServicePort();
        GripperStatusDto wcfResult = port.getGripperStatus(gripperId);
        return mapToGripperStatusResponse(wcfResult);
    } catch (ServiceFault_Exception e) {
        throw new RuntimeException("Failed: " + e.getFaultInfo().getErrorMessage());
    }
}
```

**4. Mapping-Methoden aktivieren** (Zeile ~260):
```java
// Am Ende der Datei die 3 Mapping-Methoden aktivieren:
private GripperStatusResponse mapToGripperStatusResponse(GripperStatusDto dto) { ... }
private OperationResponse mapToOperationResponse(OperationResultDto dto) { ... }
private LocationResponse mapToLocationResponse(LocationDto dto) { ... }
```

---

## ✅ Testen

### Test 1: WSDL verfügbar?
```bash
curl http://localhost:8080/WarehouseGripperService.svc?wsdl
```
✅ Sollte XML zurückgeben

### Test 2: Java API Health Check
```bash
curl http://localhost:8081/api/warehouse/health
```
✅ Sollte `true` zurückgeben

### Test 3: Greifer Status (Java → WCF → .NET)
```bash
curl http://localhost:8081/api/warehouse/grippers/1
```
✅ Sollte JSON mit Gripper-Daten zurückgeben

### Test 4: Greifer bewegen
```bash
curl -X POST "http://localhost:8081/api/warehouse/grippers/1/move?x=750&y=400&z=0"
```
✅ Sollte `{"success": true}` zurückgeben

---

## 🌐 Alle Services Übersicht

| Service | Port | URL |
|---------|------|-----|
| **Java REST API** | 8081 | http://localhost:8081/api/warehouse |
| **Swagger UI** | 8081 | http://localhost:8081/swagger-ui.html |
| **WCF SOAP** | 8080 | http://localhost:8080/WarehouseGripperService.svc |
| **WSDL** | 8080 | http://localhost:8080/WarehouseGripperService.svc?wsdl |
| **Nancy REST** | 5000 | http://localhost:5000/api |
| **SignalR** | 5000 | http://localhost:5000/hubs/gripper |

---

## 🔧 Häufige Befehle

### .NET Backend neustarten
```bash
cd robotGripperBackend.Skeleton
dotnet run
```

### Java Client neustarten
```bash
cd JavaSpringBootClient
mvn spring-boot:run
```

### WSDL neu generieren (nach WCF-Änderungen)
```bash
cd JavaSpringBootClient
mvn clean compile
# Dann Java-Code neu aktivieren!
```

### Alle Logs ansehen
```bash
# .NET Logs
tail -f robotGripperBackend.Skeleton/logs/robot-*.log

# Docker SQL Server Logs
docker logs robot-sqlserver

# Java Logs (im Terminal wo mvn läuft)
```

### Datenbank zurücksetzen
```bash
docker-compose down -v
docker-compose up -d
# .NET Backend neu starten (erstellt DB)
```

---

## 📦 Postman Collection

1. Importieren: `robotGripperBackend.Skeleton/Postman_Collection.json`
2. Sammlung "robot Warehouse Gripper - WCF & REST API" öffnen
3. Requests ausführen:
   - **Java REST API** (8081)
   - **.NET Nancy API** (5000)
   - **WCF SOAP** (8080)

---

## ❌ Troubleshooting

### Port 8080 belegt
```bash
# Freien Port finden
lsof -i :8080

# In appsettings.json ändern:
"WcfService": { "BaseAddress": "http://localhost:8081" }
```

### Java kann WSDL nicht laden
```bash
# WCF Service läuft?
curl http://localhost:8080/WarehouseGripperService.svc?wsdl

# Maven cache löschen
mvn clean
rm -rf target/
mvn compile
```

### "Connection refused" in Java
- WCF Service auf Port 8080 läuft?
- application.properties korrekt?
- Firewall blockiert?

### SOAP Fault: GRIPPER_NOT_FOUND
- Nur Gripper ID 1 und 2 existieren (Seed-Daten)
- Verwenden Sie 1 oder 2

---

## 📚 Weitere Dokumentation

- [WCF_INTEGRATION_README.md](WCF_INTEGRATION_README.md) - Vollständige Integration-Guide
- [README.md](robotGripperBackend.Skeleton/README.md) - Backend-Dokumentation
- [SETUP.md](robotGripperBackend.Skeleton/SETUP.md) - Detailliertes Setup

---

**Happy Coding!** 🎉
