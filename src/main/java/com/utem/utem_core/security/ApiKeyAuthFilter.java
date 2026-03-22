package com.utem.utem_core.security;

import com.utem.utem_core.entity.Project;
import com.utem.utem_core.repository.ProjectRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API key authentication filter for reporter write endpoints.
 * <p>
 * When {@code utem.security.enabled=true}, non-GET requests to {@code /utem/**} that
 * include an {@code X-API-Key} header are resolved to a project and allowed through.
 * Auth endpoints ({@code /utem/auth/**}) are always open.
 * <p>
 * When {@code utem.security.enabled=false} (default), the filter is a no-op.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ProjectRepository projectRepository;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            if (!securityEnabled) {
                chain.doFilter(request, response);
                return;
            }

            // Auth endpoints are always open
            if (request.getRequestURI().startsWith("/utem/auth/")) {
                chain.doFilter(request, response);
                return;
            }

            String apiKey = request.getHeader(API_KEY_HEADER);
            if (apiKey != null && !apiKey.isBlank()) {
                Project project = projectRepository.findByApiKeyAndActiveTrue(apiKey).orElse(null);
                if (project != null) {
                    ProjectContextHolder.set(project);
                    // Set a synthetic user for downstream filters
                    UserContextHolder.set(new AuthenticatedUser(
                            "api-key-" + project.getId(),
                            "api-key",
                            com.utem.utem_core.entity.User.Role.MEMBER,
                            java.util.List.of(project.getId())
                    ));
                } else {
                    sendUnauthorized(response, "Invalid or inactive API key");
                    return;
                }
            }

            chain.doFilter(request, response);
        } finally {
            ProjectContextHolder.clear();
            // Note: UserContextHolder is cleared by JwtAuthFilter if it was set there;
            // if we set it here, clear it here too
            if (request.getHeader(API_KEY_HEADER) != null) {
                UserContextHolder.clear();
            }
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}
