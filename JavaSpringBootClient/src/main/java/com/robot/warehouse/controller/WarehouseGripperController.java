package com.robot.warehouse.controller;

import com.robot.warehouse.dto.*;
import com.robot.warehouse.exception.OperationResponseException;
import com.robot.warehouse.service.WcfGripperServiceClient;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REST API Controller for Warehouse Gripper Operations
 * This controller provides a REST interface that internally communicates with
 * the .NET WCF Service
 */
@Slf4j
@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@Tag(name = "Warehouse Gripper API", description = "Control warehouse grippers via WCF service")
public class WarehouseGripperController {

    private final WcfGripperServiceClient wcfClient;

    /**
     * Fallback for operations returning OperationResponse (pick/place/move/create)
     * @throws Exception 
     */
    @SuppressWarnings("unused")
    private ResponseEntity<OperationResponse> operationResponseFallback(Throwable ex, String context) throws Exception {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker is OPEN - {}", context);
            OperationResponse error = new OperationResponse();
            error.setErrorCode("SERVICE_UNAVAILABLE");
            error.setMessage("Service is unavailable. Please try later.");
            error.setSuccess(false);
            error.setTimestamp(java.time.LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        } else {
            log.warn("Circuit Breaker fallback - {}: {}", context, ex.getMessage());
            throw new Exception(ex);
        }
    }

    // --- Specific Fallback Methods (delegate to generic handler) ---

    @SuppressWarnings("unused")
    private ResponseEntity<OperationResponse> pickPlaceOperationFallback(int gripperId, int locationId, Throwable ex) {
        return operationResponseFallback(ex, String.format("Gripper %d at Location %d", gripperId, locationId));
    }

    @SuppressWarnings("unused")
    private ResponseEntity<OperationResponse> moveGripperFallback(int id, double x, double y, double z, Throwable ex) {
        return operationResponseFallback(ex, String.format("Move Gripper %d to (%.2f, %.2f, %.2f)", id, x, y, z));
    }

    @SuppressWarnings("unused")
    private ResponseEntity<OperationResponse> createOperationFallback(OperationRequest request, Throwable ex) {
        return operationResponseFallback(ex, String.format("Create Operation: Type=%s, Gripper=%d",
            request.getOperationType(), request.getGripperId()));
    }

    @SuppressWarnings("unused")
    private ResponseEntity<GripperStatusResponse> getByIdFallback(int id, Throwable ex) {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker is OPEN - Get Gripper {}", id);
        } else {
            log.warn("Circuit Breaker fallback - Get Gripper {}: {}", id, ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @SuppressWarnings("unused")
    private ResponseEntity<List<GripperStatusResponse>> getAllGrippersFallback(Throwable ex) {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker is OPEN - Get All Grippers");
        } else {
            log.warn("Circuit Breaker fallback - Get All Grippers: {}", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(List.of());
    }

    @SuppressWarnings("unused")
    private ResponseEntity<List<LocationResponse>> getAvailableLocationsFallback(Throwable ex) {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker is OPEN - Get Available Locations");
        } else {
            log.warn("Circuit Breaker fallback - Get Available Locations: {}", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(List.of());
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if WCF service is healthy")
    public ResponseEntity<Map<String, Boolean>> healthCheck() {
        log.info("Health check requested");
        boolean healthy = wcfClient.isServiceHealthy();
        return ResponseEntity.ok(Map.of("healthy", healthy));
    }

    @CircuitBreaker(name="get-all-grippers", fallbackMethod = "getAllGrippersFallback")
    @GetMapping("/grippers")
    @Operation(summary = "Get all grippers", description = "Retrieve status of all grippers")
    public ResponseEntity<List<GripperStatusResponse>> getAllGrippers() {
        log.info("GET /api/warehouse/grippers");
        List<GripperStatusResponse> grippers = wcfClient.getAllGrippers();
        return ResponseEntity.ok(grippers);
    }

    @CircuitBreaker(name="get-gripper-by-id", fallbackMethod = "getByIdFallback")
    @GetMapping("/grippers/{id}")
    @Operation(summary = "Get gripper by ID", description = "Retrieve status of a specific gripper")
    public ResponseEntity<GripperStatusResponse> getGripperById(@PathVariable int id) {
        log.info("GET /api/warehouse/grippers/{}", id);
        GripperStatusResponse gripper = wcfClient.getGripperStatus(id);
        return ResponseEntity.ok(gripper);
    }

    @CircuitBreaker(name="move-gripper", fallbackMethod = "moveGripperFallback")
    @PostMapping("/grippers/{id}/move")
    @Operation(summary = "Move gripper", description = "Move gripper to specified position")
    public ResponseEntity<OperationResponse> moveGripper(
            @PathVariable int id,
            @RequestParam double x,
            @RequestParam double y,
            @RequestParam double z) {
        log.info("POST /api/warehouse/grippers/{}/move - Position: ({}, {}, {})", id, x, y, z);
        OperationResponse result = wcfClient.moveGripper(id, x, y, z);
        if (!result.isSuccess()) {
            throw new OperationResponseException(result.getErrorCode(), result.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @CircuitBreaker(name="pick-gripper", fallbackMethod = "pickPlaceOperationFallback")
    @PostMapping("/grippers/{id}/pick")
    @Operation(summary = "Pick load carrier", description = "Command gripper to pick load carrier from location")
    public ResponseEntity<OperationResponse> pickLoadCarrier(
            @PathVariable int id,
            @RequestParam int locationId) throws Exception {
        log.info("POST /api/warehouse/grippers/{}/pick - Location: {}", id, locationId);
        OperationResponse result = wcfClient.pickLoadCarrier(id, locationId);
        if (!result.isSuccess()) {
            throw new OperationResponseException(result.getErrorCode(), result.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @CircuitBreaker(name="place-gripper", fallbackMethod = "pickPlaceOperationFallback")
    @PostMapping("/grippers/{id}/place")
    @Operation(summary = "Place load carrier", description = "Command gripper to place load carrier at location")
    public ResponseEntity<OperationResponse> placeLoadCarrier(
            @PathVariable int id,
            @RequestParam int locationId) throws Exception {
        log.info("POST /api/warehouse/grippers/{}/place - Location: {}", id, locationId);
        OperationResponse result = wcfClient.placeLoadCarrier(id, locationId);
        if (!result.isSuccess()) {
            throw new OperationResponseException(result.getErrorCode(), result.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @CircuitBreaker(name="create-operation", fallbackMethod = "createOperationFallback")
    @PostMapping("/operations")
    @Operation(summary = "Create operation", description = "Create a new warehouse operation (queued)")
    public ResponseEntity<OperationResponse> createOperation(
            @RequestBody OperationRequest request) throws Exception {
        log.info("POST /api/warehouse/operations - Type: {}, Gripper: {}",
                request.getOperationType(), request.getGripperId());
        OperationResponse result = wcfClient.createOperation(request);
        if (!result.isSuccess()) {
            throw new OperationResponseException(result.getErrorCode(), result.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @CircuitBreaker(name="get-available-locations", fallbackMethod = "getAvailableLocationsFallback")
    @GetMapping("/locations/available")
    @Operation(summary = "Get available locations", description = "Retrieve all unoccupied storage locations")
    public ResponseEntity<List<LocationResponse>> getAvailableLocations() {
        log.info("GET /api/warehouse/locations/available");
        List<LocationResponse> locations = wcfClient.getAvailableLocations();
        return ResponseEntity.ok(locations);
    }

}
