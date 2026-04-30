package com.geodata.security.jwt;


import com.geodata.security.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
public class JwtUtils {

    @Value("${secretsJwtString}")
    private String secreteJwtString;
    private SecretKey key;
    private static final long EXPIRATION_TIME = 10L * 24 * 60 * 60 * 1000 ; //10 days in millisecond

    private final CustomUserDetailsService customUserDetailsService;


    @Autowired
    public JwtUtils(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @PostConstruct
    private void init() {
        byte[] keyBytes = secreteJwtString.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String  generateToken(String username){
        log.info("Generating the jwt");

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // creating a roles map
        Map<String, Object> claims = new HashMap<>();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // setting the roles mpa in the jwt
        claims.put("roles", roles);

        log.info("The roles the user has: {}", claims.get("roles"));

        return Jwts.builder()
                .claims(claims)
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
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }

    public String getToken(String fullToken) {
        if (fullToken != null && fullToken.startsWith("Bearer ")) {
            return fullToken.substring(7);
        }
        return null;
    }
}