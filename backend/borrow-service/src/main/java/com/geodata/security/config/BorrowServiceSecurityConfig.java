package com.geodata.security.config;

import com.geodata.security.filter.BorrowServiceAuthFilter;
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
public class BorrowServiceSecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String LIBRARIAN_ROLE = "LIBRARIAN";
    private static final String USER_ROLE = "USER";
    private final BorrowServiceAuthFilter jwtAuthFilter;

    @Autowired
    public BorrowServiceSecurityConfig(BorrowServiceAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET,"/api/borrows/librarian/**").hasAnyAuthority(ADMIN_ROLE, LIBRARIAN_ROLE)
                        .requestMatchers(HttpMethod.POST,"/api/borrows/librarian/**").hasAnyAuthority(ADMIN_ROLE, LIBRARIAN_ROLE)
                        .requestMatchers(HttpMethod.POST,"/api/borrows/create").hasAuthority(USER_ROLE)
                        .requestMatchers(HttpMethod.POST,"/api/borrows/return").hasAuthority(USER_ROLE)
                        .requestMatchers(HttpMethod.POST,"/api/borrows/transfer").hasAuthority(USER_ROLE)

                        .requestMatchers(HttpMethod.GET,"/api/borrows/**").hasAnyAuthority(ADMIN_ROLE, LIBRARIAN_ROLE, USER_ROLE)
                        .requestMatchers(HttpMethod.GET,"/api/borrows/current").hasAnyAuthority(ADMIN_ROLE, LIBRARIAN_ROLE, USER_ROLE)

                        // TODO, double this method, and make one for the client, and one for the admin and librarian

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()

//                        .requestMatchers("/api/borrows/**").hasAuthority("ADMIN")

                        .anyRequest().denyAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
