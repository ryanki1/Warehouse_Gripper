# robot Warehouse Gripper - WCF Integration Guide

## Architektur-Übersicht

```
┌──────────────────────┐
│   Java Spring Boot   │
│   REST API (8081)    │
└──────────┬───────────┘
           │ JAX-WS Client
           │ SOAP/HTTP
           ↓
┌──────────────────────┐
│  .NET WCF Service    │
│  SOAP (8080)         │
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│  Nancy REST API      │
│  + Backend Services  │
│  (Port 5000)         │
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│  SQL Server          │
│  + CAN-Bus           │
│  + OPC-UA            │
└──────────────────────┘
```

## Komponenten

### 1. .NET WCF Service (Port 8080)
- **Zweck**: SOAP-Schnittstelle für Java Integration
- **Technologie**: CoreWCF (läuft auf .NET 8)
- **Binding**: BasicHttpBinding (SOAP 1.1)
- **WSDL**: `http://localhost:8080/WarehouseGripperService.svc?wsdl`

### 2. Java Spring Boot Client (Port 8081)
- **Zweck**: REST API Wrapper für WCF Service
- **Technologie**: Spring Boot 3.2, JAX-WS
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`

### 3. Nancy REST API (Port 5000) - BESTEHT WEITER
- **Zweck**: Direkte REST-Schnittstelle (optional)
- **Technologie**: Nancy Framework
- **Verwendung**: Kann parallel zum WCF genutzt werden

---

## Installation & Setup

### Schritt 1: .NET Backend mit WCF Service starten

```bash
cd robotGripperBackend.Skeleton

# SQL Server mit Docker starten
docker-compose up -d

# Projekt bauen
dotnet restore
dotnet build

# Mit WCF Service starten (verwende ProgramWithWcf.cs)
dotnet run --project robotGripperBackend.csproj
```

**WICHTIG**: Ersetzen Sie [Program.cs](robotGripperBackend.Skeleton/Program.cs) durch [ProgramWithWcf.cs](robotGripperBackend.Skeleton/ProgramWithWcf.cs) oder benennen Sie um:

```bash
mv Program.cs Program.old.cs
mv ProgramWithWcf.cs Program.cs
dotnet run
```

Ausgabe sollte sein:
```
=================================================================
robot Gripper Backend with WCF Integration started
=================================================================
Nancy REST API:      http://localhost:5000/api
SignalR Hub:         http://localhost:5000/hubs/gripper
WCF SOAP Service:    http://localhost:8080/WarehouseGripperService.svc
WCF WSDL:            http://localhost:8080/WarehouseGripperService.svc?wsdl
=================================================================
```

**WSDL testen:**
```bash
curl http://localhost:8080/WarehouseGripperService.svc?wsdl
```

### Schritt 2: WSDL-Stubs für Java generieren

```bash
cd JavaSpringBootClient

# WSDL generieren (WCF Service muss laufen!)
mvn clean compile

# Dies generiert Java-Klassen in:
# target/generated-sources/jaxws/com/robot/warehouse/wcf/generated/
```

**Generierte Klassen:**
- `IWarehouseGripperService.java` - Service Interface
- `WarehouseGripperServiceService.java` - Service Factory
- `GripperStatusDto.java`, `OperationResultDto.java`, etc. - DTOs

### Schritt 3: Java Code aktivieren

Öffnen Sie [WcfGripperServiceClient.java](JavaSpringBootClient/src/main/java/com/robot/warehouse/service/WcfGripperServiceClient.java) und:

1. **Kommentieren Sie die Imports ein:**
```java
import jakarta.xml.ws.BindingProvider;
import javax.xml.namespace.QName;
import java.net.URL;
import java.util.stream.Collectors;
```

2. **Kommentieren Sie die `getServicePort()` Methode ein** (Zeile ~30)

3. **Ersetzen Sie MOCK-Daten durch echte WCF-Calls**:
   - In jeder Methode die `/* GENERATED CODE */` Blöcke aktivieren
   - MOCK-Code entfernen

### Schritt 4: Java Spring Boot starten

```bash
cd JavaSpringBootClient

# Build
mvn clean package

# Run
mvn spring-boot:run
```

Ausgabe:
```
========================================================
robot Warehouse Gripper Client Started
========================================================
REST API:     http://localhost:8081/api/warehouse
Swagger UI:   http://localhost:8081/swagger-ui.html
========================================================
```

---

## API Endpoints

### Java REST API (Port 8081)

| Method | Endpoint | Beschreibung |
|--------|----------|--------------|
| GET | `/api/warehouse/health` | Health Check |
| GET | `/api/warehouse/grippers` | Alle Greifer |
| GET | `/api/warehouse/grippers/{id}` | Greifer nach ID |
| POST | `/api/warehouse/grippers/{id}/move?x={x}&y={y}&z={z}` | Greifer bewegen |
| POST | `/api/warehouse/grippers/{id}/pick?locationId={id}` | Aufnehmen |
| POST | `/api/warehouse/grippers/{id}/place?locationId={id}` | Absetzen |
| POST | `/api/warehouse/operations` | Operation erstellen |
| GET | `/api/warehouse/locations/available` | Freie Lagerplätze |

### WCF SOAP Service (Port 8080)

**WSDL**: `http://localhost:8080/WarehouseGripperService.svc?wsdl`

