package com.robot.warehouse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.xml.ws.BindingProvider;

/**
 * Configuration for WCF Service Client
 */
@Configuration
public class WcfServiceConfig {

    @Value("${wcf.service.url}")
    private String serviceUrl;

    @Value("${wcf.service.timeout:30000}")
    private int timeout;

    public String getServiceUrl() {
        return serviceUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Configure binding properties for WCF client
     */
    public void configureBinding(BindingProvider bindingProvider) {
        var requestContext = bindingProvider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceUrl);
        requestContext.put("com.sun.xml.ws.connect.timeout", timeout);
        requestContext.put("com.sun.xml.ws.request.timeout", timeout);
    }
}
