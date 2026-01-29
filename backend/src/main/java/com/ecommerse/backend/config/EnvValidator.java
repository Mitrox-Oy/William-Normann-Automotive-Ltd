package com.ecommerse.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvValidator {
    private static final Logger logger = LoggerFactory.getLogger(EnvValidator.class);

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @jakarta.annotation.PostConstruct
    public void check() {
        String stripeKey = System.getenv("STRIPE_SECRET_KEY");
        
        // Only enforce Stripe key in production
        if ("prod".equals(activeProfile)) {
            if (stripeKey == null || stripeKey.isBlank()) {
                logger.error("STRIPE_SECRET_KEY missing in production environment");
                throw new IllegalStateException("STRIPE_SECRET_KEY is required in production");
            }
        } else {
            // In dev/docker, just warn if missing
            if (stripeKey == null || stripeKey.isBlank()) {
                logger.warn("STRIPE_SECRET_KEY not set - Stripe payments will not work");
            } else {
                logger.info("STRIPE_SECRET_KEY is configured");
            }
        }
    }
}



