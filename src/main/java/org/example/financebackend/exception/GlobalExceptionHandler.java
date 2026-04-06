package org.example.financebackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400 Validation ────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage(),
                        (a, b) -> a
                ));
        Map<String, Object> body = errorBody("Validation failed",
                "One or more fields have invalid values", HttpStatus.BAD_REQUEST.value());
        body.put("fields", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── 400 Type mismatch (e.g. non-UUID path variable) ───────────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'"
                + (e.getRequiredType() != null ? " — expected " + e.getRequiredType().getSimpleName() : "");
        return ResponseEntity.badRequest()
                .body(errorBody("Bad Request", msg, HttpStatus.BAD_REQUEST.value()));
    }

    // ── 401 Auth ──────────────────────────────────────────────────────────────
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleUnauthorized(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("Unauthorized", "Invalid credentials", HttpStatus.UNAUTHORIZED.value()));
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody("Forbidden", "Access denied", HttpStatus.FORBIDDEN.value()));
    }

    // ── AppException (business rule violations) ───────────────────────────────
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleAppException(AppException e) {
        return ResponseEntity.status(e.getStatus())
                .body(errorBody(e.getStatus().getReasonPhrase(), e.getMessage(), e.getStatusCode()));
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody("Conflict", e.getMessage(), HttpStatus.CONFLICT.value()));
    }

    // ── 500 Fallback ──────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Internal Server Error",
                        "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    // ── Builder helper ────────────────────────────────────────────────────────
    private Map<String, Object> errorBody(String error, String message, int statusCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("statusCode", statusCode);
        return body;
    }
}
