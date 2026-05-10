package com.khalid.freyr.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String status,
        String message,
        Map<String, String> errors,
        Instant timestamp
) {

    public static ErrorResponse of(String status, String message) {
        return new ErrorResponse(status, message, Map.of(), Instant.now());
    }

    public static ErrorResponse of(String status, String message, Map<String, String> errors) {
        return new ErrorResponse(status, message, errors, Instant.now());
    }
}
