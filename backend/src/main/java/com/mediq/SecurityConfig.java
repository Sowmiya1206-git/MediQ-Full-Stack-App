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


@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    SecurityFilterChain chain(
            HttpSecurity http,
            JwtFilter f) throws Exception {

        http
                .csrf(c -> c.disable())

                .cors(c -> c.configurationSource(cors()))

                .headers(h ->
                        h.frameOptions(o -> o.sameOrigin())
                )

                .sessionManagement(s ->
                        s.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(a -> a

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                "/h2-console/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/api/doctor/**"
                        ).hasRole("DOCTOR")

                        .requestMatchers(
                                "/api/patient/**"
                        ).hasRole("PATIENT")

                        .anyRequest().authenticated()
                )

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

                .addFilterBefore(
                        f,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }


    @Bean
    CorsConfigurationSource cors() {

        CorsConfiguration c = new CorsConfiguration();

        c.setAllowedOrigins(
                List.of("http://localhost:4200")
        );

        c.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        c.setAllowedHeaders(
                List.of("*")
        );

        c.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                c
        );

        return source;
    }
}