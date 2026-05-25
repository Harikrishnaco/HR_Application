package com.example.demo.attendance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse errorDetails = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(errorDetails, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorDetails = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected system error occurred: " + ex.getMessage()
        );
        return new ResponseEntity<>(errorDetails, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }
}