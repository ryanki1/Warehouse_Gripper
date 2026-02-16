# Robot Warehouse Gripper System - Code-Oriented Onboarding (Part 2)

**Project**: Warehouse Gripper Administration System
**Status**: End-phase development - Circuit breaker ✅, WSDL integration ⚠️, Production readiness ❌
**Timeline**: Pilot installations at customer site in 2-3 weeks

---

## Progress Update: What's Been Completed

Since the initial development phase, **significant progress** has been made:

| Component | Status | Notes |
|-----------|--------|-------|
| **Circuit Breaker** | ✅ **COMPLETE** | Resilience4j integrated, `pick-gripper` has custom fallback |
| **Basic Logging** | ✅ **COMPLETE** | SLF4J with DEBUG level for `com.robot.warehouse` |
| **REST API Structure** | ✅ **COMPLETE** | 8 endpoints with Swagger documentation |
| **DTO Models** | ✅ **COMPLETE** | Request/Response objects with Lombok |
| **WSDL Integration** | ⚠️ **PENDING** | Code scaffolded but commented out, awaiting first WSDL generation |
| **Prometheus Metrics** | ❌ **TODO** | Micrometer dependency missing |
| **Integration Tests** | ⚠️ **PARTIAL** | 2 tests exist, 1 disabled, needs expansion |
| **Error Handling** | ⚠️ **BASIC** | GlobalExceptionHandler exists but needs enhancement |

---

## Your Mission: Three Code-Focused Deliverables

You're joining at a critical juncture. The architecture is solid, but we need **production-grade code** before customer pilots. Here are three hands-on coding tasks that will directly impact deployment success.

---

## 📊 Code Task #1: Production Observability - Prometheus Metrics & Structured Logging

### The Problem

**Current State**: We log messages to console. That's it.

**What happens during pilot installation**:
- 4 AM: Customer warehouse operator reports "Gripper 3 won't pick from Aisle 2, Row 5"
- Support team sees: "INFO - Pick command sent (MOCK)" in logs
- Questions we **cannot** answer:
  - How many SOAP calls failed in the last hour?
  - What's the P95 latency for `pickLoadCarrier` operations?
  - Is this a single gripper issue or systemic WCF service degradation?
  - Are we meeting the 99.5% uptime SLA?

**Why this is critical**: Customer SLAs will be based on **measurable metrics**, not "it seems to be working."

---

### Implementation Guide

#### Step 1: Add Dependencies

Edit [pom.xml](JavaSpringBootClient/pom.xml) and add after line 95 (after spring-boot-starter-aop):

```xml
<!-- Micrometer Core -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>

<!-- Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Spring Boot Actuator for /actuator/prometheus endpoint -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### Step 2: Configure Actuator Endpoints

Add to [application.properties](JavaSpringBootClient/src/main/resources/application.properties) at the end:

```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

#### Step 3: Create Metrics Configuration Class

Create `JavaSpringBootClient/src/main/java/com/robot/warehouse/config/MetricsConfig.java`:

```java
package com.robot.warehouse.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    /**
     * Counter for total WCF SOAP calls
     * Tags: operation (move, pick, place), status (success, failure)
     */
    @Bean
    public Counter wcfSoapCallsTotal(MeterRegistry registry) {
        return Counter.builder("wcf.soap.calls.total")
                .description("Total number of WCF SOAP calls")
                .tag("operation", "unknown")
                .tag("status", "unknown")
                .register(registry);
    }

    /**
     * Timer for SOAP call duration (includes P95, P99 percentiles)
     */
    @Bean
    public Timer wcfSoapDuration(MeterRegistry registry) {
        return Timer.builder("wcf.soap.duration")
                .description("Duration of WCF SOAP calls in seconds")
                .tag("operation", "unknown")
                .register(registry);
    }
}
```

#### Step 4: Instrument WcfGripperServiceClient

Update [WcfGripperServiceClient.java](JavaSpringBootClient/src/main/java/com/robot/warehouse/service/WcfGripperServiceClient.java):

**Best Practice**: Define Timer and Counter as fields initialized in constructor (avoids registry lookups on every call).

Add these fields after line 31:

```java
private final MeterRegistry registry;

// Metrics fields - initialized once in constructor for performance
private final Timer getGripperStatusSuccessTimer;
private final Timer getGripperStatusFailureTimer;
private final Counter getGripperStatusSuccessCounter;
private final Counter getGripperStatusFailureCounter;

// Repeat for all operations: getAllGrippers, moveGripper, pickLoadCarrier,
// placeLoadCarrier, createOperation, getAvailableLocations
```

Initialize all metrics in constructor:

