package com.ecommerse.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Provides static resource handling for uploaded assets (e.g. product images).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.public-url-prefix:/uploads}")
    private String publicUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String handlerPattern = normalizeHandlerPattern(publicUrlPrefix);
        String resourceLocation = toFileResourceLocation(uploadDir);

        registry.addResourceHandler(handlerPattern)
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);

        log.debug("Configured static resource handler: {} -> {}", handlerPattern, resourceLocation);
    }

    private String normalizeHandlerPattern(String prefix) {
        String sanitized = StringUtils.hasText(prefix) ? prefix.trim() : "/uploads";
        if (!sanitized.startsWith("/")) {
            sanitized = "/" + sanitized;
        }
        if (!sanitized.endsWith("/")) {
            sanitized = sanitized + "/";
        }
        return sanitized + "**";
    }

    private String toFileResourceLocation(String directory) {
        Path path = Path.of(directory).toAbsolutePath();
        String uri = path.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
