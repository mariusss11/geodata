package com.geodata.security.jwt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class JwtUtils {

    private static final long EXPIRATION_TIME = 10L * 24 * 60 * 60 * 1000 ; //10 days in millisecond
    private SecretKey key;

    @Value("${secretsJwtString}")
    private String secretsJwtString;

    @PostConstruct
    private void init() {
        byte[] keyBytes = secretsJwtString.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String  generateToken(String username){
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token){
        return extractClaims(token, Claims::getSubject);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsTFunction){
        return claimsTFunction.apply(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && isTokenExpired(token));
    }


    /**
     * Validates if the token is expired or not
     *
     * @param token the jwt token passed through the request
     * @return true is the token already expired
     */
    public boolean isTokenExpired(String token) {
        return !extractClaims(token, Claims::getExpiration).before(new Date());
    }


    public List<SimpleGrantedAuthority> getAuthorities(String token) {
        Claims claims = extractClaims(token, Function.identity());
        Object rolesObject = claims.get("roles"); // or "authorities", depending on how you name the claim

        if (rolesObject instanceof String roleString) {
            return List.of(new SimpleGrantedAuthority(roleString));
        } else if (rolesObject instanceof List<?> rolesList) {
            return rolesList.stream()
                    .filter(String.class::isInstance)
                    .map(role -> new SimpleGrantedAuthority((String) role))
                    .toList();
        }

        return List.of(); // return empty if no roles
    }

    public String getToken(String fullToken) {
        if (fullToken != null && fullToken.startsWith("Bearer ")) {
            return fullToken.substring(7);
        }
        return null;
    }

}