package vn.vinamik.erp_backend.platform.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.vinamik.erp_backend.platform.identity.invalid_credentials_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class api_exception_handler {
    private static final Logger logger = LoggerFactory.getLogger(api_exception_handler.class);
    private final api_error_writer error_writer;

    public api_exception_handler(api_error_writer error_writer) {
        this.error_writer = error_writer;
    }

    @ExceptionHandler(invalid_credentials_exception.class)
    public ResponseEntity<api_error_response> handle_invalid_credentials(
            invalid_credentials_exception exception,
            HttpServletRequest request) {
        String correlation_id = correlation_id(request);
        logger.warn("Đăng nhập bị từ chối; correlation_id={}", correlation_id);
        return error_response(request, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password.", Map.of());
    }

    @ExceptionHandler(resource_not_found_exception.class)
    public ResponseEntity<api_error_response> handle_not_found(
            resource_not_found_exception exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<api_error_response> handle_access_denied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied.", Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<api_error_response> handle_invalid_argument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), Map.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<api_error_response> handle_validation_error(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> field_errors = new LinkedHashMap<>();
        for (FieldError field_error : exception.getBindingResult().getFieldErrors()) {
            field_errors.putIfAbsent(field_error.getField(),
                    field_error.getDefaultMessage() == null ? "Invalid value." : field_error.getDefaultMessage());
        }
        return error_response(request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Please correct the highlighted fields.", field_errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<api_error_response> handle_method_validation_error(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Please correct the highlighted fields.", Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<api_error_response> handle_constraint_violation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Please correct the highlighted fields.", Map.of());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<api_error_response> handle_unreadable_request(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request body is invalid.", Map.of());
    }

    @ExceptionHandler(field_conflict_exception.class)
    public ResponseEntity<api_error_response> handle_field_conflict(
            field_conflict_exception exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.CONFLICT, "FIELD_CONFLICT", exception.getMessage(),
                Map.of(exception.field_name(), exception.getMessage()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<api_error_response> handle_data_conflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data.", Map.of());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<api_error_response> handle_concurrent_change(
            PessimisticLockingFailureException exception,
            HttpServletRequest request) {
        return error_response(request, HttpStatus.CONFLICT, "CONCURRENT_CHANGE",
                "The resource changed concurrently. Please retry.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<api_error_response> handle_unexpected_error(
            Exception exception,
            HttpServletRequest request) {
        String correlation_id = correlation_id(request);
        logger.error("Lỗi API không dự kiến; loại lỗi={}, correlation_id={}",
                exception.getClass().getSimpleName(), correlation_id);
        return error_response(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", Map.of());
    }

    private ResponseEntity<api_error_response> error_response(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message,
            Map<String, String> field_errors) {
        return ResponseEntity.status(status)
                .body(error_writer.create_error(request, code, message, field_errors));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
