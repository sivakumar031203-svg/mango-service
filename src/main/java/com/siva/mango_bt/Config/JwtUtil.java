package com.siva.mango_bt.Config;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String subject, String role, Long entityId) {
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("role", role, "entityId", entityId != null ? entityId : 0L))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // Keep backward compat
    public String generateToken(String username) {
        return generateToken(username, "ROLE_SUPER_ADMIN", null);
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    public Long extractEntityId(String token) {
        Object id = getClaims(token).get("entityId");
        if (id instanceof Integer) return ((Integer) id).longValue();
        if (id instanceof Long) return (Long) id;
        return null;
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}