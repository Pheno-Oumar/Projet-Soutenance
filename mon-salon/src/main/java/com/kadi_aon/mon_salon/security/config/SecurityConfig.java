package com.kadi_aon.mon_salon.security.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.kadi_aon.mon_salon.security.jwt.CustomAccessDeniedHandler;
import com.kadi_aon.mon_salon.security.jwt.JwtAuthenticationEntryPoint;
import com.kadi_aon.mon_salon.security.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques d'authentification et Swagger (fonctionne avec ou sans context-path)
                        .requestMatchers(
                                "/auth/**",
                                "/*/auth/login",
                                "/api/v1/mon-salon/auth/**",
                                "/api/v1/mon-salon/*/auth/login",
                                "/*/check",
                                "/api/v1/mon-salon/*/check",
                                "/explore/**",
                                "/api/v1/mon-salon/explore/**",
                                "/kadys/**",
                                "/api/v1/mon-salon/kadys/**",
                                "/*/vitrine/**",
                                "/api/v1/mon-salon/*/vitrine/**",
                                "/*/client/register",
                                "/api/v1/mon-salon/*/client/register",
                                "/*/auth/register",
                                "/api/v1/mon-salon/*/auth/register",
                                "/assistant/**",
                                "/api/v1/mon-salon/assistant/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()


                        // Route administrateur système
                        .requestMatchers("/admin-systeme/**", "/api/v1/mon-salon/admin-systeme/**").hasRole("ADMIN_SYSTEME")

                        // Routes protégées par rôle et salon (Cumul fluide)
                        .requestMatchers("/api/v1/mon-salon/{slugSalon}/proprietaire/**")
                        .access((authentication, context) ->
                                new org.springframework.security.authorization.AuthorizationDecision(
                                        context.getRequest().getAttribute("salonSecurityService") != null || true
                                )
                        ) // Les contrôleurs utilisent @PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, '...')")

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
