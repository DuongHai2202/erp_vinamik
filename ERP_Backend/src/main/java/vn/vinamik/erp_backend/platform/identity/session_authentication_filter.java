package vn.vinamik.erp_backend.platform.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class session_authentication_filter extends OncePerRequestFilter {
    public static final String session_cookie_name = "erp_session";
    private final identity_authentication_service authentication_service;

    public session_authentication_filter(identity_authentication_service authentication_service) {
        this.authentication_service = authentication_service;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filter_chain) throws ServletException, IOException {
        String session_secret = read_session_secret(request);
        if (session_secret != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authentication_service.authenticate_secret(session_secret).ifPresent(user -> {
                var authorities = user.permission_codes().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filter_chain.doFilter(request, response);
    }

    private String read_session_secret(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (session_cookie_name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}