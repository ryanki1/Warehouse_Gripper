package com.robot.warehouse.controller;

import com.robot.warehouse.dto.*;
import com.robot.warehouse.exception.OperationResponseException;
import com.robot.warehouse.service.WcfGripperServiceClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if WCF service is healthy")
    public ResponseEntity<Map<String, Boolean>> healthCheck() {
        log.info("Health check requested");
        boolean healthy = wcfClient.isServiceHealthy();
        return ResponseEntity.ok(Map.of("healthy", healthy));
    }

    @GetMapping("/grippers")
    @Operation(summary = "Get all grippers", description = "Retrieve status of all grippers")
    public ResponseEntity<List<GripperStatusResponse>> getAllGrippers() {
        log.info("GET /api/warehouse/grippers");
        List<GripperStatusResponse> grippers = wcfClient.getAllGrippers();
        return ResponseEntity.ok(grippers);
    }

    @GetMapping("/grippers/{id}")
    @Operation(summary = "Get gripper by ID", description = "Retrieve status of a specific gripper")
    public ResponseEntity<GripperStatusResponse> getGripperById(@PathVariable int id) throws Exception {
        log.info("GET /api/warehouse/grippers/{}", id);
        GripperStatusResponse gripper = wcfClient.getGripperStatus(id);
        return ResponseEntity.ok(gripper);
    }

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

    @PostMapping("/operations")
    @Operation(summary = "Create operation", description = "Create a new warehouse operation (queued)")
    public ResponseEntity<OperationResponse> createOperation(
        @RequestBody OperationRequest request) throws Exception {
            try {
                log.info("POST /api/warehouse/operations - Type: {}, Gripper: {}",
                        request.getOperationType(), request.getGripperId());
                OperationResponse result = wcfClient.createOperation(request);
                return ResponseEntity.ok(result);
            } catch (Exception ex) {
                throw ex;
            }
    }

    @GetMapping("/locations/available")
    @Operation(summary = "Get available locations", description = "Retrieve all unoccupied storage locations")
    public ResponseEntity<List<LocationResponse>> getAvailableLocations() {
        log.info("GET /api/warehouse/locations/available");
        List<LocationResponse> locations = wcfClient.getAvailableLocations();
        return ResponseEntity.ok(locations);
    }

}
