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

    @SuppressWarnings("unused")
    private ResponseEntity<OperationResponse> pickFallback(int id, int locationId, Throwable ex) {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker 'pick-gripper' is OPEN - rejecting call for Gripper {} at Location {}", id, locationId);
            OperationResponse error = new OperationResponse();
            error.setErrorCode("SERVICE_UNAVAILABLE");
            error.setMessage("Pick Service is unavailable. Please try later.");
            error.setSuccess(false);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        } else {
            log.warn("Circuit Breaker 'pick-gripper' fallback triggered for Gripper {} at Location {}: {}", 
                    id, locationId, ex.getMessage());
            OperationResponse error = new OperationResponse();
            error.setErrorCode("LOAD_CARRIER");
            error.setMessage("Location is empty");
            error.setSuccess(false);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if WCF service is healthy")
    public ResponseEntity<Boolean> healthCheck() {
        log.info("Health check requested");
        boolean healthy = wcfClient.isServiceHealthy();
        return ResponseEntity.ok(healthy);
    }

    @CircuitBreaker(name="get-all-grippers")
    @GetMapping("/grippers")
    @Operation(summary = "Get all grippers", description = "Retrieve status of all grippers")
    public ResponseEntity<List<GripperStatusResponse>> getAllGrippers() {
        log.info("GET /api/warehouse/grippers");
        List<GripperStatusResponse> grippers = wcfClient.getAllGrippers();
        return ResponseEntity.ok(grippers);
    }

    @CircuitBreaker(name="get-gripper-by-id")
    @GetMapping("/grippers/{id}")
    @Operation(summary = "Get gripper by ID", description = "Retrieve status of a specific gripper")
    public ResponseEntity<GripperStatusResponse> getGripperById(@PathVariable int id) {
        log.info("GET /api/warehouse/grippers/{}", id);
        GripperStatusResponse gripper = wcfClient.getGripperStatus(id);
        return ResponseEntity.ok(gripper);
    }

    @CircuitBreaker(name="move-gripper")
    @PostMapping("/grippers/{id}/move")
    @Operation(summary = "Move gripper", description = "Move gripper to specified position")
    public ResponseEntity<OperationResponse> moveGripper(
            @PathVariable int id,
            @RequestParam double x,
            @RequestParam double y,
            @RequestParam double z) {
        log.info("POST /api/warehouse/grippers/{}/move - Position: ({}, {}, {})", id, x, y, z);
        OperationResponse result = wcfClient.moveGripper(id, x, y, z);
        return ResponseEntity.ok(result);
    }

    @CircuitBreaker(name="pick-gripper", fallbackMethod = "pickFallback")
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

    @CircuitBreaker(name="place-gripper")
    @PostMapping("/grippers/{id}/place")
    @Operation(summary = "Place load carrier", description = "Command gripper to place load carrier at location")
    public ResponseEntity<OperationResponse> placeLoadCarrier(
            @PathVariable int id,
            @RequestParam int locationId) {
        log.info("POST /api/warehouse/grippers/{}/place - Location: {}", id, locationId);
        OperationResponse result = wcfClient.placeLoadCarrier(id, locationId);
        return ResponseEntity.ok(result);
    }

    @CircuitBreaker(name="create-operation")
    @PostMapping("/operations")
    @Operation(summary = "Create operation", description = "Create a new warehouse operation (queued)")
    public ResponseEntity<OperationResponse> createOperation(
            @RequestBody OperationRequest request) {
        log.info("POST /api/warehouse/operations - Type: {}, Gripper: {}",
                request.getOperationType(), request.getGripperId());
        OperationResponse result = wcfClient.createOperation(request);
        return ResponseEntity.ok(result);
    }

    @CircuitBreaker(name="get-available-locations")
    @GetMapping("/locations/available")
    @Operation(summary = "Get available locations", description = "Retrieve all unoccupied storage locations")
    public ResponseEntity<List<LocationResponse>> getAvailableLocations() {
        log.info("GET /api/warehouse/locations/available");
        List<LocationResponse> locations = wcfClient.getAvailableLocations();
        return ResponseEntity.ok(locations);
    }
}
