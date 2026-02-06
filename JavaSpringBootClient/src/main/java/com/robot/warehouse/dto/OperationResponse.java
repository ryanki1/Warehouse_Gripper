package com.robot.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for operation results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResponse {
    private boolean success;
    private Integer operationId;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
}
