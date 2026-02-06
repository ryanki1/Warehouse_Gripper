package com.robot.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating warehouse operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationRequest {
    private int gripperId;
    private String operationType; // "Pick", "Place", "Move", "Home"
    private Integer sourceLocationId;
    private Integer targetLocationId;
    private Integer loadCarrierId;
    private String priority; // "Low", "Normal", "High", "Emergency"
}
