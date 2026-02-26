package com.robot.warehouse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Configuration
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Constructor injection with @Lazy to avoid circular dependency.
     * CircuitBreakerRegistry is lazily initialized to break the cycle.
     */
    public MetricsConfig(MeterRegistry meterRegistry, @Lazy CircuitBreakerRegistry circuitBreakerRegistry) {
        this.meterRegistry = meterRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Bind Circuit Breaker metrics after application context is fully initialized.
     * This avoids circular dependency issues during bean creation.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void bindCircuitBreakerMetrics() {
        TaggedCircuitBreakerMetrics
                .ofCircuitBreakerRegistry(circuitBreakerRegistry)
                .bindTo(meterRegistry);
    }

    /**
     * Custom WCF SOAP metrics
     */
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
