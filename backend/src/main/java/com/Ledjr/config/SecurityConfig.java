package com.Ledjr.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

/**
 * who's allowed to hit what.
 * login = a session cookie (Spring Security + Spring Session, stored in Postgres).
 * every endpoint needs you logged in except signing in / registering / grabbing a CSRF token
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.frontend-origin}")
    private String frontendOrigin;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // uses the corsConfigurationSource bean below
            .cors(Customizer.withDefaults())
            // CSRF on (the default). token lives in the session, frontend gets it from GET /csrf
            // and sends it back in a header on every POST/PUT/DELETE
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/signin", "/register", "/csrf").permitAll()
                // forgot password: you're not signed in, that's the whole point
                .requestMatchers("/password/forgot", "/password/reset", "/password/reset/check").permitAll()
                .anyRequest().authenticated())
            // not logged in = plain 401, not a redirect to some login page (React handles that)
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // POST /signout kills the session. 204 instead of the default redirect
            .logout(logout -> logout
                .logoutUrl("/signout")
                .logoutSuccessHandler((request, response, auth) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
        return http.build();
    }

    /** replaces the @CrossOrigin on every controller. allowCredentials = the browser can send the session cookie */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
