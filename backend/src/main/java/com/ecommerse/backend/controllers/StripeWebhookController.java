package com.ecommerse.backend.controllers;

import com.ecommerse.backend.services.OrderService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling Stripe webhook events
 */
@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);
    private final OrderService orderService;

    public StripeWebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Handle Stripe webhooks for payment events.
     * POST /api/stripe/webhook
     * 
     * Events handled:
     * - checkout.session.completed: Mark order as PAID and trigger fulfillment
     * 
     * @param sigHeader Stripe-Signature header for verification
     * @param payload   Raw request body
     * @return 200 OK if processed, 400 if signature invalid, 500 on error
     */
    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("Stripe-Signature") String sigHeader,
            @RequestBody String payload) {

        // Get webhook secret from environment
        String webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET");
        if (webhookSecret == null || webhookSecret.isBlank()) {
            logger.error("STRIPE_WEBHOOK_SECRET environment variable is not set");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }

        // Verify webhook signature
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        } catch (Exception e) {
            logger.error("Failed to parse Stripe webhook event: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }

        // Handle event
        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentIntentFailure(event);
                    break;

                case "charge.refunded":
                    // Optional: handle refunds
                    logger.info("Charge refunded event received: {}", event.getId());
                    break;

                default:
                    logger.debug("Unhandled webhook event type: {}", event.getType());
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing webhook event {}: {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }

        return ResponseEntity.ok("");
    }

    /**
     * Handle checkout.session.completed event.
     * Marks order as PAID and triggers fulfillment (emails, stock decrement).
     */
    private void handleCheckoutSessionCompleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (!deserializer.getObject().isPresent()) {
            logger.error("Unable to deserialize checkout session from event");
            return;
        }

        Object obj = deserializer.getObject().get();
        if (!(obj instanceof Session)) {
            logger.error("Unexpected object type for checkout.session.completed: {}", obj.getClass().getName());
            return;
        }

        Session session = (Session) obj;
        String orderId = session.getMetadata() != null ? session.getMetadata().get("orderId") : null;
        String paymentIntentId = session.getPaymentIntent();

        if (orderId == null || orderId.isBlank()) {
            logger.error("Missing orderId in checkout session metadata: {}", session.getId());
            return;
        }

        // Idempotent check: skip if already processed
        if (orderService.isProcessed(orderId)) {
            logger.info("Order {} already processed, skipping", orderId);
            return;
        }

        // Mark order as paid and trigger fulfillment
        try {
            orderService.markPaid(orderId, paymentIntentId);
            logger.info("Order {} marked as PAID via webhook (session: {}, payment_intent: {})",
                    orderId, session.getId(), paymentIntentId);
        } catch (Exception e) {
            logger.error("Failed to mark order {} as paid: {}", orderId, e.getMessage(), e);
            throw e; // Re-throw so webhook returns 500 and Stripe retries
        }
    }

    private void handlePaymentIntentFailure(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (!deserializer.getObject().isPresent()) {
            logger.error("Unable to deserialize payment intent from event {}", event.getId());
            return;
        }

        Object obj = deserializer.getObject().get();
        if (!(obj instanceof PaymentIntent intent)) {
            logger.error("Unexpected object type for payment_intent.payment_failed: {}", obj.getClass().getName());
            return;
        }

        String orderId = intent.getMetadata() != null ? intent.getMetadata().get("orderId") : null;

        if (orderId == null || orderId.isBlank()) {
            logger.warn("Payment intent {} missing order metadata; skipping failure handling", intent.getId());
            return;
        }

        try {
            orderService.markPaymentFailed(orderId, intent.getId());
            logger.info("Order {} marked as FAILED via webhook (payment_intent: {})", orderId, intent.getId());
        } catch (Exception ex) {
            logger.error("Failed to mark order {} as FAILED: {}", orderId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
