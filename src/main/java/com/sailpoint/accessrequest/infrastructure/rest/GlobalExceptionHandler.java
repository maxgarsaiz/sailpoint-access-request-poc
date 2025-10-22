package com.sailpoint.accessrequest.infrastructure.rest;

import com.sailpoint.accessrequest.domain.exception.AccessRequestNotFoundException;
import com.sailpoint.accessrequest.domain.exception.InvalidAccessRequestException;
import com.sailpoint.accessrequest.domain.exception.SailpointClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessRequestNotFoundException.class)
    public ProblemDetail handleAccessRequestNotFound(AccessRequestNotFoundException ex) {
        log.error("Access request not found", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Access Request Not Found");
        problemDetail.setType(URI.create("https://api.sailpoint.com/problems/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }

    @ExceptionHandler(InvalidAccessRequestException.class)
    public ProblemDetail handleInvalidAccessRequest(InvalidAccessRequestException ex) {
        log.error("Invalid access request", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Access Request");
        problemDetail.setType(URI.create("https://api.sailpoint.com/problems/invalid-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }

    @ExceptionHandler(SailpointClientException.class)
    public ProblemDetail handleSailpointClientException(SailpointClientException ex) {
        log.error("Sailpoint client error", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage()
        );
        problemDetail.setTitle("Sailpoint Client Error");
        problemDetail.setType(URI.create("https://api.sailpoint.com/problems/sailpoint-client"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error", ex);
        
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed: " + errors
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://api.sailpoint.com/problems/validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.sailpoint.com/problems/internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
}
