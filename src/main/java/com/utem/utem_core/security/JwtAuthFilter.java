package com.utem.utem_core.security;

import com.utem.utem_core.entity.User;
import com.utem.utem_core.repository.ProjectMemberRepository;
import com.utem.utem_core.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ProjectMemberRepository projectMemberRepository;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!securityEnabled) {
            chain.doFilter(request, response);
            return;
        }

        // Skip if already authenticated by ApiKeyAuthFilter
        if (UserContextHolder.get() != null) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.validateToken(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String roleStr = claims.get("role", String.class);
            List<?> rawProjectIds = claims.get("projectIds", List.class);

            User.Role role = User.Role.valueOf(roleStr);
            List<String> projectIds = rawProjectIds == null ? null
                    : rawProjectIds.stream().map(Object::toString).toList();

            UserContextHolder.set(new AuthenticatedUser(userId, username, role, projectIds));
        } catch (ExpiredJwtException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "TokenExpired");
            return;
        } catch (JwtException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "InvalidToken");
            return;
        } finally {
            // clear is done after controller — see below
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private void writeError(HttpServletResponse response, int status, String error) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + error + "\"}");
    }
}
