package com.ecommerse.backend.entities;

/**
 * Enumeration representing the various states of an order
 */
public enum OrderStatus {
    /**
     * Order has been created but not yet confirmed
     */
    PENDING,

    /**
     * Order has been confirmed and is being processed
     */
    CONFIRMED,

    /**
     * Stripe Checkout Session has been created; awaiting payment
     */
    CHECKOUT_CREATED,

    /**
     * Order payment has been successfully processed
     */
    PAID,

    /**
     * Order payment failed or was declined
     */
    FAILED,

    /**
     * Order is being prepared for shipment
     */
    PROCESSING,

    /**
     * Order has been shipped to the customer
     */
    SHIPPED,

    /**
     * Order has been delivered to the customer
     */
    DELIVERED,

    /**
     * Order has been cancelled
     */
    CANCELLED,

    /**
     * Order has been refunded
     */
    REFUNDED
}
