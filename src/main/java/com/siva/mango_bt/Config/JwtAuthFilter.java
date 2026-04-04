package com.siva.mango_bt.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = null;

        // Read from Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // Fallback: ?token= query param (for SSE streams that can't set headers)
        if (token == null) {
            token = request.getParameter("token");
        }

        if (token != null) {
            try {
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);

                    // ── FIX: read actual role from token, not hardcoded ──────────
                    String role = jwtUtil.extractRole(token);
                    // Normalise: role in token is stored as "ROLE_X" already.
                    // SimpleGrantedAuthority needs the full "ROLE_X" string.
                    if (role == null || role.isBlank()) {
                        // Fallback safety: deny auth if no role present
                        filterChain.doFilter(request, response);
                        return;
                    }
                    // Ensure it always starts with ROLE_ (defensive)
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                    if (username != null) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        List.of(new SimpleGrantedAuthority(authority))
                                );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
                // Invalid/expired token → do nothing, let Spring Security decide
            } catch (Exception e) {
                // Never let a bad token crash the filter chain
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}