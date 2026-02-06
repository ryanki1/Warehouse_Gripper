package com.robot.warehouse.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.robot.warehouse.dto.OperationResponse;
import com.robot.warehouse.exception.OperationResponseException;

@RestControllerAdvice()
public class GlobalExceptionHandler {

    @ExceptionHandler(OperationResponseException.class)
    public ResponseEntity<OperationResponse> handleOperationResponseException(OperationResponseException ex) {
        OperationResponse error = new OperationResponse();
        error.setErrorCode(ex.getErrorCode());
        error.setMessage(ex.getMessage());
        error.setSuccess(false);
        if (error.getErrorCode() == "SERVICE_UNAVAILABLE") {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        } else {
            return ResponseEntity.badRequest().body(error);
        }
    }
}
