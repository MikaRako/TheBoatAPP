package com.boatmanagement.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BoatNotFoundException.class)
    public ProblemDetail handleBoatNotFound(BoatNotFoundException ex) {
        log.warn("Boat not found: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Boat Not Found");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setTitle("Validation Error");
        detail.setProperty("errors", errors);
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Malformed JSON request");
        detail.setTitle("Validation Error");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        detail.setTitle("Internal Server Error");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
