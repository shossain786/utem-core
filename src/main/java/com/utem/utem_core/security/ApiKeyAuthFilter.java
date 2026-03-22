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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * API key authentication filter for write endpoints.
 * <p>
 * When {@code utem.security.enabled=true}, all non-GET requests to {@code /utem/**}
 * must include a valid {@code X-API-Key} header that matches an active project.
 * GET requests are always allowed (dashboard reads).
 * Project management endpoints ({@code /utem/projects/**}) are always open.
 * <p>
 * When {@code utem.security.enabled=false} (default), the filter is a no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final Set<String> OPEN_PREFIXES = Set.of("/utem/projects");

    private final ProjectRepository projectRepository;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            if (!securityEnabled || isOpenRequest(request)) {
                chain.doFilter(request, response);
                return;
            }

            String apiKey = request.getHeader(API_KEY_HEADER);
            if (apiKey == null || apiKey.isBlank()) {
                sendUnauthorized(response, "Missing X-API-Key header");
                return;
            }

            Project project = projectRepository.findByApiKeyAndActiveTrue(apiKey).orElse(null);
            if (project == null) {
                sendUnauthorized(response, "Invalid or inactive API key");
                return;
            }

            ProjectContextHolder.set(project);
            chain.doFilter(request, response);
        } finally {
            ProjectContextHolder.clear();
        }
    }

    private boolean isOpenRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        // GET requests and project management are always open
        return "GET".equalsIgnoreCase(method)
                || OPEN_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}
