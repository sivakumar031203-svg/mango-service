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

                        // ── Auth ─────────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,  "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET,   "/api/auth/verify").permitAll()

                        // ── Mangoes (public read) ─────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,   "/api/mangoes").permitAll()
                        .requestMatchers(HttpMethod.GET,   "/api/mangoes/**").permitAll()

                        // ── Orders (public place + track) ─────────────────────────────
                        .requestMatchers(HttpMethod.POST,  "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.GET,   "/api/orders/track/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/payment").permitAll()

                        // ── Payments (fully public — Razorpay flow) ───────────────────
                        .requestMatchers(HttpMethod.POST,  "/api/payments/**").permitAll()

                        // ── Reviews (public read + submit) ────────────────────────────
                        .requestMatchers(HttpMethod.GET,   "/api/reviews/mango/**").permitAll()
                        .requestMatchers(HttpMethod.POST,  "/api/reviews").permitAll()

                        // ── Coupons (public validate only) ────────────────────────────
                        .requestMatchers(HttpMethod.POST,  "/api/coupons/validate").permitAll()

                        // ── Settings (public read) ────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,   "/api/settings/public").permitAll()

                        // ── Static uploads ────────────────────────────────────────────
                        .requestMatchers("/uploads/**").permitAll()

                        // ── Admin notifications ───────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,   "/api/notifications/stream").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/notifications/unread-count").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,  "/api/notifications/mark-read").hasRole("ADMIN")

                        // ── Everything else is ADMIN only ─────────────────────────────
                        .anyRequest().hasRole("ADMIN")
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