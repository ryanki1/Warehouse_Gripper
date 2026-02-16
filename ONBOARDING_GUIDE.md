# Robot Warehouse Gripper System - Developer Onboarding Guide

**Project**: Warehouse Gripper Administration System
**Status**: End-phase development, preparing for pilot installation at customer site
**Your Role**: Java Developer (3+ years experience) - Onboarding to contribute to final pre-production tasks

---

## Executive Summary

This project provides a **Spring Boot REST API** that acts as a modern interface layer for controlling warehouse gripper robots. The underlying gripper control logic is implemented in a legacy **.NET WCF SOAP service** (black box) that handles GDPR-sensitive operations at customer warehouse sites. Your role is to help finalize the Java client layer for pilot deployment at the customer's facilities.

---

## Information Block 1: Architecture & Technology Stack

### Overview

The system follows a **two-tier architecture**:

```
┌─────────────────────────────────────┐
│   REST API Layer (Spring Boot)     │  ← You work here
│   - Modern REST endpoints           │
│   - OpenAPI/Swagger documentation   │
│   - Circuit breaker resilience      │
│   - Health monitoring               │
└─────────────┬───────────────────────┘
              │
              │ JAX-WS SOAP
              ↓
┌─────────────────────────────────────┐
│   .NET WCF Service (Black Box)      │  ← Vendor-provided
│   - GDPR-sensitive data handling    │
│   - Gripper hardware control        │
│   - Location & inventory management │
└─────────────────────────────────────┘
```

### Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Java Runtime** | OpenJDK | 17 | LTS version for enterprise deployment |
| **Framework** | Spring Boot | 3.2.1 | REST API framework |
| **SOAP Client** | JAX-WS (Jakarta) | 4.0.x | WCF service integration |
| **Resilience** | Resilience4j | 3.1.0 | Circuit breaker for fault tolerance |
| **API Docs** | SpringDoc OpenAPI | 2.3.0 | Auto-generated Swagger UI |
| **Build Tool** | Maven | 3.x | Dependency & build management |
| **Testing** | JUnit 5 + MockMvc | - | Integration testing |
| **Code Quality** | Lombok | - | Reduces boilerplate code |

### Key Files & Structure

```
JavaSpringBootClient/
├── src/main/java/com/robot/warehouse/
│   ├── controller/
│   │   └── WarehouseGripperController.java    # REST endpoints
│   ├── service/
│   │   └── WcfGripperServiceClient.java       # WCF client wrapper
│   ├── dto/
│   │   ├── GripperStatusResponse.java         # API response models
│   │   ├── OperationRequest.java
│   │   └── OperationResponse.java
│   ├── config/
│   │   └── WcfServiceConfig.java              # WCF connection config
│   └── exception/
│       └── GlobalExceptionHandler.java        # Centralized error handling
├── src/main/resources/
│   ├── application.properties                 # Base configuration (safe)
│   └── application-dev.properties             # Environment-specific (NEVER commit)
└── pom.xml                                    # Maven dependencies
```

### REST API Endpoints

The controller exposes these operations:

| HTTP Method | Endpoint | Description | Circuit Breaker |
|-------------|----------|-------------|-----------------|
| `GET` | `/api/warehouse/health` | Health check | No |
| `GET` | `/api/warehouse/grippers` | List all grippers | Yes |
| `GET` | `/api/warehouse/grippers/{id}` | Get gripper status | Yes |
| `POST` | `/api/warehouse/grippers/{id}/move` | Move gripper to X,Y,Z position | Yes |
| `POST` | `/api/warehouse/grippers/{id}/pick` | Pick load carrier from location | **Yes** (with fallback) |
| `POST` | `/api/warehouse/grippers/{id}/place` | Place load carrier at location | Yes |
| `POST` | `/api/warehouse/operations` | Queue operation for execution | Yes |
| `GET` | `/api/warehouse/locations/available` | Get empty storage locations | Yes |

