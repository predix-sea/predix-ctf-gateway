package com.predix.ctfgateway.api;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ApiResponse<T> {
    String code;
    String message;
    T data;
    String traceId;
    String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .code("OK")
                .message("Success")
                .data(data)
                .traceId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now().toString())
                .build();
    }

    public static ApiResponse<Map<String, Object>> error(String code, String message) {
        return ApiResponse.<Map<String, Object>>builder()
                .code(code)
                .message(message)
                .data(Map.of())
                .traceId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now().toString())
                .build();
    }
}
