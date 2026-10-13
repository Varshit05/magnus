package tech.vaibhavlachhwani.magnus.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for generating and validating JWT tokens.
 */
@Component
public class JwtUtil {

    // In a real app you would externalise this secret.
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expirationMs = 24 * 60 * 60 * 1000; // 24h

    public String generateToken(String username, Set<tech.vaibhavlachhwani.magnus.model.Role> roles) {
        String roleClaims = roles.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roleClaims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Set<tech.vaibhavlachhwani.magnus.model.Role> extractRoles(String token) {
        String roles = (String) Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles");
        return Set.of(roles.split(",")).stream()
                .map(tech.vaibhavlachhwani.magnus.model.Role::valueOf)
                .collect(Collectors.toSet());
    }

    public boolean isTokenValid(String token, String username) {
        try {
            String tokenUsername = extractUsername(token);
            return tokenUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }
}
