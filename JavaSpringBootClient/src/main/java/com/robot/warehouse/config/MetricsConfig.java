package com.robot.warehouse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Configuration
public class MetricsConfig {
    @Bean
    Counter wcfSoapCallsTotal(MeterRegistry registry) {
        return Counter.builder("wcf.soap.calls.total")
                .description("Total Number of WCF SOAP calls")
                .tag("operation", "unknown")
                .tag("status", "unknown")
                .register(registry);
    }

    @Bean
    Timer wcfSoapDuration(MeterRegistry registry) {
        return Timer.builder("wcf.soap.duration")
                .description("Duration of SOAP calls in seconds")
                .tag("operation", "unknown")
                .register(registry);
    }
}
