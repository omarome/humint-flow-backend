package com.example.querybuilderapi.config;

import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.security.FirebaseTokenFilter;
import com.example.querybuilderapi.security.JwtAuthenticationFilter;
import com.example.querybuilderapi.security.JwtService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import com.example.querybuilderapi.service.AuthService;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.dto.AuthResponse;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import com.example.querybuilderapi.security.HttpCookieOAuth2AuthorizationRequestRepository;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5174}")
    private String allowedOrigins;

    @Value("${app.frontend.url:http://localhost:5174}")
    private String frontendUrl;

    private final AuthService authService;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    public SecurityConfig(AuthService authService,
            HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository) {
        this.authService = authService;
        this.cookieAuthorizationRequestRepository = cookieAuthorizationRequestRepository;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService, AuthAccountRepository authAccountRepository) {
        return new JwtAuthenticationFilter(jwtService, authAccountRepository);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            JwtAuthenticationFilter jwtAuthFilter,
            FirebaseTokenFilter firebaseTokenFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public auth endpoints
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/oauth2/**")
                        .permitAll()
                        // Health check for Docker, Cloud Run, and Render keep-alive
                        .requestMatchers("/api/health", "/actuator/health").permitAll()
                        // WebSocket SockJS handshake endpoints
                        .requestMatchers("/ws/**").permitAll()
                        // All other /api/** require authentication
                        .requestMatchers("/api/**").authenticated()
                        // Everything else (static, etc.) is open
                        .anyRequest().permitAll())
                // OAuth2 Login configuration
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                        .successHandler(oauth2SuccessHandler()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Firebase token filter runs first — it also sets SecurityContext
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // Then legacy JWT filter (no-op if SecurityContext already populated)
                .addFilterBefore(jwtAuthFilter, FirebaseTokenFilter.class)

                // Security headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(cto -> {
                        })
                        .referrerPolicy(rp -> rp
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String sub = oAuth2User.getAttribute("sub"); // Google unique ID
            String picture = oAuth2User.getAttribute("picture"); // Google profile picture

            AuthResponse authResponse = authService.handleOAuthLogin(
                    email, name, AuthAccount.OAuthProvider.GOOGLE, sub, picture);

            // Redirect back to frontend with tokens as URL parameters
            // The frontend will parse these and log the user in
            String targetUrl = String.format("%s/login-success?accessToken=%s&refreshToken=%s",
                    frontendUrl,
                    authResponse.getAccessToken(),
                    authResponse.getRefreshToken());

            response.sendRedirect(targetUrl);
        };
    }
}