package com.robot.warehouse.config;

import com.robot.warehouse.dto.OperationResponse;
import com.robot.warehouse.service.WcfGripperServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class WcfGripperServiceClientTestConfig {
    
    @Bean
    @Primary
    public WcfServiceConfig wcfServiceConfigTest() {
        // Mock Config - braucht keine echte URL
        return new WcfServiceConfig() {
            @Override
            public String getServiceUrl() {
                return "http://localhost:9999/mock";
            }
            
            @Override
            public int getTimeout() {
                return 5000;
            }
        };
    }
    
    @Bean
    @Primary
    public WcfGripperServiceClient wcfGripperServiceClientTest(WcfServiceConfig config) {
        WcfGripperServiceClient mock = mock(WcfGripperServiceClient.class);
        
        // Default: Fehler-Response für CircuitBreaker-Test
        OperationResponse errorResponse = OperationResponse.builder()
                .success(false)
                .errorCode("LOCATION_EMPTY")
                .message("Location is empty")
                .timestamp(LocalDateTime.now())
                .build();
        
        when(mock.pickLoadCarrier(anyInt(), anyInt()))
                .thenReturn(errorResponse);
        
        return mock;
    }
}