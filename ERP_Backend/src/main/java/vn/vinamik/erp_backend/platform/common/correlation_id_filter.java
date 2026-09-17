package vn.vinamik.erp_backend.platform.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class correlation_id_filter extends OncePerRequestFilter {
    public static final String correlation_attribute = "erp_correlation_id";
    public static final String correlation_header = "X-Correlation-ID";
    private static final Pattern safe_correlation_id = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filter_chain) throws ServletException, IOException {
        String incoming_id = request.getHeader(correlation_header);
        String correlation_id = incoming_id != null && safe_correlation_id.matcher(incoming_id).matches()
                ? incoming_id
                : UUID.randomUUID().toString();
        request.setAttribute(correlation_attribute, correlation_id);
        response.setHeader(correlation_header, correlation_id);
        filter_chain.doFilter(request, response);
    }
}