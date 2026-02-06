package com.robot.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application for robot Warehouse Gripper Client
 * Provides REST API that communicates with .NET WCF Service
 */
@SpringBootApplication
public class GripperClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(GripperClientApplication.class, args);
        System.out.println("========================================================");
        System.out.println("robot Warehouse Gripper Client Started");
        System.out.println("========================================================");
        System.out.println("REST API:     http://localhost:8081/api/warehouse");
        System.out.println("Swagger UI:   http://localhost:8081/swagger-ui.html");
        System.out.println("========================================================");
    }
}
