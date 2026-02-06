package com.robot.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for warehouse location information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {
    private int locationId;
    private String locationCode;
    private String locationType;
    private boolean isOccupied;
    private Integer loadCarrierId;
    private double positionX;
    private double positionY;
    private double positionZ;
}
