package com.robot.warehouse.controller;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
// import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.test.web.servlet.MockMvc;

// import com.robot.warehouse.config.WcfGripperServiceClientTestConfig;
import com.robot.warehouse.dto.OperationResponse;
import com.robot.warehouse.service.WcfGripperServiceClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

// import com.robot.warehouse.GripperClientApplication;

// @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
//     properties = "spring.autoconfigure.exclude=org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration",
//     classes = {GripperClientApplication.class}
// )
@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration,org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration",
        "spring.main.allow-bean-definition-overriding=true",
        "wcf.service.url=http://localhost:9999/mock",
    }
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.pick-gripper.failure-rate-threshold=100",
    "resilience4j.circuitbreaker.instances.pick-gripper.minimum-number-of-calls=2",
    "resilience4j.circuitbreaker.instances.pick-gripper.sliding-window-type=COUNT_BASED",
    "resilience4j.circuitbreaker.instances.pick-gripper.sliding-window-size=5",
    "resilience4j.circuitbreaker.instances.pick-gripper.wait-duration-in-open-state=30s",
    "logging.level.io.github.resilience4j=DEBUG"
})
// @Import(WcfGripperServiceClientTestConfig.class) 
@Slf4j
class WarehouseGripperControllerIntegrationTest { // extends ContractVerifierMessage { // TODO [kr] ContractVerifierTest package nicht verfügbar
    @Autowired
    private MockMvc mvc;

    @MockBean
    private WcfGripperServiceClient wcfClient;
    // @Test
    // void testCompletePickAndPlaceLifecycle() {
    //     // runContractTest("getGripperStatus(1)");
    // }

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        // Reset Circuit Breaker vor jedem Test
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pick-gripper");
        circuitBreaker.reset();
    }

    @Test
    void testCircuitBreakerOpensAfterFailures() {
        OperationResponse response = new OperationResponse();
        response.setSuccess(false);
        response.setErrorCode("LOCATION_EMPTY");
        response.setMessage("Location 99 is empty");
        when(wcfClient.pickLoadCarrier(1, 99))
            .thenReturn(response);

        try {
            for (int i = 1; i <= 5; i++) {
                log.info("Number of calls: {}", i);
                mvc.perform(
                    post("/api/warehouse/grippers/1/pick").param("locationId", "99"))
                    .andExpect(status().isBadRequest());
            } 
            mvc.perform(
                    post("/api/warehouse/grippers/1/pick").param("locationId", "99"))
                    .andExpect(status().isServiceUnavailable());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    @Disabled("TODO [kr] include")
    void testPickFromEmptyLocationFails() {
        assumeTrue(false, "TODO [kr] temporary workaround");
        // Arrange
        OperationResponse response = new OperationResponse();
        response.setSuccess(false);
        response.setErrorCode("LOCATION_EMPTY");
        response.setMessage("Location 99 is empty");

        when(wcfClient.pickLoadCarrier(1, 99))
            .thenReturn(response);
    
        try {
            // Act
            mvc.perform(
                post("/api/warehouse/grippers/1/pick").param("locationId", "99"))
            // Assert
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("LOCATION_EMPTY"));
            verify(wcfClient, times(1)).pickLoadCarrier(1, 99);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
