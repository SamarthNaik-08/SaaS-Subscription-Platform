package com.saasplatform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        RateLimitConfig config = resolveRateLimitConfig(path, request.getMethod());

        if (config != null) {
            String bucketKey = config.groupName + ":" + clientIp;
            boolean allowed = rateLimiterService.tryAcquire(bucketKey, config.capacity, config.refillRatePerMinute);

            if (!allowed) {
                log.warn("Rate limit exceeded for clientIp={}, path={}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", "60");

                ApiResponse<Void> errorResponse = ApiResponse.error(
                        "Rate limit exceeded. Please try again in a minute.",
                        "TOO_MANY_REQUESTS"
                );
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitConfig resolveRateLimitConfig(String path, String method) {
        if (path.startsWith("/api/v1/auth/login")) {
            return new RateLimitConfig("auth_login", 15, 15);
        } else if (path.startsWith("/api/v1/auth/register")) {
            return new RateLimitConfig("auth_register", 10, 10);
        } else if (path.startsWith("/api/v1/auth/refresh")) {
            return new RateLimitConfig("auth_refresh", 30, 30);
        } else if (path.startsWith("/api/v1/users/me/change-password")) {
            return new RateLimitConfig("change_password", 5, 5);
        } else if (path.startsWith("/api/v1/billing/orders/create")) {
            return new RateLimitConfig("order_create", 20, 20);
        } else if (path.startsWith("/api/v1/billing/orders/verify")) {
            return new RateLimitConfig("order_verify", 20, 20);
        } else if (path.startsWith("/api/v1/billing/webhook")) {
            return new RateLimitConfig("billing_webhook", 60, 60);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record RateLimitConfig(String groupName, int capacity, int refillRatePerMinute) {}
}
