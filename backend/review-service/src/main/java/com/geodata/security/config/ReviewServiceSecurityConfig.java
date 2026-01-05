package com.geodata.security.config;

import com.geodata.security.filter.ClientServiceAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class ReviewServiceSecurityConfig {

    private final ClientServiceAuthFilter jwtAuthFilter;
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String LIBRARIAN_ROLE = "LIBRARIAN";
    private static final String USER_ROLE = "USER";

    private static final String DEFAULT_REVIEW_ENDPOINT = "/api/review/**";

    @Autowired
    public ReviewServiceSecurityConfig(ClientServiceAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(HttpMethod.GET, DEFAULT_REVIEW_ENDPOINT).hasAnyAuthority(ADMIN_ROLE, LIBRARIAN_ROLE, USER_ROLE)

                        .requestMatchers(HttpMethod.POST, DEFAULT_REVIEW_ENDPOINT).hasAnyAuthority(ADMIN_ROLE, LIBRARIAN_ROLE, USER_ROLE)

                        .requestMatchers(HttpMethod.PUT, "/api/review/librarian/**").hasAnyAuthority(ADMIN_ROLE, LIBRARIAN_ROLE)
                        .requestMatchers(HttpMethod.PUT, DEFAULT_REVIEW_ENDPOINT).hasAnyAuthority(USER_ROLE)

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .anyRequest().denyAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
