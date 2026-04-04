package com.siva.mango_bt.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public auth endpoints ────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/admin/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/seller/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/customer/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/customer/register").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/auth/verify").permitAll()

                        // ── FIX Bug 3: change-password MUST come before the /sellers/** wildcard ──
                        // Sellers change their own password via a POST to /auth (not /sellers)
                        .requestMatchers(HttpMethod.POST, "/api/auth/seller/change-password")
                        .hasRole("SELLER")

                        // ── Mangoes (public read) ────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,  "/api/mangoes").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/mangoes/**").permitAll()

                        // Mango mutations — seller or super admin
                        .requestMatchers(HttpMethod.POST,   "/api/mangoes")
                        .hasAnyRole("SUPER_ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PUT,    "/api/mangoes/**")
                        .hasAnyRole("SUPER_ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PATCH,  "/api/mangoes/**")
                        .hasAnyRole("SUPER_ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/mangoes/**")
                        .hasAnyRole("SUPER_ADMIN", "SELLER")

                        // ── Orders (public place + track) ────────────────────────────
                        .requestMatchers(HttpMethod.POST,  "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.GET,   "/api/orders/track/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/payment").permitAll()

                        // ── Payments (fully public — Razorpay flow) ──────────────────
                        .requestMatchers(HttpMethod.POST, "/api/payments/**").permitAll()

                        // ── Reviews (public read + submit) ───────────────────────────
                        .requestMatchers(HttpMethod.GET,  "/api/reviews/mango/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reviews").permitAll()

                        // ── Coupons (public validate only) ───────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/coupons/validate").permitAll()

                        // ── Settings (public read) ───────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/settings/public").permitAll()

                        // ── Static uploads ───────────────────────────────────────────
                        .requestMatchers("/uploads/**").permitAll()

                        // ── Public seller endpoints ──────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/sellers/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sellers/*/public").permitAll()

                        // ── FIX Bug 6: SSE stream is permitAll — auth is validated inside
                        // NotificationService via the ?token= param processed by JwtAuthFilter
                        // (the filter already sets the SecurityContext before the controller runs)
                        .requestMatchers(HttpMethod.GET, "/api/notifications/stream").permitAll()

                        // ── FIX Bug 2: Notifications use SUPER_ADMIN or SELLER (not ADMIN)
                        // Both admin dashboard and seller dashboard poll unread-count
                        .requestMatchers(HttpMethod.GET,  "/api/notifications/unread-count")
                        .hasAnyRole("SUPER_ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/notifications/mark-read")
                        .hasAnyRole("SUPER_ADMIN", "SELLER")

                        // ── Sellers management — SUPER_ADMIN only ───────────────────
                        // NOTE: This wildcard comes AFTER the more-specific /active and /*/public
                        // rules above, so those public endpoints are not caught here.
                        .requestMatchers("/api/sellers/**").hasRole("SUPER_ADMIN")

                        // ── Orders management — seller or super admin ────────────────
                        .requestMatchers(HttpMethod.GET,   "/api/orders").hasAnyRole("SUPER_ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.GET,   "/api/orders/**").hasAnyRole("SUPER_ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/**").hasAnyRole("SUPER_ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.GET,   "/api/orders/stats/summary")
                        .hasAnyRole("SUPER_ADMIN", "SELLER")

                        // ── FIX Bug 7: anyRequest uses SUPER_ADMIN (matches what login generates)
                        .anyRequest().hasAnyRole("SUPER_ADMIN", "SELLER")
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}