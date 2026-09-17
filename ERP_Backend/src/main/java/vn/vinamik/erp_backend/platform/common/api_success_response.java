package vn.vinamik.erp_backend.platform.common;

public record api_success_response<T>(String message, T data) {
}