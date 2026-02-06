package com.robot.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Gripper Status
 * Mirrors .NET GripperStatusDto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GripperStatusResponse {
    private int gripperId;
    private String state;
    private double positionX;
    private double positionY;
    private double positionZ;
    private boolean hasLoadCarrier;
    private Integer currentLoadCarrierId;
    private boolean isEnabled;
    private boolean hasError;
    private String errorMessage;
    private double currentLoad;
    private double gripperWidth;
    private LocalDateTime lastUpdated;
}
