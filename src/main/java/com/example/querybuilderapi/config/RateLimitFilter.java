package com.example.querybuilderapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> windowStartTimes = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 100; // Max requests per window
    private static final long WINDOW_SIZE_MS = 60000; // 1 minute window

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIP = getClientIP(request);

        long currentTime = System.currentTimeMillis();
        windowStartTimes.putIfAbsent(clientIP, currentTime);

        long windowStart = windowStartTimes.get(clientIP);
        if (currentTime - windowStart > WINDOW_SIZE_MS) {
            // Reset window
            requestCounts.put(clientIP, new AtomicInteger(0));
            windowStartTimes.put(clientIP, currentTime);
        }

        AtomicInteger count = requestCounts.computeIfAbsent(clientIP, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > MAX_REQUESTS) {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}