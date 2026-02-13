package com.ecommerse.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CheckoutExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutExpiryScheduler.class);

    private final OrderService orderService;
    private final int expiryMinutes;

    public CheckoutExpiryScheduler(
            OrderService orderService,
            @Value("${checkout.expiry.minutes:45}") int expiryMinutes) {
        this.orderService = orderService;
        this.expiryMinutes = expiryMinutes;
    }

    @Scheduled(fixedDelayString = "${checkout.expiry.cleanup-ms:300000}")
    public void expireStaleCheckouts() {
        int expired = orderService.expireStaleCheckouts(expiryMinutes);
        if (expired > 0) {
            logger.info("Expired {} stale checkout(s) older than {} minute(s)", expired, expiryMinutes);
        }
    }
}
