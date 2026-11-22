package com.group4.warehouse.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown anywhere in the app when a request must fail with a readable message.
 * GlobalExceptionHandler turns it into an HTTP response with an ApiError body.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /** 409: the record exists but its status does not allow this change (e.g. deleting a shipped issue). */
    public static ApiException locked(String message) {
        return new ApiException(HttpStatus.CONFLICT, "LOCKED", message);
    }

    /** 409: a record the request points at (supplier, order, staff, ...) does not exist. */
    public static ApiException missingReference(String message) {
        return new ApiException(HttpStatus.CONFLICT, "REFERENCE_NOT_FOUND", message);
    }
}
