# Wichtige Kommandos - Robot Warehouse Gripper Project

## .NET WCF Service starten

### WCF-Service starten (Port 8080)
```bash
# In das .NET Verzeichnis wechseln
cd robotGripperBackend.Skeleton

# Projekt bauen (einmalig oder nach Änderungen)
dotnet restore
dotnet build

# WCF Service starten
dotnet run
```

**Erwartete Ausgabe:**
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

**Hinweis:** Der WCF-Service läuft NICHT in Docker, sondern als .NET Prozess!

---

## Docker Befehle

### Docker Container Status prüfen
```bash
# Zeigt alle laufenden Container mit Details
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"

# Zeigt alle Container (inkl. gestoppte)
docker ps -a

# Prüfen ob Container auf Port 8080 läuft (WCF-Service)
docker ps | grep 8080

# Prüfen ob Container auf Port 1433 läuft (SQL Server)
docker ps | grep 1433
```

### Docker Container starten/stoppen
```bash
# Docker Compose Services starten
docker-compose up -d

# Docker Compose Services stoppen
docker-compose down

# Einzelnen Container starten
docker start <container-name>

# Einzelnen Container stoppen
docker stop <container-name>

# Container Logs anzeigen
docker logs <container-name>

# Container Logs live verfolgen
docker logs -f <container-name>
```

## Spring Boot Befehle

### Maven Build & Run
```bash
# Build mit Maven
mvn clean compile

# Tests ausführen
mvn test

# Spring Boot starten
mvn spring-boot:run

# Build mit Tests überspringen
mvn clean install -DskipTests
```

### Application Status prüfen
```bash
# Health Check
curl http://localhost:9999/actuator/health

# Spring Boot Endpoints anzeigen
curl http://localhost:9999/actuator

# Prometheus Metrics
curl http://localhost:9999/actuator/prometheus
```

## WCF Service Befehle

### WCF Service prüfen
```bash
# WSDL abrufen (prüft ob Service läuft)
curl http://localhost:8080/WarehouseGripperService.svc?wsdl

# Service Endpoint prüfen
curl http://localhost:8080/WarehouseGripperService.svc
```

## API Testing Befehle

### REST API Endpoints testen

```bash
# Health Check
curl http://localhost:9999/api/warehouse/health

# Alle Grippers abrufen
curl -s http://localhost:9999/api/warehouse/grippers

# Einzelnen Gripper abrufen
curl http://localhost:9999/api/warehouse/grippers/1

# Gripper bewegen (POST)
curl -X POST "http://localhost:9999/api/warehouse/grippers/1/move?x=10.5&y=20.3&z=5.0"

# Load Carrier picken (POST)
curl -X POST "http://localhost:9999/api/warehouse/grippers/1/pick?locationId=100"

# Load Carrier platzieren (POST)
curl -X POST "http://localhost:9999/api/warehouse/grippers/1/place?locationId=200"

# Verfügbare Locations abrufen
curl http://localhost:9999/api/warehouse/locations/available

# Operation erstellen (POST mit JSON Body)
curl -X POST http://localhost:9999/api/warehouse/operations \
  -H "Content-Type: application/json" \
  -d '{
    "gripperId": 1,
    "operationType": "PICK_AND_PLACE",
    "sourceLocationId": 100,
    "targetLocationId": 200,
    "loadCarrierId": "LC-001",
    "priority": 5
  }'
```

### JSON formatiert ausgeben (optional)
```bash
# Mit jq (falls installiert)
curl -s http://localhost:9999/api/warehouse/grippers | jq

# jq installieren (macOS)
brew install jq
```

## Nützliche Debug Befehle

### Port-Nutzung prüfen
```bash
# Prüfen welcher Prozess Port 9999 verwendet (Spring Boot)
lsof -i :9999

# Prüfen welcher Prozess Port 8080 verwendet (WCF Service)
lsof -i :8080

# Prüfen welcher Prozess Port 1433 verwendet (SQL Server)
lsof -i :1433
```

### Java Prozesse prüfen
```bash
# Alle Java Prozesse anzeigen
ps aux | grep java

# Java Version prüfen
java -version
```

### Logs anzeigen
```bash
# Spring Boot Logs (wenn mit mvn spring-boot:run gestartet)
# Logs erscheinen direkt im Terminal

# Docker Container Logs
docker logs robot-wcf-service

# Docker Container Logs live
docker logs -f robot-wcf-service
```

## Git Befehle

### Status prüfen
```bash
# Git Status anzeigen
git status

# Änderungen anzeigen
git diff

# Staged Änderungen anzeigen
git diff --staged
```

## Swagger/OpenAPI

### API Dokumentation öffnen
```bash
# Swagger UI im Browser öffnen
open http://localhost:9999/swagger-ui.html

# OpenAPI JSON abrufen
curl http://localhost:9999/api-docs
```

---

## Troubleshooting

### Problem: "Connection refused" bei curl
**Lösung:**
1. Prüfen ob Spring Boot läuft: `ps aux | grep java`
2. Prüfen ob Port korrekt: `lsof -i :9999`
3. Spring Boot neu starten: `mvn spring-boot:run`

### Problem: "WCF service connection failed"
**Lösung:**
1. Prüfen ob Docker läuft: `docker ps`
2. Prüfen ob WCF Container läuft: `docker ps | grep 8080`
3. WSDL testen: `curl http://localhost:8080/WarehouseGripperService.svc?wsdl`
4. Docker Container starten: `docker-compose up -d`

### Problem: "ServiceFault_Exception cannot be resolved"
**Lösung:**
1. WSDL neu generieren: `mvn clean compile`
2. IDE refresh/reimport
3. Korrekte Exception-Klasse verwenden (siehe generated code)

---

## Wichtige Konfiguration

- **Spring Boot Port:** 9999 (konfiguriert in `application.properties`)
- **WCF Service Port:** 8080 (konfiguriert in `application.properties`)
- **SQL Server Port:** 1433 (Standard)
- **WSDL URL:** http://localhost:8080/WarehouseGripperService.svc?wsdl