```java
public WcfGripperServiceClient(WcfServiceConfig config, MeterRegistry registry) {
    this.config = config;
    this.registry = registry;

    // Initialize all metrics once - avoids registry lookup on every operation
    this.getGripperStatusSuccessTimer = registry.timer("wcf.soap.duration",
            "operation", "getGripperStatus", "status", "success");
    this.getGripperStatusFailureTimer = registry.timer("wcf.soap.duration",
            "operation", "getGripperStatus", "status", "failure");
    this.getGripperStatusSuccessCounter = registry.counter("wcf.soap.calls.total",
            "operation", "getGripperStatus", "status", "success");
    this.getGripperStatusFailureCounter = registry.counter("wcf.soap.calls.total",
            "operation", "getGripperStatus", "status", "failure");

    // Repeat initialization for all 7 operations (28 total fields)
}
```

Then wrap each method with metrics using field references. Example for `getGripperStatus()`:

```java
public GripperStatusResponse getGripperStatus(int gripperId) {
    log.info("Getting status for Gripper {}", gripperId);

    Timer.Sample sample = Timer.start(registry);

    try {
        /* GENERATED CODE - Uncomment after WSDL generation:
        IWarehouseGripperService port = getServicePort();
        GripperStatusDto wcfResult = port.getGripperStatus(gripperId);

        sample.stop(getGripperStatusSuccessTimer);
        getGripperStatusSuccessCounter.increment();

        return mapToGripperStatusResponse(wcfResult);
        */

        // TEMPORARY MOCK DATA
        GripperStatusResponse response = GripperStatusResponse.builder()
                .gripperId(gripperId)
                .state("Idle")
                .positionX(0.0)
                .positionY(0.0)
                .positionZ(0.0)
                .hasLoadCarrier(false)
                .isEnabled(true)
                .hasError(false)
                .lastUpdated(LocalDateTime.now())
                .build();

        sample.stop(getGripperStatusSuccessTimer);
        getGripperStatusSuccessCounter.increment();

        return response;

    } catch (Exception e) {
        sample.stop(getGripperStatusFailureTimer);
        getGripperStatusFailureCounter.increment();

        log.error("Failed to get gripper status", e);
        throw new RuntimeException("Failed to get gripper status", e);
    }
}
```

**Repeat this pattern for**:
- `getAllGrippers()`
- `moveGripper()`
- `pickLoadCarrier()`
- `placeLoadCarrier()`
- `createOperation()`
- `getAvailableLocations()`

#### Step 5: Add Correlation ID Support (MDC Logging)

Create `JavaSpringBootClient/src/main/java/com/robot/warehouse/filter/CorrelationIdFilter.java`:

```java
package com.robot.warehouse.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Get correlation ID from header or generate new one
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        log.debug("Request received with correlation ID: {}", correlationId);

        // Add correlation ID to response header for client-side tracking
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
```

Update logging pattern in [application.properties](JavaSpringBootClient/src/main/resources/application.properties) line 14:

```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%X{correlationId}] - %msg%n
```

#### Step 6: Test the Metrics

```bash
# Start the application
mvn spring-boot:run

# Make some API calls
curl http://localhost:9999/api/warehouse/grippers
curl http://localhost:9999/api/warehouse/grippers/1
curl "http://localhost:9999/api/warehouse/grippers/1/move?x=100&y=200&z=50"

# View Prometheus metrics
curl http://localhost:9999/actuator/prometheus | grep wcf
```

**Expected output**:
```
# HELP wcf_soap_calls_total Total number of WCF SOAP calls
# TYPE wcf_soap_calls_total counter
wcf_soap_calls_total{operation="getGripperStatus",status="success",} 2.0
wcf_soap_calls_total{operation="getAllGrippers",status="success",} 1.0
wcf_soap_calls_total{operation="moveGripper",status="success",} 1.0

# HELP wcf_soap_duration_seconds Duration of WCF SOAP calls in seconds
# TYPE wcf_soap_duration_seconds summary
wcf_soap_duration_seconds_count{operation="getGripperStatus",} 2.0
wcf_soap_duration_seconds_sum{operation="getGripperStatus",} 0.042
```

#### Step 7: Create Grafana Dashboard (Optional but Recommended)

Create `JavaSpringBootClient/grafana-dashboard.json`:

