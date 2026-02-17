package com.robot.warehouse.service;

import com.robot.warehouse.config.WcfServiceConfig;
import com.robot.warehouse.dto.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.slf4j.MDC;

import com.robot.warehouse.wcf.generated.*;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.net.URL;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for communicating with .NET WCF Warehouse Gripper Service
 *
 * IMPORTANT: After first build with WCF service running, the WSDL will generate
 * Java stub classes in: target/generated-sources/jaxws/com/robot/warehouse/wcf/generated/
 *
 * Uncomment the code sections marked with "// GENERATED CODE" after running:
 * mvn clean compile (with WCF service running on http://localhost:8080)
 */
@Slf4j
@Service
@SuppressWarnings("unused") // config will be used after WSDL generation
public class WcfGripperServiceClient {

    private final WcfServiceConfig config;
    private final MeterRegistry registry;

    // Metrics fields - initialized once in constructor for performance
    private final Timer getGripperStatusSuccessTimer;
    private final Timer getGripperStatusFailureTimer;
    private final Counter getGripperStatusSuccessCounter;
    private final Counter getGripperStatusFailureCounter;

    private final Timer getAllGrippersSuccessTimer;
    private final Timer getAllGrippersFailureTimer;
    private final Counter getAllGrippersSuccessCounter;
    private final Counter getAllGrippersFailureCounter;

    private final Timer moveGripperSuccessTimer;
    private final Timer moveGripperFailureTimer;
    private final Counter moveGripperSuccessCounter;
    private final Counter moveGripperFailureCounter;

    private final Timer pickLoadCarrierSuccessTimer;
    private final Timer pickLoadCarrierFailureTimer;
    private final Counter pickLoadCarrierSuccessCounter;
    private final Counter pickLoadCarrierFailureCounter;

    private final Timer placeLoadCarrierSuccessTimer;
    private final Timer placeLoadCarrierFailureTimer;
    private final Counter placeLoadCarrierSuccessCounter;
    private final Counter placeLoadCarrierFailureCounter;

    private final Timer createOperationSuccessTimer;
    private final Timer createOperationFailureTimer;
    private final Counter createOperationSuccessCounter;
    private final Counter createOperationFailureCounter;

    private final Timer getAvailableLocationsSuccessTimer;
    private final Timer getAvailableLocationsFailureTimer;
    private final Counter getAvailableLocationsSuccessCounter;
    private final Counter getAvailableLocationsFailureCounter;

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

        this.getAllGrippersSuccessTimer = registry.timer("wcf.soap.duration",
                "operation", "getAllGrippers", "status", "success");
        this.getAllGrippersFailureTimer = registry.timer("wcf.soap.duration",
                "operation", "getAllGrippers", "status", "failure");
        this.getAllGrippersSuccessCounter = registry.counter("wcf.soap.calls.total",
                "operation", "getAllGrippers", "status", "success");
        this.getAllGrippersFailureCounter = registry.counter("wcf.soap.calls.total",
                "operation", "getAllGrippers", "status", "failure");

        this.moveGripperSuccessTimer = registry.timer("wcf.soap.duration",
                "operation", "moveGripper", "status", "success");
        this.moveGripperFailureTimer = registry.timer("wcf.soap.duration",
                "operation", "moveGripper", "status", "failure");
        this.moveGripperSuccessCounter = registry.counter("wcf.soap.calls.total",
                "operation", "moveGripper", "status", "success");
        this.moveGripperFailureCounter = registry.counter("wcf.soap.calls.total",
                "operation", "moveGripper", "status", "failure");

        this.pickLoadCarrierSuccessTimer = registry.timer("wcf.soap.duration",
                "operation", "pickLoadCarrier", "status", "success");
        this.pickLoadCarrierFailureTimer = registry.timer("wcf.soap.duration",
                "operation", "pickLoadCarrier", "status", "failure");
        this.pickLoadCarrierSuccessCounter = registry.counter("wcf.soap.calls.total",
                "operation", "pickLoadCarrier", "status", "success");
        this.pickLoadCarrierFailureCounter = registry.counter("wcf.soap.calls.total",
                "operation", "pickLoadCarrier", "status", "failure");

        this.placeLoadCarrierSuccessTimer = registry.timer("wcf.soap.duration",
                "operation", "placeLoadCarrier", "status", "success");
        this.placeLoadCarrierFailureTimer = registry.timer("wcf.soap.duration",
                "operation", "placeLoadCarrier", "status", "failure");
        this.placeLoadCarrierSuccessCounter = registry.counter("wcf.soap.calls.total",
                "operation", "placeLoadCarrier", "status", "success");
        this.placeLoadCarrierFailureCounter = registry.counter("wcf.soap.calls.total",
                "operation", "placeLoadCarrier", "status", "failure");

        this.createOperationSuccessTimer = registry.timer("wcf.soap.duration",
                "operation", "createOperation", "status", "success");
        this.createOperationFailureTimer = registry.timer("wcf.soap.duration",
                "operation", "createOperation", "status", "failure");
        this.createOperationSuccessCounter = registry.counter("wcf.soap.calls.total",
                "operation", "createOperation", "status", "success");
        this.createOperationFailureCounter = registry.counter("wcf.soap.calls.total",
                "operation", "createOperation", "status", "failure");

        this.getAvailableLocationsSuccessTimer = registry.timer("wcf.soap.duration",
                "operation", "getAvailableLocations", "status", "success");
        this.getAvailableLocationsFailureTimer = registry.timer("wcf.soap.duration",
                "operation", "getAvailableLocations", "status", "failure");
        this.getAvailableLocationsSuccessCounter = registry.counter("wcf.soap.calls.total",
                "operation", "getAvailableLocations", "status", "success");
        this.getAvailableLocationsFailureCounter = registry.counter("wcf.soap.calls.total",
                "operation", "getAvailableLocations", "status", "failure");
    }

     private IWarehouseGripperService getServicePort() {
         try {
             URL wsdlUrl = new URL(config.getWsdlUrl());
             QName serviceName = new QName(
                 "http://tempuri.org/",
                 "WarehouseGripperService"
             );
    
             WarehouseGripperService service =
                 new WarehouseGripperService(wsdlUrl, serviceName);
             IWarehouseGripperService port = service.getBasicHttpBindingIWarehouseGripperService();
     
             config.configureBinding((BindingProvider) port);
             return port;
         } catch (Exception e) {
             log.error("Failed to create WCF service port", e);
             throw new RuntimeException("WCF service connection failed", e);
         }
    }

    /**
     * Get status of a specific gripper
     */
    public GripperStatusResponse getGripperStatus(int gripperId) {
        log.info("Getting status for Gripper {}", gripperId);

        Timer.Sample sample = Timer.start(registry);

        try {
            IWarehouseGripperService port = getServicePort();
            GripperStatusDto wcfResult = port.getGripperStatus(gripperId);

            sample.stop(getGripperStatusSuccessTimer);
            getGripperStatusSuccessCounter.increment();

            return mapToGripperStatusResponse(wcfResult);
        } catch (IWarehouseGripperServiceGetGripperStatusServiceFaultFaultFaultMessage e) {
            sample.stop(getGripperStatusFailureTimer);
            getGripperStatusFailureCounter.increment();
            String errorMsg = unwrapJAXBElement(e.getFaultInfo().getErrorMessage());
            log.error("WCF Service Fault: {}", errorMsg);
            throw new RuntimeException("Failed to get gripper status: " + errorMsg);
        } catch (Exception e) {
            sample.stop(getGripperStatusFailureTimer);
            getGripperStatusFailureCounter.increment();
            log.error("Failed to get gripper status", e);
            throw new RuntimeException("Failed to get gripper status", e);
        }

        // try {
        //     // TEMPORARY MOCK DATA - Remove after WSDL generation
        //     GripperStatusResponse response = GripperStatusResponse.builder()
        //             .gripperId(gripperId)
        //             .state("Idle")
        //             .positionX(0.0)
        //             .positionY(0.0)
        //             .positionZ(0.0)
        //             .hasLoadCarrier(false)
        //             .isEnabled(true)
        //             .hasError(false)
        //             .lastUpdated(LocalDateTime.now())
        //             .build();

        //     sample.stop(getGripperStatusSuccessTimer);
        //     getGripperStatusSuccessCounter.increment();

        //     return response;

        // } catch (Exception e) {
        //     sample.stop(getGripperStatusFailureTimer);
        //     getGripperStatusFailureCounter.increment();

        //     log.error("Failed to get Gripper status", e);
        //     throw new RuntimeException("Failed to get Gripper status", e);
        // }
    }

    /**
     * Get status of all grippers
     */
    public List<GripperStatusResponse> getAllGrippers() {
        log.info("Getting all grippers");

        Timer.Sample sample = Timer.start(registry);

        try {
            IWarehouseGripperService port = getServicePort();
            ArrayOfGripperStatusDto wcfResult = port.getAllGrippers();

            sample.stop(getAllGrippersSuccessTimer);
            getAllGrippersSuccessCounter.increment();

            return wcfResult.getGripperStatusDto().stream()
                .map(this::mapToGripperStatusResponse)
                .collect(Collectors.toList());
        } catch (IWarehouseGripperServiceGetAllGrippersServiceFaultFaultFaultMessage e) {
            sample.stop(getAllGrippersFailureTimer);
            getAllGrippersFailureCounter.increment();
            String errorMsg = unwrapJAXBElement(e.getFaultInfo().getErrorMessage());
            log.error("WCF Service Fault: {}", errorMsg);
            throw new RuntimeException("Failed to get all grippers: " + errorMsg);
        } catch (Exception e) {
            sample.stop(getAllGrippersFailureTimer);
            getAllGrippersFailureCounter.increment();
            log.error("Failed to get all grippers", e);
            throw new RuntimeException("Failed to get all grippers", e);
        }

        // try {
        //     // TEMPORARY MOCK DATA
        //     List<GripperStatusResponse> response = List.of(
        //         GripperStatusResponse.builder().gripperId(1).state("Idle").build(),
        //         GripperStatusResponse.builder().gripperId(2).state("Idle").build()
        //     );

        //     sample.stop(getAllGrippersSuccessTimer);
        //     getAllGrippersSuccessCounter.increment();

        //     return response;

        // } catch (Exception e) {
        //     sample.stop(getAllGrippersFailureTimer);
        //     getAllGrippersFailureCounter.increment();

        //     log.error("Failed to get all grippers", e);
        //     throw new RuntimeException("Failed to get all grippers", e);
        // }
    }

    /**
     * Move gripper to position
     */
    public OperationResponse moveGripper(int gripperId, double x, double y, double z) {
        log.info("Moving Gripper {} to ({}, {}, {})", gripperId, x, y, z);

        Timer.Sample sample = Timer.start(registry);

        try {
            IWarehouseGripperService port = getServicePort();
            OperationResultDto wcfResult = port.moveGripper(gripperId, x, y, z);
            sample.stop(moveGripperSuccessTimer);
            moveGripperSuccessCounter.increment();
            return mapToOperationResponse(wcfResult);
        } catch (IWarehouseGripperServiceMoveGripperServiceFaultFaultFaultMessage e) {
            sample.stop(moveGripperFailureTimer);
            moveGripperFailureCounter.increment();
            String errorMsg = unwrapJAXBElement(e.getFaultInfo().getErrorMessage());
            log.error("WCF Service Fault: {}", errorMsg);
            throw new RuntimeException("Failed to move gripper: " + errorMsg);
        } catch (Exception e) {
            sample.stop(moveGripperFailureTimer);
            moveGripperFailureCounter.increment();
            log.error("Failed to move gripper", e);
            throw new RuntimeException("Failed to move gripper", e);
        }

        // try {
        //     // TEMPORARY MOCK DATA
        //     OperationResponse response = OperationResponse.builder()
        //             .success(true)
        //             .message("Move command sent (MOCK)")
        //             .timestamp(LocalDateTime.now())
        //             .build();

        //     sample.stop(moveGripperSuccessTimer);
        //     moveGripperSuccessCounter.increment();

        //     return response;

        // } catch (Exception e) {
        //     sample.stop(moveGripperFailureTimer);
        //     moveGripperFailureCounter.increment();

        //     log.error("Failed to move gripper", e);
        //     throw new RuntimeException("Failed to move gripper", e);
        // }
    }

    /**
     * Pick load carrier from location
     */
    public OperationResponse pickLoadCarrier(int gripperId, int locationId) {
        log.info("Gripper {} picking from Location {}", gripperId, locationId);

        Timer.Sample sample = Timer.start(registry);

        /* GENERATED CODE - Uncomment after WSDL generation:
        try {
            IWarehouseGripperService port = getServicePort();
            OperationResultDto wcfResult = port.pickLoadCarrier(gripperId, locationId);

            return mapToOperationResponse(wcfResult);
        } catch (ServiceFault_Exception e) {
            log.error("WCF Service Fault: {}", e.getFaultInfo().getErrorMessage());
            throw new RuntimeException("Failed to pick load carrier: " + e.getFaultInfo().getErrorMessage());
        } catch (Exception e) {
            log.error("Failed to pick load carrier", e);
            throw new RuntimeException("Failed to pick load carrier", e);
        }
        */

        try {
            // TEMPORARY MOCK DATA
            OperationResponse response = OperationResponse.builder()
                    .success(true)
                    .message("Pick command sent (MOCK)")
                    .timestamp(LocalDateTime.now())
                    .build();

            sample.stop(pickLoadCarrierSuccessTimer);
            pickLoadCarrierSuccessCounter.increment();

            return response;

        } catch (Exception e) {
            sample.stop(pickLoadCarrierFailureTimer);
            pickLoadCarrierFailureCounter.increment();

            log.error("Failed to pick load carrier", e);
            throw new RuntimeException("Failed to pick load carrier", e);
        }
    }

    /**
     * Place load carrier at location
     */
    public OperationResponse placeLoadCarrier(int gripperId, int locationId) {
        log.info("Gripper {} placing at Location {}", gripperId, locationId);

        Timer.Sample sample = Timer.start(registry);

        /* GENERATED CODE - Uncomment after WSDL generation:
        try {
            IWarehouseGripperService port = getServicePort();
            OperationResultDto wcfResult = port.placeLoadCarrier(gripperId, locationId);

            return mapToOperationResponse(wcfResult);
        } catch (ServiceFault_Exception e) {
            log.error("WCF Service Fault: {}", e.getFaultInfo().getErrorMessage());
            throw new RuntimeException("Failed to place load carrier: " + e.getFaultInfo().getErrorMessage());
        } catch (Exception e) {
            log.error("Failed to place load carrier", e);
            throw new RuntimeException("Failed to place load carrier", e);
        }
        */

        try {
            // TEMPORARY MOCK DATA
            OperationResponse response = OperationResponse.builder()
                    .success(true)
                    .message("Place command sent (MOCK)")
                    .timestamp(LocalDateTime.now())
                    .build();

            sample.stop(placeLoadCarrierSuccessTimer);
            placeLoadCarrierSuccessCounter.increment();

            return response;

        } catch (Exception e) {
            sample.stop(placeLoadCarrierFailureTimer);
            placeLoadCarrierFailureCounter.increment();

            log.error("Failed to place load carrier", e);
            throw new RuntimeException("Failed to place load carrier", e);
        }
    }

    /**
     * Create warehouse operation
     */
    public OperationResponse createOperation(OperationRequest request) {
        log.info("Creating operation: Type={}, Gripper={}", request.getOperationType(), request.getGripperId());

        Timer.Sample sample = Timer.start(registry);

        // try {
        //     IWarehouseGripperService port = getServicePort();

        //     OperationRequestDto wcfRequest = new OperationRequestDto();
        //     wcfRequest.setGripperId(request.getGripperId());
        //     wcfRequest.setOperationType(request.getOperationType());
        //     wcfRequest.setSourceLocationId(request.getSourceLocationId());
        //     wcfRequest.setTargetLocationId(request.getTargetLocationId());
        //     wcfRequest.setLoadCarrierId(request.getLoadCarrierId());
        //     wcfRequest.setPriority(request.getPriority());

        //     OperationResultDto wcfResult = port.createOperation(wcfRequest);
        //     return mapToOperationResponse(wcfResult);
        // } catch (ServiceFault_Exception e) {
        //     log.error("WCF Service Fault: {}", e.getFaultInfo().getErrorMessage());
        //     throw new RuntimeException("Failed to create operation: " + e.getFaultInfo().getErrorMessage());
        // } catch (Exception e) {
        //     log.error("Failed to create operation", e);
        //     throw new RuntimeException("Failed to create operation", e);
        // }

        try {
            // TEMPORARY MOCK DATA
            OperationResponse response = OperationResponse.builder()
                    .success(true)
                    .operationId(123)
                    .message("Operation created (MOCK)")
                    .timestamp(LocalDateTime.now())
                    .build();

            sample.stop(createOperationSuccessTimer);
            createOperationSuccessCounter.increment();

            return response;

        } catch (Exception e) {
            sample.stop(createOperationFailureTimer);
            createOperationFailureCounter.increment();

            log.error("Failed to create operation", e);
            throw new RuntimeException("Failed to create operation", e);
        }
    }

    /**
     * Get available locations
     */
    public List<LocationResponse> getAvailableLocations() {
        log.info("Getting available locations");

        Timer.Sample sample = Timer.start(registry);

        try {
            IWarehouseGripperService port = getServicePort();
            ArrayOfLocationDto wcfResult = port.getAvailableLocations();

            sample.stop(getAvailableLocationsSuccessTimer);
            getAvailableLocationsSuccessCounter.increment();

            return wcfResult.getLocationDto().stream()
                .map(this::mapToLocationResponse)
                .collect(Collectors.toList());
        } catch (IWarehouseGripperServiceGetAvailableLocationsServiceFaultFaultFaultMessage e) {
            sample.stop(getAvailableLocationsFailureTimer);
            getAvailableLocationsFailureCounter.increment();
            String errorMsg = unwrapJAXBElement(e.getFaultInfo().getErrorMessage());
            log.error("WCF Service Fault: {}", errorMsg);
            throw new RuntimeException("Failed to get available locations: " + errorMsg);
        } catch (Exception e) {
            sample.stop(getAvailableLocationsFailureTimer);
            getAvailableLocationsFailureCounter.increment();
            log.error("Failed to get available locations", e);
            throw new RuntimeException("Failed to get available locations", e);
        }

        // try {
        //     // TEMPORARY MOCK DATA
        //     List<LocationResponse> response = List.of(
        //         LocationResponse.builder().locationId(1).locationCode("R01-L01-P01").isOccupied(false).build(),
        //         LocationResponse.builder().locationId(2).locationCode("R01-L01-P02").isOccupied(false).build()
        //     );

        //     sample.stop(getAvailableLocationsSuccessTimer);
        //     getAvailableLocationsSuccessCounter.increment();

        //     return response;

        // } catch (Exception e) {
        //     sample.stop(getAvailableLocationsFailureTimer);
        //     getAvailableLocationsFailureCounter.increment();

        //     log.error("Failed to get available locations", e);
        //     throw new RuntimeException("Failed to get available locations", e);
        // }
    }

    /**
     * Health check
     */
    public boolean isServiceHealthy() {
        try {
            IWarehouseGripperService port = getServicePort();
            return port.isServiceHealthy();
        } catch (Exception e) {
            log.error("Health check failed", e);
            return false;
        }
    }

    /**
     * Helper method to extract value from JAXBElement (handles nulls)
     */
    private <T> T unwrapJAXBElement(JAXBElement<T> element) {
        return element != null ? element.getValue() : null;
    }
    
    private GripperStatusResponse mapToGripperStatusResponse(GripperStatusDto dto) {
        return GripperStatusResponse.builder()
                .gripperId(dto.getGripperId() != null ? dto.getGripperId() : 0)
                .state(unwrapJAXBElement(dto.getState()))
                .positionX(dto.getPositionX() != null ? dto.getPositionX() : 0.0)
                .positionY(dto.getPositionY() != null ? dto.getPositionY() : 0.0)
                .positionZ(dto.getPositionZ() != null ? dto.getPositionZ() : 0.0)
                .hasLoadCarrier(dto.isHasLoadCarrier() != null && dto.isHasLoadCarrier())
                .currentLoadCarrierId(unwrapJAXBElement(dto.getCurrentLoadCarrierId()))
                .isEnabled(dto.isIsEnabled() != null && dto.isIsEnabled())
                .hasError(dto.isHasError() != null && dto.isHasError())
                .errorMessage(unwrapJAXBElement(dto.getErrorMessage()))
                .currentLoad(dto.getCurrentLoad() != null ? dto.getCurrentLoad() : 0.0)
                .gripperWidth(dto.getGripperWidth() != null ? dto.getGripperWidth() : 0.0)
                .lastUpdated(dto.getLastUpdated() != null
                    ? dto.getLastUpdated().toGregorianCalendar().toZonedDateTime().toLocalDateTime()
                    : LocalDateTime.now())
                .build();
    }

    private OperationResponse mapToOperationResponse(OperationResultDto dto) {
        return OperationResponse.builder()
                .success(dto.isSuccess())
                .operationId(this.unwrapJAXBElement(dto.getOperationId()))
                .message(this.unwrapJAXBElement(dto.getMessage()))
                .errorCode(this.unwrapJAXBElement(dto.getErrorCode()))
                .timestamp(dto.getTimestamp().toGregorianCalendar()
                    .toZonedDateTime().toLocalDateTime())
                .build();
    }

    private LocationResponse mapToLocationResponse(LocationDto dto) {
        return LocationResponse.builder()
                .locationId(dto.getLocationId())
                .locationCode(this.unwrapJAXBElement(dto.getLocationCode()))
                .locationType(this.unwrapJAXBElement(dto.getLocationType()))
                .isOccupied(dto.isIsOccupied())
                .loadCarrierId(this.unwrapJAXBElement(dto.getLoadCarrierId()))
                .positionX(dto.getPositionX())
                .positionY(dto.getPositionY())
                .positionZ(dto.getPositionZ())
                .build();
    }

}
