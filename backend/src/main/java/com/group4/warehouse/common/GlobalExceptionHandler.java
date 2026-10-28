package com.group4.warehouse.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tools.jackson.core.JacksonException;

/**
 * Catches exceptions from every controller and returns an ApiError JSON body
 * instead of a stack trace or a Whitelabel page.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Our own errors: not found, duplicate, in use, ... */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ApiError(e.getMessage(), e.code()));
    }

    /** @Valid failed: return every invalid field so the form can highlight them. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiError("Please correct the highlighted fields.", "VALIDATION_FAILED", fields));
    }

    /**
     * Body is missing, is not valid JSON, or a value has the wrong type
     * (e.g. "reorderLevel": 12.5 or "abc" for a whole-number field).
     * When Jackson can tell which field failed, report it like a validation error.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
            String field = jackson.getPath().get(jackson.getPath().size() - 1).getPropertyName();
            if (field != null) {
                return ResponseEntity.badRequest().body(new ApiError(
                        "Please correct the highlighted fields.", "VALIDATION_FAILED",
                        Map.of(field, "Invalid value: wrong type for this field (e.g. a decimal where a whole number is required).")));
            }
        }
        return ResponseEntity.badRequest()
                .body(new ApiError("The request body is missing or is not valid JSON.", "INVALID_JSON"));
    }

    /** Anything else. Spring's own errors (404 unknown URL, 405 wrong method) keep their status. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception e) {
        if (e instanceof ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return ResponseEntity.status(status).body(new ApiError(status.getReasonPhrase(), status.name()));
        }
        log.error("Unhandled error", e);
        return ResponseEntity.internalServerError()
                .body(new ApiError("Something went wrong on the server.", "INTERNAL_ERROR"));
    }
}
