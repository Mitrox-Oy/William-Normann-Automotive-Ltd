package com.ecommerse.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        System.out.println("=== JWT VALIDATION DEBUG ===");
        System.out.println("Token length: " + (authToken != null ? authToken.length() : "null"));
        System.out.println("Token starts with: "
                + (authToken != null && authToken.length() > 20 ? authToken.substring(0, 20) + "..." : "invalid"));
        System.out.println("JWT Secret length: " + jwtSecret.length());
        System.out
                .println("JWT Secret starts with: " + jwtSecret.substring(0, Math.min(10, jwtSecret.length())) + "...");

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken)
                    .getBody();

            System.out.println("JWT validation successful");
            System.out.println("Token subject: " + claims.getSubject());
            System.out.println("Token issued at: " + claims.getIssuedAt());
            System.out.println("Token expires at: " + claims.getExpiration());
            System.out.println("Current time: " + new Date());

            return true;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            System.out.println("JWT ERROR: Invalid signature - " + e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            System.out.println("JWT ERROR: Malformed token - " + e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
            System.out.println("JWT ERROR: Token expired - " + e.getMessage());
            System.out.println(
                    "Token was valid from " + e.getClaims().getIssuedAt() + " to " + e.getClaims().getExpiration());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            System.out.println("JWT ERROR: Unsupported token - " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
            System.out.println("JWT ERROR: Empty claims - " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected JWT error: {}", e.getMessage());
            System.out.println("JWT ERROR: Unexpected error - " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("JWT validation failed");
        return false;
    }
}
