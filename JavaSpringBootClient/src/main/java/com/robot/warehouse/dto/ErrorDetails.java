package com.robot.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified error response structure for all API errors
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDetails {
    private boolean success;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    private int status;
}
