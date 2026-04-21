package com.f1.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.concurrent.CompletionException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OpenF1Exception.class)
    public ResponseEntity<Map<String, Object>> handleOpenF1Exception(OpenF1Exception ex) {
        log.error("OpenF1 API error (HTTP {}): {}", ex.getStatusCode(), ex.getMessage());
        // 429 from upstream → 503 to the client (upstream unavailable, not client fault)
        int status = ex.getStatusCode() == 429 ? 503 : ex.getStatusCode();
        return ResponseEntity.status(status).body(Map.of(
                "error", ex.getMessage(),
                "status", status));
    }

    // CompletableFuture.join() wraps exceptions in CompletionException — unwrap it.
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<Map<String, Object>> handleCompletionException(CompletionException ex) {
        if (ex.getCause() instanceof OpenF1Exception cause) {
            return handleOpenF1Exception(cause);
        }
        log.error("Unexpected parallel execution error", ex);
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "status", 500));
    }
}
