package vn.vinamik.erp_backend.platform.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import vn.vinamik.erp_backend.platform.common.api_error_writer;

@Configuration
@EnableMethodSecurity
public class security_configuration {
    @Bean
    public PasswordEncoder password_encoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public session_authentication_filter session_authentication_filter(
            identity_authentication_service authentication_service) {
        return new session_authentication_filter(authentication_service);
    }

    @Bean
    public SecurityFilterChain security_filter_chain(
            HttpSecurity http,
            session_authentication_filter session_filter,
            api_error_writer error_writer) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new spa_csrf_token_request_handler()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(request_cache -> request_cache.disable())
                .formLogin(form_login -> form_login.disable())
                .httpBasic(http_basic -> http_basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/login",
                                "/api/v1/auth/registration-requests",
                                "/error",
                                "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, auth_exception) ->
                                error_writer.write_error(request, response, 401,
                                        "AUTHENTICATION_REQUIRED", "Authentication is required."))
                        .accessDeniedHandler((request, response, access_exception) ->
                                error_writer.write_error(request, response, 403,
                                        "ACCESS_DENIED", "Access denied.")))
                .addFilterBefore(session_filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

