package com.example.querybuilderapi.security;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * JWT authentication filter.
 * Extracts Bearer token from the Authorization header, validates it,
 * and sets the SecurityContext so downstream filters/controllers see
 * the authenticated principal.
 *
 * Registered as a @Bean in SecurityConfig (NOT @Component) so that
 * @DataJpaTest and other test slices don't accidentally pull it in.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthAccountRepository authAccountRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AuthAccountRepository authAccountRepository) {
        this.jwtService = jwtService;
        this.authAccountRepository = authAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No Bearer token → skip (let Spring Security handle it)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String email = jwtService.getEmailFromToken(jwt);

            // Only set context if not already authenticated
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<AuthAccount> accountOpt = authAccountRepository.findByEmail(email);

                if (accountOpt.isPresent()) {
                    AuthAccount account = accountOpt.get();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    account,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            // Invalid/expired token — clear context, let request proceed (will get 401)
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT filter for public auth endpoints
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/oauth2");
    }
}
