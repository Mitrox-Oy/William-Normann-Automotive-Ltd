package com.ecommerse.backend.services.payments;

import com.ecommerse.backend.dto.OrderDTO;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import com.stripe.param.PaymentIntentCreateParams;

import com.stripe.net.RequestOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

/**
 * Service for integrating with Stripe payment processing
 */
@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${STRIPE_SECRET_KEY:sk_test_placeholder}")
    private String stripeSecretKey;

    @Value("${frontend.base.url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${checkout.success.path:/checkout/success}")
    private String successPath;

    @Value("${checkout.cancel.path:/checkout/cancel}")
    private String cancelPath;

    @PostConstruct
    public void init() {
        if (stripeSecretKey == null || stripeSecretKey.isEmpty() || stripeSecretKey.equals("sk_test_placeholder")) {
            logger.warn(
                    "Stripe secret key is not configured. Payment processing will fail. Please set STRIPE_SECRET_KEY environment variable.");
        } else {
            Stripe.apiKey = stripeSecretKey;
            logger.info("Stripe service initialized");
        }
        logger.info("Stripe Checkout URLs: success={}, cancel={}", getSuccessUrlTemplate(), getCancelUrl());
    }

    private String getSuccessUrlTemplate() {
        String base = frontendBaseUrl != null ? frontendBaseUrl.replaceAll("/$", "") : "http://localhost:3000";
        String path = successPath != null ? successPath : "/checkout/success";
        return base + path + "?session_id={CHECKOUT_SESSION_ID}";
    }

    private String getCancelUrl() {
        String base = frontendBaseUrl != null ? frontendBaseUrl.replaceAll("/$", "") : "http://localhost:3000";
        String path = cancelPath != null ? cancelPath : "/checkout/cancel";
        return base + path;
    }

    /**
     * Create a PaymentIntent for an order
     *
     * @param amount        Amount in cents (e.g., $10.00 = 1000 cents)
     * @param currency      Currency code (e.g., "usd")
     * @param orderId       Order ID for metadata
     * @param customerEmail Customer email for metadata
     * @return PaymentIntent client secret
     * @throws StripeException if Stripe API call fails
     */
    public String createPaymentIntent(Long amount, String currency, Long orderId, String customerEmail)
            throws StripeException {

        if (stripeSecretKey == null || stripeSecretKey.isEmpty() || stripeSecretKey.equals("sk_test_placeholder")) {
            throw new IllegalArgumentException(
                    "Stripe API key is not configured. Please set STRIPE_SECRET_KEY environment variable.");
        }

        logger.debug("Creating payment intent for order {} with amount {} {}", orderId, amount, currency);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency != null ? currency.toLowerCase() : "usd")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods
                                .builder()
                                .setEnabled(true)
                                .build())
                .putMetadata("orderId", orderId.toString())
                .putMetadata("customerEmail", customerEmail)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                .build();

        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey("pi_create_" + orderId)
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params, requestOptions);
        logger.debug("Payment intent created: {}", paymentIntent.getId());
        return paymentIntent.getClientSecret();
    }

    /**
     * Create a PaymentIntent from BigDecimal amount
     *
     * @param amount        Amount in dollars (e.g., BigDecimal("10.00"))
     * @param currency      Currency code (e.g., "usd")
     * @param orderId       Order ID for metadata
     * @param customerEmail Customer email for metadata
     * @return PaymentIntent client secret
     * @throws StripeException if Stripe API call fails
     */
    public String createPaymentIntent(BigDecimal amount, String currency, Long orderId, String customerEmail)
            throws StripeException {
        // Convert BigDecimal to cents (multiply by 100)
        long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
        return createPaymentIntent(amountInCents, currency, orderId, customerEmail);
    }

    // Checkout Session flow removed in favor of Payment Intents + Payment Element

    /**
     * Retrieve a PaymentIntent by ID
     *
     * @param paymentIntentId Stripe PaymentIntent ID
     * @return PaymentIntent object
     * @throws StripeException if Stripe API call fails
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Confirm that a PaymentIntent was successfully processed
     *
     * @param paymentIntentId Stripe PaymentIntent ID
     * @return true if payment succeeded, false otherwise
     * @throws StripeException if Stripe API call fails
     */
    public boolean verifyPaymentSucceeded(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = retrievePaymentIntent(paymentIntentId);
        return "succeeded".equals(paymentIntent.getStatus());
    }
}
