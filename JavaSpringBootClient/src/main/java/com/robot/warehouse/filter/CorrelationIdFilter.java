package com.robot.warehouse.filter;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest.Headers;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CorrelationIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String CORRELATION_ID_HEADER = "X-Correlation-ID";
        final String CORRELATION_ID_MDC_KEY = "correlationId";

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationIdHeader = httpRequest.getHeader(CORRELATION_ID_HEADER);

        if (correlationIdHeader == null || correlationIdHeader.isEmpty()) {
            correlationIdHeader = UUID.randomUUID().toString();
        }
        log.debug("CorrelationId received from the request, otherwise generated: {}", correlationIdHeader);

        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationIdHeader);

        chain.doFilter(request, response);

        try {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationIdHeader);        
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
    
}
