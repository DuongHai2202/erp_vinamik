package vn.vinamik.erp_backend.platform.identity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;
import vn.vinamik.erp_backend.platform.common.correlation_id_filter;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class auth_controller {
    private static final Logger logger = LoggerFactory.getLogger(auth_controller.class);
    private final identity_authentication_service authentication_service;
    private final identity_registration_service registration_service;
    private final long session_ttl_seconds;
    private final boolean secure_cookie;

    public auth_controller(
            identity_authentication_service authentication_service,
            identity_registration_service registration_service,
            @Value("${erp.auth.session-ttl-seconds:43200}") long session_ttl_seconds,
            @Value("${erp.auth.secure-cookie:false}") boolean secure_cookie) {
        this.authentication_service = authentication_service;
        this.registration_service = registration_service;
        this.session_ttl_seconds = session_ttl_seconds;
        this.secure_cookie = secure_cookie;
    }

    @GetMapping("/csrf")
    public api_success_response<Map<String, String>> csrf_token(CsrfToken csrf_token) {
        return new api_success_response<>("CSRF token issued.", Map.of("token", csrf_token.getToken()));
    }

    @PostMapping("/registration-requests")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    public api_success_response<Void> submit_registration(
            @Valid @RequestBody registration_request_submission registration_request,
            HttpServletRequest request) {
        registration_service.submit(registration_request, correlation_id(request));
        return new api_success_response<>("Registration request submitted for review.", null);
    }

    @PostMapping("/login")
    public api_success_response<auth_user_response> login(
            @Valid @RequestBody login_request login_request,
            HttpServletRequest request,
            HttpServletResponse response) {
        login_result login_result = authentication_service
                .login(login_request.username(), login_request.password(), correlation_id(request))
                .orElseThrow(invalid_credentials_exception::new);

        ResponseCookie session_cookie = ResponseCookie.from(
                        session_authentication_filter.session_cookie_name,
                        login_result.session_secret())
                .httpOnly(true)
                .secure(secure_cookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(session_ttl_seconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, session_cookie.toString());
        return new api_success_response<>("Login successful.", login_result.user());
    }

    @GetMapping("/me")
    public api_success_response<auth_user_response> current_user(
            @AuthenticationPrincipal authenticated_user authenticated_user) {
        auth_user_response user = new auth_user_response(
                authenticated_user.user_id(),
                authenticated_user.username(),
                authenticated_user.permission_codes(),
                authenticated_user.super_admin());
        return new api_success_response<>("Session retrieved successfully.", user);
    }

    @PostMapping("/logout")
    public api_success_response<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal authenticated_user actor) {
        String raw_secret = read_session_secret(request);
        int revoked_count = authentication_service.revoke_secret(raw_secret, actor.user_id(), correlation_id(request));
        ResponseCookie expired_cookie = ResponseCookie.from(
                        session_authentication_filter.session_cookie_name,
                        "")
                .httpOnly(true)
                .secure(secure_cookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired_cookie.toString());
        if (revoked_count > 0) {
            logger.info("Đã thu hồi phiên đăng nhập; số phiên={}, actor_user_id={}", revoked_count, actor.user_id());
        }
        return new api_success_response<>("Logout successful.", null);
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }

    private String read_session_secret(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (session_authentication_filter.session_cookie_name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
