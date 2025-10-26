package com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest;

import com.maxgarsaiz.sailpoint.domain.exception.EntityAlreadyLockedException;
import com.maxgarsaiz.sailpoint.domain.exception.EntityNotFoundException;
import com.maxgarsaiz.sailpoint.domain.exception.InvalidStateTransitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFoundException(EntityNotFoundException ex) {
        log.error(ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("%s Not Found".formatted(ex.getEntityType()));
        problemDetail.setType(URI.create("https://api.sailpoint-poc.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
    
    @ExceptionHandler(EntityAlreadyLockedException.class)
    public ProblemDetail handleEntityAlreadyLockedException(EntityAlreadyLockedException ex) {
        log.error(ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("%s Locked".formatted(ex.getEntityType()));
        problemDetail.setType(URI.create("https://api.sailpoint-poc.com/errors/locked"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
    
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidStateTransitionException(InvalidStateTransitionException ex) {
        log.error("Invalid state transition", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problemDetail.setTitle("Invalid State Transition");
        problemDetail.setType(URI.create("https://api.sailpoint-poc.com/errors/invalid-transition"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://api.sailpoint-poc.com/errors/validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", ex.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList()
        );
        
        return problemDetail;
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        log.error("Unexpected error", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.sailpoint-poc.com/errors/internal"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
}