```json
{
  "dashboard": {
    "title": "Warehouse Gripper - Production Monitoring",
    "panels": [
      {
        "title": "SOAP Call Success Rate (5 min)",
        "targets": [
          {
            "expr": "sum(rate(wcf_soap_calls_total{status=\"success\"}[5m])) / sum(rate(wcf_soap_calls_total[5m])) * 100"
          }
        ],
        "type": "graph"
      },
      {
        "title": "P95 SOAP Latency by Operation",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(wcf_soap_duration_seconds_bucket[5m]))"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Error Rate by Operation",
        "targets": [
          {
            "expr": "sum(rate(wcf_soap_calls_total{status=\"failure\"}[5m])) by (operation)"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

---

### 📋 Deliverables for Task #1

- [ ] Micrometer + Prometheus dependencies added to `pom.xml`
- [ ] All 7 WCF service methods instrumented with metrics
- [ ] Correlation ID filter implemented and tested
- [ ] `/actuator/prometheus` endpoint returns metrics
- [ ] Documentation: Create `METRICS.md` listing all custom metrics and their purpose
- [ ] (Bonus) Grafana dashboard JSON

**Success Criteria**:
- Run 100 API calls, then verify Prometheus metrics show correct counts and latencies
- Every log line includes `[correlationId]`

---

## 🧪 Code Task #2: Complete Integration Test Suite

### The Problem

**Current test coverage**:
- ✅ `testCircuitBreakerOpensAfterFailures()` - Works correctly
- ❌ `testPickFromEmptyLocationFails()` - Disabled with `@Disabled("TODO [kr] include")`
- ❌ No end-to-end workflow tests
- ❌ No load/concurrency tests

**What customers will do during pilot**:
```
07:00 - Operator queues 50 pick operations for morning dispatch
07:15 - First gripper encounters empty location (inventory mismatch)
07:16 - System should: fail that operation, continue with others
07:30 - Peak load: 4 grippers operating concurrently
08:00 - All orders fulfilled, grippers return to idle
```

**We need tests that simulate this reality**.

---

### Implementation Guide

#### Step 1: Fix the Disabled Test

Open [WarehouseGripperControllerIntegrationTest.java:99-124](JavaSpringBootClient/src/test/java/com/robot/warehouse/controller/WarehouseGripperControllerIntegrationTest.java#L99-L124).

The issue: The test was disabled with a workaround. Let's investigate and fix it.

**Replace the disabled test** (lines 99-124) with:

```java
@Test
void testPickFromEmptyLocationFails() throws Exception {
    // Arrange
    OperationResponse response = new OperationResponse();
    response.setSuccess(false);
    response.setErrorCode("LOAD_CARRIER");
    response.setMessage("Location is empty");

    when(wcfClient.pickLoadCarrier(1, 99))
        .thenReturn(response);

    // Act & Assert
    mvc.perform(
        post("/api/warehouse/grippers/1/pick")
            .param("locationId", "99"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("LOAD_CARRIER"))
        .andExpect(jsonPath("$.message").value("Location is empty"));

    // Verify WCF client was called exactly once
    verify(wcfClient, times(1)).pickLoadCarrier(1, 99);
}
```

**Why it was disabled**: The error code in the fallback method ([WarehouseGripperController.java:47](JavaSpringBootClient/src/main/java/com/robot/warehouse/controller/WarehouseGripperController.java#L47)) returns `"LOAD_CARRIER"`, but the test expected `"LOCATION_EMPTY"`. Fixed above.

#### Step 2: Add End-to-End Workflow Test

Add this test to `WarehouseGripperControllerIntegrationTest.java`:

```java
@Test
void testCompletePickAndPlaceCycle() throws Exception {
    // Scenario: Pick from Location 5, Place at Location 12

    // 1. Verify gripper starts idle
    GripperStatusResponse idleStatus = new GripperStatusResponse();
    idleStatus.setGripperId(1);
    idleStatus.setState("Idle");
    idleStatus.setHasLoadCarrier(false);

    when(wcfClient.getGripperStatus(1)).thenReturn(idleStatus);

    mvc.perform(get("/api/warehouse/grippers/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("Idle"))
        .andExpect(jsonPath("$.hasLoadCarrier").value(false));

    // 2. Execute pick operation
    OperationResponse pickSuccess = new OperationResponse();
    pickSuccess.setSuccess(true);
    pickSuccess.setMessage("Pick successful");
    pickSuccess.setOperationId(101);

    when(wcfClient.pickLoadCarrier(1, 5)).thenReturn(pickSuccess);

    mvc.perform(
        post("/api/warehouse/grippers/1/pick")
            .param("locationId", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.operationId").value(101));

    // 3. Verify gripper now has load carrier
    GripperStatusResponse withLoad = new GripperStatusResponse();
    withLoad.setGripperId(1);
    withLoad.setState("Idle");
    withLoad.setHasLoadCarrier(true);
    withLoad.setCurrentLoadCarrierId("LC-5001");

    when(wcfClient.getGripperStatus(1)).thenReturn(withLoad);

    mvc.perform(get("/api/warehouse/grippers/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasLoadCarrier").value(true))
        .andExpect(jsonPath("$.currentLoadCarrierId").value("LC-5001"));

    // 4. Execute place operation
    OperationResponse placeSuccess = new OperationResponse();
    placeSuccess.setSuccess(true);
    placeSuccess.setMessage("Place successful");
    placeSuccess.setOperationId(102);

    when(wcfClient.placeLoadCarrier(1, 12)).thenReturn(placeSuccess);

    mvc.perform(
        post("/api/warehouse/grippers/1/place")
            .param("locationId", "12"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.operationId").value(102));

    // 5. Verify gripper returned to idle without load
    when(wcfClient.getGripperStatus(1)).thenReturn(idleStatus);

    mvc.perform(get("/api/warehouse/grippers/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("Idle"))
        .andExpect(jsonPath("$.hasLoadCarrier").value(false));

    // Verify all operations executed in correct order
    verify(wcfClient, times(1)).pickLoadCarrier(1, 5);
    verify(wcfClient, times(1)).placeLoadCarrier(1, 12);
    verify(wcfClient, times(3)).getGripperStatus(1);
}
```

#### Step 3: Add Concurrent Operations Test

Add this test (requires additional dependencies):

First, add to `pom.xml` in `<dependencies>`:

```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```

Then add the test:

```java
@Test
void testConcurrentMoveOperations() throws Exception {
    // Simulate 10 concurrent move operations
    OperationResponse moveSuccess = new OperationResponse();
    moveSuccess.setSuccess(true);
    moveSuccess.setMessage("Move successful");

    when(wcfClient.moveGripper(anyInt(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(moveSuccess);

    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<Future<ResultActions>> futures = new ArrayList<>();

    // Submit 10 concurrent requests
    for (int i = 0; i < 10; i++) {
        final int gripperId = (i % 2) + 1; // Alternate between gripper 1 and 2
        final double x = 100.0 + i * 10;

        Future<ResultActions> future = executor.submit(() ->
            mvc.perform(
                post("/api/warehouse/grippers/" + gripperId + "/move")
                    .param("x", String.valueOf(x))
                    .param("y", "200.0")
                    .param("z", "50.0"))
        );
        futures.add(future);
    }

    // Verify all requests completed successfully
    for (Future<ResultActions> future : futures) {
        ResultActions result = future.get(5, TimeUnit.SECONDS);
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.success").value(true));
    }

    executor.shutdown();

    // Verify WCF client handled all 10 requests
    verify(wcfClient, times(10)).moveGripper(anyInt(), anyDouble(), anyDouble(), anyDouble());
}
```

#### Step 4: Add Circuit Breaker Recovery Test

```java
@Test
void testCircuitBreakerRecoveryAfterWaitDuration() throws Exception {
    OperationResponse failResponse = new OperationResponse();
    failResponse.setSuccess(false);
    failResponse.setErrorCode("LOAD_CARRIER");
    failResponse.setMessage("Location is empty");

    // Cause circuit to open (5 failures)
    when(wcfClient.pickLoadCarrier(1, 99)).thenReturn(failResponse);

    for (int i = 0; i < 5; i++) {
        mvc.perform(
            post("/api/warehouse/grippers/1/pick").param("locationId", "99"))
            .andExpect(status().isBadRequest());
    }

    // Next call should be rejected (circuit OPEN)
    mvc.perform(
        post("/api/warehouse/grippers/1/pick").param("locationId", "99"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));

    // Wait for circuit to transition to HALF-OPEN (30s configured)
    // For testing, we'll manually reset it
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pick-gripper");
    circuitBreaker.transitionToHalfOpenState();

    // Now mock a successful response
    OperationResponse successResponse = new OperationResponse();
    successResponse.setSuccess(true);
    successResponse.setMessage("Pick successful");

    when(wcfClient.pickLoadCarrier(1, 5)).thenReturn(successResponse);

    // This should succeed and close the circuit
    mvc.perform(
        post("/api/warehouse/grippers/1/pick").param("locationId", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Verify circuit is closed
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
}
```

#### Step 5: Add Test Documentation

Create `JavaSpringBootClient/TEST_PLAN.md`:

```markdown
# Integration Test Plan

## Test Execution

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=WarehouseGripperControllerIntegrationTest
```

### Run Single Test
```bash
mvn test -Dtest=WarehouseGripperControllerIntegrationTest#testCompletePickAndPlaceCycle
```

## Test Scenarios

| Test | Purpose | Expected Result |
|------|---------|-----------------|
| `testCircuitBreakerOpensAfterFailures` | Verify circuit opens after 5 failures | HTTP 503 on 6th call |
| `testPickFromEmptyLocationFails` | Validate error handling for business errors | HTTP 400 with error code |
| `testCompletePickAndPlaceCycle` | End-to-end pick & place workflow | All operations succeed in sequence |
| `testConcurrentMoveOperations` | Load test with 10 concurrent requests | All requests complete successfully |
| `testCircuitBreakerRecoveryAfterWaitDuration` | Verify circuit recovery in HALF-OPEN state | Circuit closes after successful probe |

## Pre-Deployment Testing

Before deploying to customer pilot:

1. **Run full test suite**: `mvn clean test`
2. **Verify 100% pass rate**: All tests must pass
3. **Review logs**: No ERROR or WARN messages during test execution
4. **Check coverage**: Minimum 80% code coverage on controller and service layers

## Testing Against Real WCF Service

To test against the actual .NET WCF service (not mocks):

1. Start the .NET service: `cd robotGripperBackend.Skeleton && dotnet run`
2. Verify WSDL is accessible: `curl http://localhost:8080/WarehouseGripperService.svc?wsdl`
3. Uncomment WSDL-generated code in `WcfGripperServiceClient.java`
4. Remove `@MockBean` from tests (use real service)
5. Run tests: `mvn test`

**Note**: Real WCF testing requires coordination with .NET team.
```

---

### 📋 Deliverables for Task #2

- [ ] Fix disabled test `testPickFromEmptyLocationFails()`
- [ ] Add `testCompletePickAndPlaceCycle()` end-to-end test
- [ ] Add `testConcurrentMoveOperations()` load test
- [ ] Add `testCircuitBreakerRecoveryAfterWaitDuration()` recovery test
- [ ] Create `TEST_PLAN.md` documentation
- [ ] All tests pass: `mvn test` shows 100% success

**Success Criteria**:
- Minimum 5 integration tests covering happy path, error cases, concurrency, and circuit breaker
- All tests green on local machine
- Test execution completes in < 10 seconds

---

## 🔌 Code Task #3: WSDL Integration Readiness & Error Handling

### The Problem

**Current state**: All WCF service calls return **mock data** because the WSDL-generated Java stubs are commented out.

**What needs to happen**:
1. .NET WCF service must be running on `http://localhost:8080`
2. Run `mvn clean compile` to generate Java stubs from WSDL
3. Uncomment code blocks in `WcfGripperServiceClient.java`
4. Handle WCF-specific exceptions (`ServiceFault_Exception`)

**The challenge**: WCF faults are different from Java exceptions. We need robust error handling.

---

### Implementation Guide

#### Step 1: Prepare WSDL Generation

Currently, the JAX-WS plugin configuration in [pom.xml:128-148](JavaSpringBootClient/pom.xml#L128-L148) is commented out.

**Before uncommenting**, verify:

```bash
# Check if .NET service is running
curl http://localhost:8080/WarehouseGripperService.svc?wsdl

# Expected: XML WSDL document (not 404)
```

If you get the WSDL, **uncomment lines 133-147** in `pom.xml`:

```xml
<plugin>
    <groupId>com.sun.xml.ws</groupId>
    <artifactId>jaxws-maven-plugin</artifactId>
    <version>4.0.1</version>
    <executions>
        <execution>
            <goals>
                <goal>wsimport</goal>
            </goals>
            <configuration>
                <wsdlUrls>
                    <wsdlUrl>http://localhost:8080/WarehouseGripperService.svc?wsdl</wsdlUrl>
                </wsdlUrls>
                <packageName>com.robot.warehouse.wcf.generated</packageName>
                <sourceDestDir>${project.build.directory}/generated-sources/jaxws</sourceDestDir>
                <keep>true</keep>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Then generate stubs:

```bash
mvn clean compile
```

**Output location**: `target/generated-sources/jaxws/com/robot/warehouse/wcf/generated/`

**Verify**:
```bash
ls target/generated-sources/jaxws/com/robot/warehouse/wcf/generated/

# Expected files:
# - IWarehouseGripperService.java (interface)
# - WarehouseGripperServiceService.java (service class)
# - GripperStatusDto.java
# - OperationResultDto.java
# - ServiceFault_Exception.java
# - ... (other DTOs)
```

#### Step 2: Create Enhanced Exception Handling

Create `JavaSpringBootClient/src/main/java/com/robot/warehouse/exception/WcfServiceException.java`:

```java
package com.robot.warehouse.exception;

import lombok.Getter;

/**
 * Custom exception for WCF service communication errors
 */
@Getter
public class WcfServiceException extends RuntimeException {

    private final String errorCode;
    private final String operation;
    private final boolean isRecoverable;

    public WcfServiceException(String operation, String errorCode, String message, boolean isRecoverable) {
        super(String.format("WCF operation '%s' failed: %s (code: %s)", operation, message, errorCode));
        this.operation = operation;
        this.errorCode = errorCode;
        this.isRecoverable = isRecoverable;
    }

    public WcfServiceException(String operation, String message, Throwable cause) {
        super(String.format("WCF operation '%s' failed: %s", operation, message), cause);
        this.operation = operation;
        this.errorCode = "COMMUNICATION_ERROR";
        this.isRecoverable = true; // Communication errors are usually temporary
    }
}
```

Update [GlobalExceptionHandler.java](JavaSpringBootClient/src/main/java/com/robot/warehouse/handler/GlobalExceptionHandler.java):

```java
package com.robot.warehouse.handler;

import com.robot.warehouse.dto.OperationResponse;
import com.robot.warehouse.exception.OperationResponseException;
import com.robot.warehouse.exception.WcfServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OperationResponseException.class)
    public ResponseEntity<OperationResponse> handleOperationResponseException(OperationResponseException ex) {
        log.warn("Operation failed: {} - {}", ex.getErrorCode(), ex.getMessage());

        OperationResponse response = new OperationResponse();
        response.setSuccess(false);
        response.setErrorCode(ex.getErrorCode());
        response.setMessage(ex.getMessage());
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(WcfServiceException.class)
    public ResponseEntity<OperationResponse> handleWcfServiceException(WcfServiceException ex) {
        log.error("WCF service error: operation={}, code={}, recoverable={}",
            ex.getOperation(), ex.getErrorCode(), ex.isRecoverable(), ex);

        OperationResponse response = new OperationResponse();
        response.setSuccess(false);
        response.setErrorCode(ex.getErrorCode());
        response.setMessage(ex.getMessage());
        response.setTimestamp(LocalDateTime.now());

        // Return 503 if recoverable (temporary issue), 500 if not
        HttpStatus status = ex.isRecoverable()
            ? HttpStatus.SERVICE_UNAVAILABLE
            : HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OperationResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        OperationResponse response = new OperationResponse();
        response.setSuccess(false);
        response.setErrorCode("INTERNAL_ERROR");
        response.setMessage("An unexpected error occurred. Please contact support.");
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

#### Step 3: Update WcfGripperServiceClient with Real WCF Calls

Once WSDL is generated, update [WcfGripperServiceClient.java](JavaSpringBootClient/src/main/java/com/robot/warehouse/service/WcfGripperServiceClient.java).

**Uncomment lines 8-12** (imports):

```java
import com.robot.warehouse.wcf.generated.*;
import jakarta.xml.ws.BindingProvider;
import javax.xml.namespace.QName;
import java.net.URL;
import java.util.stream.Collectors;
```

**Uncomment and update `getServicePort()` method** (lines 38-60):

```java
private IWarehouseGripperService getServicePort() {
    try {
        URL wsdlUrl = new URL(config.getServiceUrl() + "?wsdl");
        QName serviceName = new QName(
            "http://robot.warehouse.gripper/2024",
            "WarehouseGripperService"
        );

        WarehouseGripperServiceService service =
            new WarehouseGripperServiceService(wsdlUrl, serviceName);
        IWarehouseGripperService port = service.getBasicHttpBindingIWarehouseGripperService();

        config.configureBinding((BindingProvider) port);
        return port;
    } catch (Exception e) {
        log.error("Failed to create WCF service port", e);
        throw new WcfServiceException("getServicePort", "Failed to connect to WCF service", e);
    }
}
```

**Update `getGripperStatus()` method** with better error handling:

```java
public GripperStatusResponse getGripperStatus(int gripperId) {
    log.info("Getting status for Gripper {}", gripperId);

    Timer.Sample sample = Timer.start(meterRegistry);

    try {
        IWarehouseGripperService port = getServicePort();
        GripperStatusDto wcfResult = port.getGripperStatus(gripperId);

        sample.stop(Timer.builder("wcf.soap.duration")
            .tag("operation", "getGripperStatus")
            .tag("status", "success")
            .register(meterRegistry));

        meterRegistry.counter("wcf.soap.calls.total",
            "operation", "getGripperStatus",
            "status", "success").increment();

        return mapToGripperStatusResponse(wcfResult);

    } catch (ServiceFault_Exception e) {
        sample.stop(Timer.builder("wcf.soap.duration")
            .tag("operation", "getGripperStatus")
            .tag("status", "failure")
            .register(meterRegistry));

        meterRegistry.counter("wcf.soap.calls.total",
            "operation", "getGripperStatus",
            "status", "failure").increment();

        log.error("WCF Service Fault: {}", e.getFaultInfo().getErrorMessage());

        // Determine if error is recoverable
        boolean recoverable = isRecoverableError(e.getFaultInfo().getErrorCode());

        throw new WcfServiceException(
            "getGripperStatus",
            e.getFaultInfo().getErrorCode(),
            e.getFaultInfo().getErrorMessage(),
            recoverable
        );

    } catch (Exception e) {
        sample.stop(Timer.builder("wcf.soap.duration")
            .tag("operation", "getGripperStatus")
            .tag("status", "failure")
            .register(meterRegistry));

        meterRegistry.counter("wcf.soap.calls.total",
            "operation", "getGripperStatus",
            "status", "failure").increment();

        log.error("Failed to get gripper status", e);
        throw new WcfServiceException("getGripperStatus", "Communication error", e);
    }
}

/**
 * Determine if a WCF error code indicates a recoverable error
 */
private boolean isRecoverableError(String errorCode) {
    return errorCode != null && (
        errorCode.equals("TIMEOUT") ||
        errorCode.equals("SERVICE_UNAVAILABLE") ||
        errorCode.equals("NETWORK_ERROR") ||
        errorCode.equals("GRIPPER_BUSY")
    );
}
```

**Uncomment mapping methods** (lines 300-343):

```java
private GripperStatusResponse mapToGripperStatusResponse(GripperStatusDto dto) {
    return GripperStatusResponse.builder()
            .gripperId(dto.getGripperId())
            .state(dto.getState())
            .positionX(dto.getPositionX())
            .positionY(dto.getPositionY())
            .positionZ(dto.getPositionZ())
            .hasLoadCarrier(dto.isHasLoadCarrier())
            .currentLoadCarrierId(dto.getCurrentLoadCarrierId())
            .isEnabled(dto.isIsEnabled())
            .hasError(dto.isHasError())
            .errorMessage(dto.getErrorMessage())
            .currentLoad(dto.getCurrentLoad())
            .gripperWidth(dto.getGripperWidth())
            .lastUpdated(dto.getLastUpdated().toGregorianCalendar()
                .toZonedDateTime().toLocalDateTime())
            .build();
}

private OperationResponse mapToOperationResponse(OperationResultDto dto) {
    return OperationResponse.builder()
            .success(dto.isSuccess())
            .operationId(dto.getOperationId())
            .message(dto.getMessage())
            .errorCode(dto.getErrorCode())
            .timestamp(dto.getTimestamp().toGregorianCalendar()
                .toZonedDateTime().toLocalDateTime())
            .build();
}

private LocationResponse mapToLocationResponse(LocationDto dto) {
    return LocationResponse.builder()
            .locationId(dto.getLocationId())
            .locationCode(dto.getLocationCode())
            .locationType(dto.getLocationType())
            .isOccupied(dto.isIsOccupied())
            .loadCarrierId(dto.getLoadCarrierId())
            .positionX(dto.getPositionX())
            .positionY(dto.getPositionY())
            .positionZ(dto.getPositionZ())
            .build();
}
```

**Repeat this pattern** for all other methods:
- `getAllGrippers()`
- `moveGripper()`
- `pickLoadCarrier()`
- `placeLoadCarrier()`
- `createOperation()`
- `getAvailableLocations()`

#### Step 4: Create WSDL Integration Checklist

Create `JavaSpringBootClient/WSDL_INTEGRATION.md`:

```markdown
# WSDL Integration Checklist

## Prerequisites

- [ ] .NET WCF service is running on `http://localhost:8080`
- [ ] WSDL is accessible: `curl http://localhost:8080/WarehouseGripperService.svc?wsdl` returns XML

## Integration Steps

1. **Generate Java Stubs**
   - [ ] Uncomment JAX-WS plugin in `pom.xml` (lines 133-147)
   - [ ] Run: `mvn clean compile`
   - [ ] Verify generated files in `target/generated-sources/jaxws/com/robot/warehouse/wcf/generated/`

2. **Update Service Client**
   - [ ] Uncomment imports in `WcfGripperServiceClient.java` (lines 8-12)
   - [ ] Uncomment `getServicePort()` method (lines 38-60)
   - [ ] Uncomment all WCF service calls (replace mock data blocks)
   - [ ] Uncomment mapping methods (lines 300-343)

3. **Test Integration**
   - [ ] Start .NET service: `cd robotGripperBackend.Skeleton && dotnet run`
   - [ ] Start Java app: `mvn spring-boot:run`
   - [ ] Test health check: `curl http://localhost:9999/api/warehouse/health`
   - [ ] Test get grippers: `curl http://localhost:9999/api/warehouse/grippers`
   - [ ] Verify logs show "SOAP call" messages (not "MOCK")

4. **Verify Error Handling**
   - [ ] Stop .NET service
   - [ ] Call API endpoint → should return HTTP 503 with `SERVICE_UNAVAILABLE` error
   - [ ] Check circuit breaker opens after 5 failures
   - [ ] Restart .NET service
   - [ ] Verify circuit breaker recovers

## Troubleshooting

### WSDL Generation Fails

**Error**: `Failed to parse WSDL`

**Solution**: Check .NET service logs. WSDL might have validation errors.

### ClassNotFoundException at Runtime

**Error**: `java.lang.ClassNotFoundException: com.robot.warehouse.wcf.generated.IWarehouseGripperService`

**Solution**:
```bash
mvn clean compile
# Verify target/generated-sources/jaxws exists
ls -la target/generated-sources/jaxws/com/robot/warehouse/wcf/generated/
```

### Connection Refused

**Error**: `java.net.ConnectException: Connection refused`

**Solution**: Verify .NET service is running:
```bash
curl http://localhost:8080/WarehouseGripperService.svc?wsdl
```

### SOAP Fault Errors

**Error**: `ServiceFault_Exception: Gripper not found`

**Solution**: Check gripper ID. .NET service may have different gripper configuration.

## Production Deployment Notes

- **DO NOT** generate WSDL stubs at build time in production
- **DO** commit generated sources to Git (in `src/main/java-generated/`)
- **DO** version-pin WSDL contract with .NET team
- **DO** have rollback plan if WSDL changes break compatibility
```

---

### 📋 Deliverables for Task #3

- [ ] WSDL Java stubs generated successfully
- [ ] All mock data blocks replaced with real WCF calls
- [ ] `WcfServiceException` custom exception implemented
- [ ] `GlobalExceptionHandler` updated with WCF error handling
- [ ] `isRecoverableError()` method classifies errors correctly
- [ ] `WSDL_INTEGRATION.md` documentation created
- [ ] Tested against running .NET WCF service
- [ ] Verified error handling (stop .NET service, confirm circuit breaker opens)

**Success Criteria**:
- Application successfully calls .NET WCF service
- No "MOCK" messages in logs
- Circuit breaker protects against WCF service failures
- Error responses distinguish between business errors (400) and service errors (503)

---

## 🚀 Week 1 Sprint Goals

By end of Week 1, you must complete:

| Day | Focus | Deliverable |
|-----|-------|-------------|
| **Mon-Tue** | Task #1: Metrics | Prometheus endpoint operational |
| **Wed** | Task #2: Tests | 5+ integration tests passing |
| **Thu-Fri** | Task #3: WSDL | Real WCF calls working |

### Definition of Done

**Task #1 (Metrics)**:
- ✅ `curl http://localhost:9999/actuator/prometheus \| grep wcf` returns metrics
- ✅ All WCF methods instrumented with counters + timers
- ✅ Correlation ID appears in every log line
- ✅ `METRICS.md` documents all custom metrics

**Task #2 (Tests)**:
- ✅ `mvn test` shows 5+ tests passing
- ✅ Disabled test is fixed and re-enabled
- ✅ `testCompletePickAndPlaceCycle` simulates end-to-end workflow
- ✅ `TEST_PLAN.md` documents how to run tests

**Task #3 (WSDL)**:
- ✅ WSDL Java stubs generated in `target/generated-sources/jaxws/`
- ✅ Application makes real SOAP calls to .NET service (no mocks)
- ✅ Circuit breaker opens when .NET service is stopped
- ✅ `WSDL_INTEGRATION.md` documents integration process

---

## 🎯 Week 2-3 Preview (After Completion)

Once you complete these three tasks, we'll move to:

### Week 2: Production Hardening
- **Docker packaging**: Multi-stage Dockerfile with JRE 17
- **Kubernetes manifests**: Deployment, Service, ConfigMap, Secret
- **Helm chart**: For customer-specific configurations
- **Health probes**: Liveness, readiness, startup probes
- **Resource limits**: CPU/memory requests and limits

### Week 3: Customer Pilot Preparation
- **Runbooks**: Incident response procedures
- **Monitoring playbooks**: Grafana alerts, PagerDuty integration
- **Deployment guide**: Step-by-step for customer IT team
- **Smoke tests**: Quick validation scripts for post-deployment
- **Rollback procedures**: How to revert to previous version

### Week 4: On-Site Support
- **Travel to customer site** (if required)
- **Deployment assistance**
- **Live monitoring during pilot**
- **Issue triage and hotfixes**

---

## 📚 Additional Resources

### Key Documentation Files

| File | Purpose |
|------|---------|
| [ONBOARDING_GUIDE.md](ONBOARDING_GUIDE.md) | Process & architecture overview |
| [Project_Instructions.md](Project_Instructions.md) | GDPR compliance rules |
| [pom.xml](JavaSpringBootClient/pom.xml) | Maven dependencies |
| [application.properties](JavaSpringBootClient/src/main/resources/application.properties) | Configuration reference |

### Useful Commands

```bash
# Run application with debug logging
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.robot.warehouse=DEBUG"

# Run tests with detailed output
mvn test -Dtest=WarehouseGripperControllerIntegrationTest -X

# Generate WSDL stubs
mvn clean compile

# Check code coverage
mvn jacoco:report
# Open: target/site/jacoco/index.html

# Package as JAR
mvn clean package
java -jar target/gripper-client-1.0.0.jar

# View Prometheus metrics
curl http://localhost:9999/actuator/prometheus | grep -E "wcf|http"

# Check circuit breaker state
curl http://localhost:9999/actuator/health | jq .components.circuitBreakers
```

### Architecture Diagrams

```
REST API → Circuit Breaker → WCF Client → SOAP → .NET Service → Hardware
    ↓            ↓               ↓
 Swagger   Resilience4j    Micrometer
              ↓                 ↓
         Fallback         Prometheus
```

---

## 🆘 Getting Help

### Technical Issues

| Problem Area | Solution |
|--------------|----------|
| **Maven build errors** | `mvn clean compile -X` for detailed logs |
| **WSDL generation fails** | Verify .NET service is running: `curl http://localhost:8080/...?wsdl` |
| **Tests failing** | Check logs in `target/surefire-reports/` |
| **Circuit breaker not working** | Verify Resilience4j config in `application.properties` |
| **Metrics not appearing** | Check Actuator is enabled: `curl http://localhost:9999/actuator` |

### Escalation Path

1. **Codebase questions**: Review [ONBOARDING_GUIDE.md](ONBOARDING_GUIDE.md) + this document
2. **GDPR/Security**: Check [Project_Instructions.md](Project_Instructions.md) compliance rules
3. **WCF service issues**: Contact .NET team (they own `robotGripperBackend.Skeleton/`)
4. **Stuck on task**: Ask senior Java developer (include file:line references)
5. **Production concerns**: Escalate to project lead

### Daily Standup

**Time**: 9:00 AM CET
**Format**:
- What I completed yesterday
- What I'm working on today
- Any blockers

**Bring**:
- Specific code questions with file:line numbers
- Test failures with error messages
- Questions about customer requirements

---

## 💪 Let's Ship This!

You're joining at the most exciting phase: **turning working code into production-ready code**.

The architecture is solid. The circuit breaker is implemented. Now we need:
- **Visibility** (metrics)
- **Confidence** (tests)
- **Integration** (real WSDL calls)

Three focused tasks. One week. Customer pilots in 2-3 weeks.

**Welcome to the final stretch.** 🚀
