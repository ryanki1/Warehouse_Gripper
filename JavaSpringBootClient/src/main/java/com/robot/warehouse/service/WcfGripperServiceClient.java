package com.robot.warehouse.service;

import com.robot.warehouse.config.WcfServiceConfig;
import com.robot.warehouse.dto.*;
import com.robot.warehouse.exception.OperationResponseException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        /**
     * Fallback helper - throws appropriate exception based on error type
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private OperationResponse operationResponseFallback(Throwable ex, String context) throws Exception {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker is OPEN - {}", context);
            OperationResponse error = new OperationResponse();
            error.setErrorCode("SERVICE_UNAVAILABLE");
            error.setMessage("Service is unavailable. Please try later.");
            throw new OperationResponseException(error.getErrorCode(), error.getMessage());
        } else if (ex instanceof OperationResponseException) {
            // Business Fehler (GRIPPER_NOT_FOUND, etc.) → Original-Fehler behalten!
            log.warn("Circuit Breaker fallback - Business error: {}", ex.getMessage());
            throw (OperationResponseException) ex;
        } else {
            log.error("Circuit Breaker fallback - Unexpected error: {}: {}", context, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    // --- Specific Fallback Methods (delegate to generic handler) ---

    @SuppressWarnings("unused")
    private OperationResponse pickPlaceOperationFallback(int gripperId, int locationId, Throwable ex) throws Exception {
        return operationResponseFallback(ex, String.format("Gripper %d at Location %d", gripperId, locationId));
    }

    @SuppressWarnings("unused")
    private OperationResponse moveGripperFallback(int id, double x, double y, double z, Throwable ex) throws Exception {
        return operationResponseFallback(ex, String.format("Move Gripper %d to (%.2f, %.2f, %.2f)", id, x, y, z));
    }

    @SuppressWarnings("unused")
    private OperationResponse createOperationFallback(OperationRequest request, Throwable ex) throws Exception {
        return operationResponseFallback(ex, String.format("Create Operation: Type=%s, Gripper=%d",
            request.getOperationType(), request.getGripperId()));
    }

    @SuppressWarnings("unused")
    private GripperStatusResponse getByIdFallback(int id, Throwable ex) throws Exception {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker is OPEN - Get Gripper {}", id);
            OperationResponse error = new OperationResponse();
            error.setErrorCode("SERVICE_UNAVAILABLE");
            error.setMessage("Service is unavailable. Please try later.");
            throw new OperationResponseException(error.getErrorCode(), error.getMessage());
        } else if (ex instanceof OperationResponseException) {
            log.warn("Circuit Breaker fallback - Get Gripper {}: {}", id, ex.getMessage());
            throw (OperationResponseException) ex;  // Re-throw original
        } else {
            log.error("Circuit Breaker fallback - Unexpected Error: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unused")
    private List<GripperStatusResponse> getAllGrippersFallback(Throwable ex) throws Exception {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker is OPEN - Get All Grippers");
            throw new OperationResponseException("SERVICE_UNAVAILABLE", "Service is unavailable. Please try later.");
        } else if (ex instanceof OperationResponseException) {
            log.warn("Circuit Breaker fallback - Get All Grippers: {}", ex.getMessage());
            throw (OperationResponseException) ex;
        } else {
            log.error("Circuit Breaker fallback - Get All Grippers: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unused")
    private List<LocationResponse> getAvailableLocationsFallback(Throwable ex) throws Exception {
        if (ex instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker is OPEN - Get Available Locations");
            throw new OperationResponseException("SERVICE_UNAVAILABLE", "Service is unavailable. Please try later.");
        } else if (ex instanceof OperationResponseException) {
            log.warn("Circuit Breaker fallback - Get Available Locations: {}", ex.getMessage());
            throw (OperationResponseException) ex;
        } else {
            log.error("Circuit Breaker fallback - Get Available Locations: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
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
    @CircuitBreaker(name="get-gripper-by-id", fallbackMethod = "getByIdFallback")
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
            log.error("✅ SOAP Fault caught - ErrorCode: {}, Message: {}",
                unwrapJAXBElement(e.getFaultInfo().getErrorCode()), errorMsg);
            throw new OperationResponseException(
                unwrapJAXBElement(e.getFaultInfo().getErrorCode()),
                unwrapJAXBElement(e.getFaultInfo().getStackTrace()),
                unwrapJAXBElement(e.getFaultInfo().getErrorMessage())
            );
        } catch (Exception e) {
            sample.stop(getGripperStatusFailureTimer);
            getGripperStatusFailureCounter.increment();
            log.error("Failed to get gripper status", e);
            throw new RuntimeException("Failed to get gripper status", e);
        }

    }

    /**
     * Get status of all grippers
     */
    @CircuitBreaker(name="get-all-grippers", fallbackMethod = "getAllGrippersFallback")
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

    }

    /**
     * Move gripper to position
     */
    @CircuitBreaker(name="move-gripper", fallbackMethod = "moveGripperFallback")
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

            String errorCode = unwrapJAXBElement(e.getFaultInfo().getErrorCode());
            String errorMsg = unwrapJAXBElement(e.getFaultInfo().getErrorMessage());
            String stackTrace = unwrapJAXBElement(e.getFaultInfo().getStackTrace());

            throw new OperationResponseException(errorCode, stackTrace, errorMsg);
        } catch (Exception e) {
            sample.stop(moveGripperFailureTimer);
            moveGripperFailureCounter.increment();

            throw new RuntimeException("Failed to move gripper", e);
        }

    }

    /**
     * Pick load carrier from location
     */
    @CircuitBreaker(name="pick-gripper", fallbackMethod = "pickPlaceOperationFallback")
    public OperationResponse pickLoadCarrier(int gripperId, int locationId) {
        log.info("Gripper {} picking from Location {}", gripperId, locationId);

        Timer.Sample sample = Timer.start(registry);

        try {
            IWarehouseGripperService port = getServicePort();
            OperationResultDto wcfResult = port.pickLoadCarrier(gripperId, locationId);

            sample.stop(pickLoadCarrierSuccessTimer);
            pickLoadCarrierSuccessCounter.increment();

            return mapToOperationResponse(wcfResult);
        } catch (IWarehouseGripperServicePickLoadCarrierServiceFaultFaultFaultMessage e) {
            sample.stop(pickLoadCarrierFailureTimer);
            pickLoadCarrierFailureCounter.increment();

            String errorCode = unwrapJAXBElement(e.getFaultInfo().getErrorCode());
            String errorMsg = unwrapJAXBElement(e.getFaultInfo().getErrorMessage());
            String stackTrace = unwrapJAXBElement(e.getFaultInfo().getStackTrace());

            throw new OperationResponseException(errorCode, stackTrace, errorMsg);
        } catch (Exception e) {
            sample.stop(pickLoadCarrierFailureTimer);
            pickLoadCarrierFailureCounter.increment();
            throw new RuntimeException("Failed to pick Load Carrier", e);
        }

    }

    /**
     * Place load carrier at location
     */
    @CircuitBreaker(name="place-gripper", fallbackMethod = "pickPlaceOperationFallback")
    public OperationResponse placeLoadCarrier(int gripperId, int locationId) {
        log.info("Gripper {} placing at Location {}", gripperId, locationId);

        Timer.Sample sample = Timer.start(registry);

        try {
            IWarehouseGripperService port = getServicePort();
            OperationResultDto wcfResult = port.placeLoadCarrier(gripperId, locationId);

            sample.stop(placeLoadCarrierSuccessTimer);
            placeLoadCarrierSuccessCounter.increment();

            return mapToOperationResponse(wcfResult);
        } catch (IWarehouseGripperServicePlaceLoadCarrierServiceFaultFaultFaultMessage e) {
            sample.stop(placeLoadCarrierFailureTimer);
            placeLoadCarrierFailureCounter.increment();

            String errorCode = unwrapJAXBElement(e.getFaultInfo().getErrorCode());
            String errorMsg = unwrapJAXBElement(e.getFaultInfo().getErrorMessage());
            String stackTrace = unwrapJAXBElement(e.getFaultInfo().getStackTrace());

            throw new OperationResponseException(errorCode, stackTrace, errorMsg);
        } catch (Exception e) {
            sample.stop(placeLoadCarrierFailureTimer);
            placeLoadCarrierFailureCounter.increment();
            throw new RuntimeException("Failed to place Load Carrier", e);
        }

    }

    /**
     * Create warehouse operation
     */
    @CircuitBreaker(name="create-operation", fallbackMethod = "createOperationFallback")
    public OperationResponse createOperation(OperationRequest request) throws Exception {
        log.info("Creating operation: Type={}, Gripper={}", request.getOperationType(), request.getGripperId());

        Timer.Sample sample = Timer.start(registry);

        try {
            IWarehouseGripperService port = getServicePort();
            ObjectFactory factory = new ObjectFactory();

            OperationRequestDto wcfRequest = factory.createOperationRequestDto();
            wcfRequest.setGripperId(request.getGripperId());
            wcfRequest.setOperationType(factory.createOperationRequestDtoOperationType(request.getOperationType()));
            wcfRequest.setSourceLocationId(factory.createOperationRequestDtoSourceLocationId(request.getSourceLocationId()));
            wcfRequest.setTargetLocationId(factory.createOperationRequestDtoTargetLocationId(request.getTargetLocationId()));
            wcfRequest.setLoadCarrierId(factory.createOperationRequestDtoLoadCarrierId(request.getLoadCarrierId()));
            wcfRequest.setPriority(factory.createOperationRequestDtoPriority(request.getPriority()));

            OperationResultDto wcfResult = port.createOperation(wcfRequest);

            sample.stop(createOperationSuccessTimer);
            createOperationSuccessCounter.increment();

            if (!wcfResult.isSuccess()) {
                throw new OperationResponseException(this.unwrapJAXBElement(wcfResult.getErrorCode()), this.unwrapJAXBElement(wcfResult.getMessage()));
            }

            return mapToOperationResponse(wcfResult);
        }
        catch (IWarehouseGripperServiceCreateOperationServiceFaultFaultFaultMessage e) {
            sample.stop(createOperationFailureTimer);
            createOperationFailureCounter.increment();
            String errorMsg = unwrapJAXBElement(e.getFaultInfo().getErrorMessage());
            log.error("✅ SOAP Fault caught - ErrorCode: {}, Message: {}",
                unwrapJAXBElement(e.getFaultInfo().getErrorCode()), errorMsg);
            throw new OperationResponseException(
                unwrapJAXBElement(e.getFaultInfo().getErrorCode()),
                unwrapJAXBElement(e.getFaultInfo().getStackTrace()),
                unwrapJAXBElement(e.getFaultInfo().getErrorMessage())
            );
        }
        catch (Exception e) {
            sample.stop(createOperationFailureTimer);
            createOperationFailureCounter.increment();
            throw new RuntimeException("Failed to create operation", e);
        }

    }

    /**
     * Get available locations
     */
    @CircuitBreaker(name="get-available-locations", fallbackMethod = "getAvailableLocationsFallback")
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

    /**
     * Helper method to wrap String values as JAXBElement (for WCF requests)
     */
    private JAXBElement<String> wrapString(String value) {
        if (value == null) return null;
        return new JAXBElement<>(new QName("http://robot.warehouse.gripper/2024", "string"),
                                 String.class, value);
    }

    /**
     * Helper method to wrap Integer values as JAXBElement (for WCF requests)
     */
    private JAXBElement<Integer> wrapInteger(Integer value) {
        if (value == null) return null;
        return new JAXBElement<>(new QName("http://robot.warehouse.gripper/2024", "int"),
                                 Integer.class, value);
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
