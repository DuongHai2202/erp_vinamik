package vn.vinamik.erp_backend.platform.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

/**
 * Keeps the masked token for response rendering while accepting the raw token
 * copied by a browser SPA from the readable XSRF-TOKEN cookie.
 */
public final class spa_csrf_token_request_handler implements CsrfTokenRequestHandler {
    private final CsrfTokenRequestHandler plain_handler = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor_handler = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> deferred_csrf_token) {
        xor_handler.handle(request, response, deferred_csrf_token);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrf_token) {
        String header_value = request.getHeader(csrf_token.getHeaderName());
        if (header_value != null) {
            return plain_handler.resolveCsrfTokenValue(request, csrf_token);
        }
        return xor_handler.resolveCsrfTokenValue(request, csrf_token);
    }
}