**Operations:**
- `GetGripperStatus(int gripperId)`
- `GetAllGrippers()`
- `MoveGripper(int gripperId, double x, double y, double z)`
- `PickLoadCarrier(int gripperId, int locationId)`
- `PlaceLoadCarrier(int gripperId, int locationId)`
- `CreateOperation(OperationRequestDto request)`
- `GetOperationStatus(int operationId)`
- `CancelOperation(int operationId)`
- `GetAvailableLocations()`
- `GetLocation(int locationId)`
- `IsServiceHealthy()`

---

## Beispiel-Workflows

### 1. Greifer-Status über Java API abrufen

```bash
curl http://localhost:8081/api/warehouse/grippers/1
```

Antwort:
```json
{
  "gripperId": 1,
  "state": "Idle",
  "positionX": 0.0,
  "positionY": 0.0,
  "positionZ": 0.0,
  "hasLoadCarrier": false,
  "isEnabled": true,
  "hasError": false,
  "lastUpdated": "2024-01-30T10:30:00"
}
```

### 2. Greifer bewegen via Java API

```bash
curl -X POST "http://localhost:8081/api/warehouse/grippers/1/move?x=750&y=400&z=0"
```

Antwort:
```json
{
  "success": true,
  "message": "Gripper 1 move command sent to position (750.0, 400.0, 0.0)",
  "timestamp": "2024-01-30T10:31:00"
}
```

### 3. Operation erstellen (Pick & Place)

```bash
curl -X POST http://localhost:8081/api/warehouse/operations \
  -H "Content-Type: application/json" \
  -d '{
    "gripperId": 1,
    "operationType": "Move",
    "sourceLocationId": 5,
    "targetLocationId": 12,
    "priority": "Normal"
  }'
```

Antwort:
```json
{
  "success": true,
  "operationId": 123,
  "message": "Operation 123 created successfully",
  "timestamp": "2024-01-30T10:32:00"
}
```

### 4. Direkte SOAP Anfrage (Test)

```bash
curl -X POST http://localhost:8080/WarehouseGripperService.svc \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://robot.warehouse.gripper/2024/IWarehouseGripperService/GetGripperStatus" \
  -d '<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:kar="http://robot.warehouse.gripper/2024">
  <soap:Body>
    <kar:GetGripperStatus>
      <kar:gripperId>1</kar:gripperId>
    </kar:GetGripperStatus>
  </soap:Body>
</soap:Envelope>'
```

---

## Postman Collection

Import [Postman_Collection.json](robotGripperBackend.Skeleton/Postman_Collection.json) in Postman:

1. Öffnen Sie Postman
2. File → Import
3. Wählen Sie `Postman_Collection.json`
4. Collection "robot Warehouse Gripper - WCF & REST API" erscheint

**Enthält:**
- ✅ Java REST API Requests (8081)
- ✅ .NET Nancy API Requests (5000)
- ✅ WCF SOAP Requests (8080)

---

## Troubleshooting

### WCF Service startet nicht

**Fehler:**
```
Unable to start CoreWCF service
```

**Lösung:**
- Prüfen Sie, ob Port 8080 frei ist:
```bash
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows
```

- CoreWCF Packages installiert?
```bash
dotnet list package | grep CoreWCF
```

### WSDL nicht verfügbar

**Fehler:**
```
404 Not Found - http://localhost:8080/WarehouseGripperService.svc?wsdl
```

**Lösung:**
- WCF Service läuft? Logs prüfen
- `ServiceMetadataBehavior.HttpGetEnabled = true` in [ProgramWithWcf.cs](robotGripperBackend.Skeleton/ProgramWithWcf.cs)?
- Browser öffnen: `http://localhost:8080/WarehouseGripperService.svc`

### Java WSDL Generation schlägt fehl

**Fehler:**
```
Failed to invoke wsimport
```

**Lösung:**
1. WCF Service muss laufen!
2. WSDL manuell testen:
```bash
curl http://localhost:8080/WarehouseGripperService.svc?wsdl
```
3. Wenn WSDL OK, Maven cache löschen:
```bash
mvn clean
rm -rf target/
mvn compile
```

### Java Client: Connection Refused

**Fehler:**
```
java.net.ConnectException: Connection refused
```

**Lösung:**
- WCF Service läuft auf Port 8080?
- [application.properties](JavaSpringBootClient/src/main/resources/application.properties) korrekt?
```properties
wcf.service.url=http://localhost:8080/WarehouseGripperService.svc
```

### SOAP Fault: GRIPPER_NOT_FOUND