**Note**: The WCF client currently returns **mock data** because WSDL generation happens during the first Maven build with the .NET service running. See [WcfGripperServiceClient.java:20-24](JavaSpringBootClient/src/main/java/com/robot/warehouse/service/WcfGripperServiceClient.java#L20-L24) for integration instructions.

---

### 📋 **TASK 1: Local Development Environment Setup & API Exploration**

**Objective**: Get the Spring Boot application running and familiarize yourself with the REST API surface area.

#### Steps:

1. **Build the project**:
   ```bash
   cd JavaSpringBootClient
   mvn clean install
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

   The server starts on port `9999` (see [application.properties:2](JavaSpringBootClient/src/main/resources/application.properties#L2))

3. **Open the Swagger UI**:
   ```
   http://localhost:9999/swagger-ui.html
   ```

4. **Explore the API**:
   - Test the `/api/warehouse/health` endpoint (should return `true`)
   - Try `GET /api/warehouse/grippers` (returns mock data with 2 grippers)
   - Execute `POST /api/warehouse/grippers/1/move` with parameters: `x=100`, `y=200`, `z=50`
   - Observe the response structure and logging output in your terminal

5. **Review the logs**:
   - Notice the DEBUG-level logging for `com.robot.warehouse` package
   - Identify which service methods are called for each endpoint

#### Deliverable:
Create a brief summary (can be a comment in code or a note) documenting:
- Which endpoints return mock data vs. real WCF responses
- The structure of `OperationResponse` and `GripperStatusResponse` DTOs
- Any questions about the API design or missing functionality

**Success Criteria**: You can make successful REST API calls and understand the request/response flow.

---

## Information Block 2: Resilience4j Circuit Breaker Pattern

### Why Circuit Breakers?

In a warehouse automation system, **network failures** or **service unavailability** can cascade:

1. The .NET WCF service might become unresponsive (network issue, service restart, etc.)
2. Without protection, each REST API call would **wait for timeout** (30 seconds per call)
3. Threads would accumulate, causing the Spring Boot application to become unresponsive
4. Warehouse operations would halt completely

**Circuit Breaker Pattern** prevents this by:
- **Failing fast** when the downstream service is unhealthy
- **Giving the service time to recover** before retrying
- **Protecting system resources** (threads, connections)

### Circuit Breaker Configuration

See [application.properties:21-30](JavaSpringBootClient/src/main/resources/application.properties#L21-L30) for the `pick-gripper` circuit breaker:

```properties
resilience4j.circuitbreaker.instances.pick-gripper.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.pick-gripper.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.pick-gripper.sliding-window-size=5
resilience4j.circuitbreaker.instances.pick-gripper.wait-duration-in-open-state=30s
```

**What this means**:
- After **5 consecutive failures**, the circuit opens
- Circuit stays **OPEN for 30 seconds** (rejects all calls immediately)
- After 30s, enters **HALF-OPEN** state (allows 2 test calls)
- If test calls succeed → **CLOSED** (normal operation resumes)
- If test calls fail → **OPEN** again for another 30s

### Circuit Breaker States

```
┌──────────────┐
│   CLOSED     │  Normal operation - calls pass through
│  (Working)   │
└──────┬───────┘
       │
       │ 5 failures
       ↓
┌──────────────┐
│     OPEN     │  Fail fast - reject calls immediately
│  (Broken)    │  Returns: HTTP 503 SERVICE_UNAVAILABLE
└──────┬───────┘
       │
       │ After 30s
       ↓
┌──────────────┐
│  HALF-OPEN   │  Testing - allow 2 probe calls
│  (Testing)   │
└──────┬───────┘
       │
       ├─ Success → CLOSED
       └─ Failure → OPEN
```

### Fallback Method Implementation

See [WarehouseGripperController.java:35-52](JavaSpringBootClient/src/main/java/com/robot/warehouse/controller/WarehouseGripperController.java#L35-L52):

```java
@CircuitBreaker(name="pick-gripper", fallbackMethod = "pickFallback")
@PostMapping("/grippers/{id}/pick")
public ResponseEntity<OperationResponse> pickLoadCarrier(
        @PathVariable int id,
        @RequestParam int locationId) throws Exception {
    // Primary method
}

private ResponseEntity<OperationResponse> pickFallback(
        int id, int locationId, Throwable ex) {
    if (ex instanceof CallNotPermittedException) {
        // Circuit is OPEN - return 503
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    } else {
        // Other error (e.g., location empty) - return 400
        return ResponseEntity.badRequest().body(error);
    }
}
```

**Key Points**:
- `CallNotPermittedException` indicates circuit is OPEN
- Fallback distinguishes between **service unavailable** vs. **business logic errors**
- Only the `pick-gripper` circuit has a custom fallback (others use default behavior)

---

### 📋 **TASK 2: Circuit Breaker Testing & Analysis**

**Objective**: Understand and test the circuit breaker behavior to ensure resilience during pilot deployment.

#### Steps:

1. **Review the integration test**:
   - Open [WarehouseGripperControllerIntegrationTest.java:74-97](JavaSpringBootClient/src/test/java/com/robot/warehouse/controller/WarehouseGripperControllerIntegrationTest.java#L74-L97)
   - Study the `testCircuitBreakerOpensAfterFailures()` test
   - Understand how it simulates 5 failures to open the circuit

2. **Run the circuit breaker test**:
   ```bash
   mvn test -Dtest=WarehouseGripperControllerIntegrationTest#testCircuitBreakerOpensAfterFailures
   ```

3. **Analyze the test output**:
   - Observe the first 5 calls returning `400 BAD_REQUEST` (business error)
   - Notice the 6th call returns `503 SERVICE_UNAVAILABLE` (circuit OPEN)
   - Check the logs for circuit state transitions

4. **Manual testing** (optional but recommended):
   - Start the application: `mvn spring-boot:run`
   - Use curl or Postman to call the pick endpoint with an invalid location:
     ```bash
     for i in {1..5}; do
       curl -X POST "http://localhost:9999/api/warehouse/grippers/1/pick?locationId=99"
       echo "\n---"
     done
     ```
   - On the 6th call, you should see the circuit open

5. **Investigate other circuit breakers**:
   - Search the codebase: Why does only `pick-gripper` have a fallback method?
   - What happens to other endpoints (`move-gripper`, `place-gripper`) when their circuits open?
   - Should they also have custom fallbacks?

#### Deliverable:
Write a short analysis answering:
- **When should the circuit breaker NOT fail fast?** (Hint: Consider business errors vs. infrastructure errors)
- **Do other operations need custom fallback methods?** Justify your recommendation.
- **How would you monitor circuit breaker state in production?** (Think: metrics, alerts, dashboards)

**Success Criteria**: You can explain circuit breaker behavior and identify potential improvements for production deployment.

---

## Information Block 3: GDPR Compliance & Pre-Production Readiness

### The Compliance Challenge

The customer operates in the EU medical supply sector, making this project subject to:
- **GDPR** (General Data Protection Regulation)
- **DSGVO** (German implementation of GDPR)

The .NET WCF service handles sensitive data:
- **Infrastructure**: IP addresses, hostnames, network topology
- **Credentials**: Service accounts, API keys, connection strings
- **Business data**: Customer warehouse layouts, inventory systems

### Critical Rule

**🚨 NEVER commit or expose**:
- `application-dev.properties`, `application-prod.properties` (environment-specific configs)
- Any file in `robotGripperBackend.Skeleton/` directory (vendor code)
- IP addresses, hostnames, or URLs (except `localhost`, `example.com`)
- Passwords, tokens, or certificates

**✅ SAFE to commit**:
- `application.properties` (base configuration with placeholders)
- Java source code in `src/` directories
- Test files (`*Test.java`)
- This documentation

### Protected Files

See [Project_Instructions.md:43-53](Project_Instructions.md#L43-L53) for the complete exclusion list:

```
🚫 robotGripperBackend.Skeleton/     # Vendor .NET code (GDPR-sensitive)
🚫 application-*.properties          # Environment configs (secrets)
🚫 target/, build/                   # Build output
```

These are already in `.gitignore`, but **you must be vigilant** when:
- Creating code examples
- Writing commit messages
- Debugging (don't paste logs with sensitive data into chat tools)
- Documenting deployment

### Git Workflow

See [Project_Instructions.md:116-121](Project_Instructions.md#L116-L121):

- **YOU (the developer)** create all commits
- AI tools (Claude, Copilot, etc.) may suggest changes but **never commit directly**
- Always review `git diff` before committing

### Pre-Deployment Checklist

Before pilot installation at customer site:

| Category | Requirement | Status | Responsible |
|----------|------------|--------|-------------|
| **Functional** | All REST endpoints working with real WCF service | ⚠️ Pending WSDL generation | Team |
| **Functional** | Circuit breaker tested under load | ⚠️ TODO | **You** (Task 2) |
| **Security** | No hardcoded credentials in code | ✅ Verified | Team |
| **Security** | Environment-specific configs use placeholders | ✅ Verified | Team |
| **Security** | HTTPS/TLS configured for production | ⚠️ TODO | DevOps |
| **Compliance** | No GDPR-sensitive data in Git history | ✅ Verified | Team |
| **Testing** | Integration tests passing | ⚠️ 1 test disabled | **You** (Task 3) |
| **Monitoring** | Health check endpoint tested | ⚠️ TODO | **You** (Task 1) |
| **Docs** | API documentation (Swagger) reviewed | ⚠️ TODO | **You** (Task 1) |

---

### 📋 **TASK 3: Pre-Production Compliance & Readiness Verification**

**Objective**: Ensure the codebase is production-ready and compliant before customer deployment.

#### Steps:

1. **Verify `.gitignore` is working**:
   ```bash
   git status
   ```

   **Check**:
   - No `application-dev.properties` or `application-prod.properties` files listed
   - No files from `robotGripperBackend.Skeleton/` directory
   - No `target/` build output

2. **Review Git history for leaked secrets**:
   ```bash
   git log --all --full-history -- "*.properties"
   ```

   **Verify**: Only `application.properties` (base file) appears in history.

3. **Scan code for hardcoded sensitive data**:
   - Search for patterns like:
     - IP addresses: `grep -r "192.168." JavaSpringBootClient/src/`
     - Passwords: `grep -ri "password\s*=" JavaSpringBootClient/src/`

   **All values should be placeholders**: `${ENVIRONMENT_VARIABLE:default-value}`

4. **Fix the disabled test**:
   - Open [WarehouseGripperControllerIntegrationTest.java:99-124](JavaSpringBootClient/src/test/java/com/robot/warehouse/controller/WarehouseGripperControllerIntegrationTest.java#L99-L124)
   - The `testPickFromEmptyLocationFails()` test is marked `@Disabled`
   - Investigate why it was disabled (check comments and Git history)
   - Fix the test (or document why it should remain disabled)

5. **Run the full test suite**:
   ```bash
   mvn test
   ```

   **Target**: All tests should pass (or have documented reasons for being disabled).

6. **Document deployment prerequisites**:
   - What environment variables must be set in production? (e.g., `WCF_SERVICE_URL`)
   - What network connectivity is required? (ports, firewall rules)
   - What credentials need to be provisioned?

#### Deliverable:
Create a **Pre-Production Checklist** document (or update this file) with:
- ✅/❌ status for each security verification step
- A list of **environment variables** required for production deployment
- Status of the disabled test + remediation plan
- Any GDPR compliance concerns you identified

**Success Criteria**: You can confidently say "no sensitive data is committed" and all tests pass.

---

## Quick Reference

### Common Commands

```bash
# Build
mvn clean install

# Run locally
mvn spring-boot:run

# Run tests
mvn test

# Run specific test
mvn test -Dtest=WarehouseGripperControllerIntegrationTest

# Check for uncommitted files
git status

# View recent commits
git log --oneline -10

# Search for sensitive data patterns
grep -r "192.168." JavaSpringBootClient/src/
```

### Important URLs (Local Development)

| Service | URL |
|---------|-----|
| Spring Boot App | `http://localhost:9999` |
| Swagger UI | `http://localhost:9999/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:9999/api-docs` |
| Health Check | `http://localhost:9999/api/warehouse/health` |
| .NET WCF Service (Mock) | `http://localhost:9999/mock` |

### Key Contacts

| Role | Responsibility |
|------|---------------|
| **Project Lead** | Overall delivery, customer liaison |
| **Senior Java Dev** | Spring Boot architecture, code reviews |
| **.NET Vendor** | WCF service support (black box) |
| **DevOps/SRE** | Production deployment, infrastructure |
| **Compliance Officer** | GDPR/DSGVO verification |

---

## Next Steps After Onboarding

Once you complete Tasks 1-3, you'll be ready to contribute to:

1. **WCF Integration Completion**:
   - Uncomment generated WSDL stub code in [WcfGripperServiceClient.java](JavaSpringBootClient/src/main/java/com/robot/warehouse/service/WcfGripperServiceClient.java)
   - Test real WCF service calls
   - Handle WCF-specific exceptions (`ServiceFault_Exception`)

2. **Production Hardening**:
   - Add more comprehensive error handling
   - Implement request/response logging for audit trails
   - Add circuit breaker monitoring/metrics

3. **Integration Testing**:
   - Expand test coverage (currently only 2 tests)
   - Add end-to-end tests with WCF service
   - Load testing to validate circuit breaker thresholds

4. **Deployment Preparation**:
   - Create production configuration templates
   - Write deployment documentation for customer IT team
   - Participate in pilot installation planning

---

## Questions?

If you have questions while working through the tasks:

1. **Technical questions**: Ask the senior Java developer (mention file path + line number)
2. **Architecture questions**: Review this document and [Project_Instructions.md](Project_Instructions.md)
3. **GDPR questions**: Consult the compliance officer BEFORE committing any questionable code
4. **WCF service questions**: Contact the .NET vendor (but remember it's a black box)

**Welcome to the team!** 🎉

Your contributions over the next few weeks will directly impact the success of the customer pilot deployment. Focus on quality, security, and resilience.
