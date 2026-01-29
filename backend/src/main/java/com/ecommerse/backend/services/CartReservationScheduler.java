package com.ecommerse.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CartReservationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CartReservationScheduler.class);

    private final CartService cartService;

    public CartReservationScheduler(CartService cartService) {
        this.cartService = cartService;
    }

    @Scheduled(fixedDelayString = "${cart.reservation.cleanup-ms:300000}")
    public void releaseExpiredReservations() {
        int released = cartService.releaseExpiredReservations();
        if (released > 0) {
            logger.info("Released {} expired cart reservations", released);
        }
    }
}
