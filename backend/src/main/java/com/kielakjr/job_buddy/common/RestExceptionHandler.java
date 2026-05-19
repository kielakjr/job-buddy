package com.kielakjr.job_buddy.common;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kielakjr.job_buddy.application.ApplicationNotFoundException;
import com.kielakjr.job_buddy.tag.TagNotFoundException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ApplicationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<Map<String, Object>> tagNotFound(TagNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("BAD_REQUEST", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("CONFLICT", "Resource conflict", List.of()));
    }

    @ExceptionHandler(com.kielakjr.job_buddy.application.InvalidTagIdsException.class)
    public ResponseEntity<Map<String, Object>> invalidTagIds(
            com.kielakjr.job_buddy.application.InvalidTagIdsException ex) {
        var details = ex.getUnknownIds().stream()
                .map(id -> Map.<String, Object>of("tagId", id.toString()))
                .toList();
        return ResponseEntity.badRequest().body(error("INVALID_TAG_IDS", ex.getMessage(), details));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(error("VALIDATION", "Request validation failed", fields));
    }

    private static Map<String, Object> error(String code, String message, List<Map<String, Object>> details) {
        return Map.of(
                "timestamp", OffsetDateTime.now(),
                "code", code,
                "message", message,
                "details", details);
    }
}
