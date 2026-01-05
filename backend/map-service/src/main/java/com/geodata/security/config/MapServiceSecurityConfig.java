package com.geodata.security.config;

import com.geodata.security.filter.MapServiceAuthFilter;
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
public class MapServiceSecurityConfig {

    private final MapServiceAuthFilter jwtAuthFilter;

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MANAGER_ROLE = "MANAGER";
    private static final String USER_ROLE = "USER";

    @Autowired
    public MapServiceSecurityConfig(MapServiceAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize


                        // request for managing maps (create)
                        .requestMatchers(HttpMethod.POST, "/api/maps/manager/**").hasAnyAuthority(ADMIN_ROLE, MANAGER_ROLE)

                        // request for management
                        .requestMatchers("/api/maps/manager/**").hasAnyAuthority(ADMIN_ROLE, MANAGER_ROLE)

                        // request for getting the maps
                        .requestMatchers(HttpMethod.GET, "/api/maps/**").hasAnyAuthority(ADMIN_ROLE, MANAGER_ROLE, USER_ROLE)


                        // request for getting all the categories available
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").hasAnyAuthority(ADMIN_ROLE, MANAGER_ROLE, USER_ROLE)

                        // request for adding new categories
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyAuthority(ADMIN_ROLE, MANAGER_ROLE)

                        // for api-docs
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()

                        .anyRequest().denyAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
