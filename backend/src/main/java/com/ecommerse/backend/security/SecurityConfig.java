package com.ecommerse.backend.security;

import com.ecommerse.backend.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless JWT API
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - must be first
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/owner/login").permitAll()
                        .requestMatchers("/api/checkout/**", "/api/stripe/webhook").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/images/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // Actuator: keep only basic health/info public (for uptime checks). Everything else must be protected.
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Public read access to products and categories
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        // Product and Category write operations - require authentication
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole("OWNER", "ADMIN")
                        // Customer endpoints
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/auth/change-password").authenticated()
                        // File management - only OWNER and ADMIN
                        .requestMatchers("/api/files/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/wishlist/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/shipping/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "OWNER", "ADMIN")
                        // Test endpoints
                        .requestMatchers("/api/test/user").hasAnyRole("CUSTOMER", "OWNER", "ADMIN")
                        .requestMatchers("/api/test/customer").hasRole("CUSTOMER")
                        .requestMatchers("/api/test/owner").hasAnyRole("OWNER", "ADMIN")
                        // Owner/Admin endpoints
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers("/api/owner/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/analytics/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/reports/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/alerts/**").hasAnyRole("OWNER", "ADMIN")
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (origins.isEmpty()) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            // Merge provided origins with common localhost dev patterns and configured
            // frontend URL
            List<String> merged = new java.util.ArrayList<>(origins);

            String normalizedFrontend = normalizeFrontendBaseUrl();
            addIfMissing(merged, normalizedFrontend);
            if (normalizedFrontend.contains("localhost")) {
                addIfMissing(merged, normalizedFrontend.replace("localhost", "127.0.0.1"));
            }

            for (String def : List.of(
                    "http://localhost:4200",
                    "http://127.0.0.1:4200",
                    "http://localhost:3000",
                    "http://127.0.0.1:3000")) {
                addIfMissing(merged, def);
            }
            configuration.setAllowedOriginPatterns(merged);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private void addIfMissing(List<String> origins, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        if (!origins.contains(candidate)) {
            origins.add(candidate);
        }
    }

    private String normalizeFrontendBaseUrl() {
        String value = (frontendBaseUrl == null || frontendBaseUrl.isBlank())
                ? "http://localhost:4200"
                : frontendBaseUrl.trim();

        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
