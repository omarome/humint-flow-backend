package com.example.querybuilderapi.config;

import com.example.querybuilderapi.security.JwtAuthenticationFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables authentication for tests.
 * The @Primary annotation ensures this takes precedence over the main SecurityConfig.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    public TestSecurityConfig() {
        super();
    }


    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * Provide a no-op JwtAuthenticationFilter to satisfy any bean references
     * without needing the full JwtService / AuthAccountRepository graph.
     */
    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(null, null);
    }
}