package com.portfolio.backtester.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for split-deploy topologies. In local Docker compose the nginx container proxies
 * /api/ to the backend so the browser sees same-origin and CORS is a no-op —
 * but production needs explicit allow-list.
 *
 * Allowed origins are configured via the BACKTESTER_CORS_ORIGINS env var
 * (comma-separated). Wildcards are deliberately rejected: if you genuinely
 * want to allow everything, set BACKTESTER_CORS_ALLOWED_ORIGINS=* but be aware that
 * combined with credentials it's a footgun.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${backtester.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "Idempotency-Key")
                .exposedHeaders("Location", "Idempotency-Key")
                .allowCredentials(false)
                .maxAge(3600);

        // Actuator endpoints — read-only, safe to allow same origins
        registry.addMapping("/actuator/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET")
                .maxAge(3600);
    }
}