**Fehler:**
```xml
<ServiceFault>
  <ErrorCode>GRIPPER_NOT_FOUND</ErrorCode>
  <ErrorMessage>Gripper with ID 99 not found</ErrorMessage>
</ServiceFault>
```

**Lösung:**
- Datenbank enthält nur Gripper 1 und 2 (Seed-Daten)
- Verwenden Sie existierende IDs: 1 oder 2

---

## Architektur-Details

### Warum WCF als Zwischenschicht?

1. **Java-Kompatibilität**: JAX-WS unterstützt SOAP/WSDL out-of-the-box
2. **Typsicherheit**: WSDL generiert Java-Klassen
3. **Standards**: SOAP ist standardisiert (vs. REST Variationen)
4. **Legacy Integration**: Viele Enterprise-Systeme nutzen SOAP

### Datenfluss: Java → WCF → Nancy

```
Java Client Request
  ↓
JAX-WS (SOAP)
  ↓
WCF Service (WarehouseGripperService.cs)
  ↓
GripperControlService.cs (bestehend)
  ↓
OperationQueueManager.cs (bestehend)
  ↓
SQL Server + CAN-Bus
```

**Wichtig**: Nancy REST API (Port 5000) bleibt voll funktionsfähig!

### WCF DataContracts

Alle DataContracts verwenden:
- `[DataContract(Namespace = "http://robot.warehouse.gripper/2024")]`
- `[DataMember(Order = N)]` für Serialisierung
- Mapping zu/von internen Models

**Beispiel:**
```csharp
[DataContract]
public class GripperStatusDto
{
    [DataMember(Order = 1)]
    public int GripperId { get; set; }

    [DataMember(Order = 2)]
    public string State { get; set; }
    // ...
}
```

### Fehlerbehandlung

WCF nutzt `FaultException<ServiceFault>`:

```csharp
throw new FaultException<ServiceFault>(
    new ServiceFault
    {
        ErrorCode = "GRIPPER_NOT_FOUND",
        ErrorMessage = $"Gripper {id} not found"
    },
    new FaultReason("Gripper not found"));
```

Java fängt als:
```java
try {
    port.getGripperStatus(id);
} catch (ServiceFault_Exception e) {
    log.error("WCF Error: {}", e.getFaultInfo().getErrorMessage());
}
```

---

## Erweiterungen

### Weitere WCF Operations hinzufügen

1. **Interface erweitern** ([IWarehouseGripperService.cs](robotGripperBackend.Skeleton/WcfService/IWarehouseGripperService.cs)):
```csharp
[OperationContract]
MyResponseDto MyNewOperation(MyRequestDto request);
```

2. **Implementation** ([WarehouseGripperService.cs](robotGripperBackend.Skeleton/WcfService/WarehouseGripperService.cs)):
```csharp
public MyResponseDto MyNewOperation(MyRequestDto request)
{
    // Implementation
}
```

3. **Java regenerieren**:
```bash
mvn clean compile  # WSDL neu generieren
```

4. **Java Service nutzen**:
```java
var result = port.myNewOperation(request);
```

### Authentifizierung hinzufügen

**WCF:**
```csharp
// In ProgramWithWcf.cs
serviceBuilder.AddServiceEndpoint<WarehouseGripperService, IWarehouseGripperService>(
    new WSHttpBinding
    {
        Security = new WSHttpSecurity
        {
            Mode = SecurityMode.TransportWithMessageCredential,
            Message = new NonDualMessageSecurityOverHttp
            {
                ClientCredentialType = MessageCredentialType.UserName
            }
        }
    },
    "/WarehouseGripperService.svc"
);
```

**Java:**
```java
BindingProvider bp = (BindingProvider) port;
bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "user");
bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "pass");
```

---

## Performance-Tipps

1. **Connection Pooling**: JAX-WS Client wiederverwerden
```java
@Bean
public IWarehouseGripperService wcfServicePort() {
    // Singleton Bean
}
```

2. **Timeout konfigurieren**:
```properties
wcf.service.timeout=30000  # 30 Sekunden
```

3. **Async Operations** (Java):
```java
CompletableFuture.supplyAsync(() ->
    wcfClient.moveGripper(id, x, y, z)
);
```

4. **SOAP Message Compression**:
```csharp
// In WCF Binding
binding.MessageEncoding = WSMessageEncoding.Mtom;
```

---

## Nächste Schritte

1. ✅ WCF Service mit .NET Backend deployen
2. ✅ Java Client mit WSDL generieren
3. ✅ REST API in Java testen
4. 📊 Monitoring mit Application Insights/Prometheus
5. 🔒 Authentifizierung implementieren
6. 🚀 Kubernetes Deployment vorbereiten

---

**Viel Erfolg mit der Integration!** 🎉

Bei Fragen siehe auch:
- [README.md](robotGripperBackend.Skeleton/README.md) - Haupt-Dokumentation
- [SETUP.md](robotGripperBackend.Skeleton/SETUP.md) - Quick Start Guide
