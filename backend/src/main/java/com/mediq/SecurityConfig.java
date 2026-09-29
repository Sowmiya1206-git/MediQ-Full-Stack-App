
package com.mediq;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.stereotype.Component;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


/**
 * JWT Authentication Filter
 */
@Component
class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwt;
    private final UserRepo users;

    JwtFilter(JwtUtil j, UserRepo u) {
        this.jwt = j;
        this.users = u;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain)
            throws ServletException, IOException {

        // Allow CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String h = req.getHeader("Authorization");

        if (h != null && h.startsWith("Bearer ")) {

            try {
                String email = jwt.subject(h.substring(7));

                users.findByEmail(email)
                        .filter(User::isActive)
                        .ifPresent(u -> {

                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            u.getEmail(),
                                            null,
                                            List.of(
                                                    new SimpleGrantedAuthority(
                                                            "ROLE_" + u.getRole()
                                                    )
                                            )
                                    );

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(auth);
                        });

            } catch (Exception ignored) {
                // Invalid token -> unauthenticated
            }
        }

        chain.doFilter(req, res);
    }
}


/**
 * Spring Security Configuration
 */
@Configuration
public class SecurityConfig {

    /**
     * Password Encoder
     */
    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * Security Filter Chain
     */
    @Bean
    SecurityFilterChain chain(
            HttpSecurity http,
            JwtFilter f) throws Exception {

        http
                // Disable CSRF for REST API
                .csrf(c -> c.disable())

                // Enable CORS configuration
                .cors(c -> c.configurationSource(cors()))

                // Allow H2 console frames
                .headers(h ->
                        h.frameOptions(o -> o.sameOrigin())
                )

                // Stateless JWT authentication
                .sessionManagement(s ->
                        s.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Authorization rules
                .authorizeHttpRequests(a -> a

                        // Allow browser CORS preflight requests
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                "/h2-console/**"
                        ).permitAll()

                        // Admin endpoints
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // Doctor endpoints
                        .requestMatchers(
                                "/api/doctor/**"
                        ).hasRole("DOCTOR")

                        // Patient endpoints
                        .requestMatchers(
                                "/api/patient/**"
                        ).hasRole("PATIENT")

                        // All remaining endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Authentication and authorization error handling
                .exceptionHandling(e ->
                        e
                                .authenticationEntryPoint((q, r, ex) -> {
                                    r.setStatus(401);
                                    r.setContentType("application/json");

                                    r.getWriter().write(
                                            "{\"message\":\"Authentication required\"}"
                                    );
                                })

                                .accessDeniedHandler((q, r, ex) -> {
                                    r.setStatus(403);
                                    r.setContentType("application/json");

                                    r.getWriter().write(
                                            "{\"message\":\"Access denied\"}"
                                    );
                                })
                )

                // Add JWT filter
                .addFilterBefore(
                        f,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }


    /**
     * CORS Configuration
     */
    @Bean
    CorsConfigurationSource cors() {

        CorsConfiguration c = new CorsConfiguration();

        // Allowed frontend origins
        c.setAllowedOrigins(
                List.of(
                        "http://localhost:4200",
                        "https://med-i-q-full-stack-app.vercel.app"
                )
        );

        // Allowed HTTP methods
        c.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Allow request headers, including Authorization
        c.setAllowedHeaders(
                List.of("*")
        );

        // Allow credentials
        c.setAllowCredentials(true);

        // Register CORS configuration for all API routes
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                c
        );

        return source;
    }
}