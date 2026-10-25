package com.group4.warehouse.common;

import java.util.Map;

/**
 * The JSON body of every error response.
 * The frontend shows {@code message} to the user and puts {@code fields} next to form inputs.
 *
 * Example: {"message":"Warehouse WH09 was not found.","code":"NOT_FOUND","fields":{}}
 */
public record ApiError(String message, String code, Map<String, String> fields) {

    public ApiError(String message, String code) {
        this(message, code, Map.of());
    }
}
