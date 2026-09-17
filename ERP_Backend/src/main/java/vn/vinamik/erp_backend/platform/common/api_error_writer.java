package vn.vinamik.erp_backend.platform.common;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
public class api_error_writer {
    private final ObjectMapper object_mapper;

    public api_error_writer(ObjectMapper object_mapper) {
        this.object_mapper = object_mapper;
    }

    public api_error_response create_error(
            HttpServletRequest request,
            String code,
            String message,
            Map<String, String> field_errors) {
        Object correlation_value = request.getAttribute(correlation_id_filter.correlation_attribute);
        String correlation_id = correlation_value instanceof String value ? value : UUID.randomUUID().toString();
        return new api_error_response(code, message, correlation_id, field_errors);
    }

    public void write_error(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        object_mapper.writeValue(response.getOutputStream(), create_error(request, code, message, Map.of()));
    }
}