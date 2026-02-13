package com.ecommerse.backend.controllers;

import com.ecommerse.backend.entities.Order;
import com.ecommerse.backend.entities.OrderItem;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.services.OrderService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for Stripe Checkout Session flow
 */
@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CheckoutController {

        private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);
        private final OrderService orderService;
        private final UserRepository userRepository;
        private final String frontendBaseUrl;
        private final String configuredStripeSecret;

        public CheckoutController(
                        OrderService orderService,
                        UserRepository userRepository,
                        @Value("${app.frontend-url:http://localhost:4200}") String frontendBaseUrl,
                        @Value("${stripe.secret.key:${STRIPE_SECRET_KEY:}}") String configuredStripeSecret) {
                this.orderService = orderService;
                this.userRepository = userRepository;
                this.frontendBaseUrl = normalizeFrontendUrl(frontendBaseUrl);
                this.configuredStripeSecret = configuredStripeSecret != null ? configuredStripeSecret.trim() : "";
        }

        private String normalizeFrontendUrl(String configuredUrl) {
                String value = (configuredUrl == null || configuredUrl.isBlank())
                                ? "http://localhost:4200"
                                : configuredUrl.trim();

                // Avoid trailing slash duplications when building URLs
                if (value.endsWith("/")) {
                        value = value.substring(0, value.length() - 1);
                }
                return value;
        }

        private String resolveStripeSecretKey() {
                if (configuredStripeSecret != null && !configuredStripeSecret.isBlank()) {
                        return configuredStripeSecret;
                }
                String envValue = System.getenv("STRIPE_SECRET_KEY");
                return envValue != null ? envValue.trim() : null;
        }

        /**
         * Request DTO for create-order endpoint
         */
        public record CreateOrderRequest(
                        String shippingAddress,
                        String shippingCity,
                        String shippingPostalCode,
                        String shippingCountry,
                        BigDecimal shippingAmount,
                        BigDecimal taxAmount) {
        }

        /**
         * Response DTO for create-order endpoint
         */
        public record CreateOrderResponse(
                        Long orderId,
                        String orderNumber,
                        Long totalCents,
                        String currency) {
        }

        /**
         * Request DTO for create-session endpoint
         */
        public record CreateSessionReq(String orderId) {
        }

        /**
         * Create an order from the authenticated user's cart (PENDING status).
         * This endpoint is called before creating the Stripe Checkout Session.
         * 
         * POST /api/checkout/create-order
         * Body: { shippingAddress, shippingCity, shippingPostalCode, shippingCountry,
         * shippingAmount, taxAmount }
         * Response: { orderId, orderNumber, totalCents, currency }
         */
        @PostMapping("/create-order")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ResponseEntity<?> createOrder(
                        @Valid @RequestBody CreateOrderRequest request,
                        Authentication authentication) {
                try {
                        logger.info("Creating order from cart for user: {}", authentication.getName());

                        // Get authenticated user
                        User user = userRepository.findByUsername(authentication.getName())
                                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                        // Create order from cart with PENDING status
                        var orderDTO = orderService.createOrderFromCart(
                                        user.getId(),
                                        request.shippingAddress(),
                                        request.shippingCity(),
                                        request.shippingPostalCode(),
                                        request.shippingCountry(),
                                        request.shippingAmount() != null ? request.shippingAmount() : BigDecimal.ZERO,
                                        request.taxAmount() != null ? request.taxAmount() : BigDecimal.ZERO);

                        // Calculate total in cents
                        BigDecimal totalAmount = orderDTO.getTotalAmount() != null ? orderDTO.getTotalAmount()
                                        : BigDecimal.ZERO;
                        long totalCents = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

                        CreateOrderResponse response = new CreateOrderResponse(
                                        orderDTO.getId(),
                                        orderDTO.getOrderNumber(),
                                        totalCents,
                                        "eur" // Default currency
                        );

                        logger.info("Order created successfully: {} (total: {} cents)", orderDTO.getOrderNumber(),
                                        totalCents);
                        return ResponseEntity.ok(response);

                } catch (IllegalArgumentException e) {
                        logger.error("Failed to create order: {}", e.getMessage());
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage()));
                } catch (Exception e) {
                        logger.error("Unexpected error creating order: {}", e.getMessage(), e);
                        return ResponseEntity.status(500).body(Map.of(
                                        "error", "Failed to create order: " + e.getMessage()));
                }
        }

        /**
         * Create a Stripe Checkout Session for an existing PENDING order.
         * 
         * POST /api/checkout/create-session
         * Body: { "orderId": "123" }
         * Response: { "id": "cs_test_..." }
         */
        @PostMapping("/create-session")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ResponseEntity<?> createSession(@RequestBody(required = false) CreateSessionReq req,
                        HttpServletRequest httpRequest) {
                try {
                        // Validate request
                        if (req == null || req.orderId() == null || req.orderId().isBlank()) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "orderId is required",
                                                "hint",
                                                "Send {\"orderId\":\"...\"} with Content-Type: application/json"));
                        }

                        // Load order
                        var orderOpt = orderService.findPending(req.orderId());
                        if (orderOpt.isEmpty()) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "Order not found or not PENDING",
                                                "orderId", req.orderId()));
                        }
                        var order = orderOpt.get();

                        // If order already has a checkout session, return it instead of creating a new
                        // one
                        // This prevents conflicts with old Stripe data and avoids duplicate sessions
                        if (order.getStripeCheckoutSessionId() != null
                                        && !order.getStripeCheckoutSessionId().isBlank()) {
                                logger.info("Order {} already has checkout session {}, returning existing session",
                                                req.orderId(), order.getStripeCheckoutSessionId());
                                return ResponseEntity.ok(Map.of("id", order.getStripeCheckoutSessionId()));
                        }

                        // Recalculate total from DB
                        long totalCents = orderService.recalculateTotalCents(req.orderId());
                        if (totalCents <= 0) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "Total must be > 0",
                                                "computedTotal", totalCents,
                                                "hint", "Check unit prices and quantities"));
                        }

                        // Load items from DB
                        var items = orderService.findItems(req.orderId());
                        if (items.isEmpty()) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "Order has no items"));
                        }

                        // Build line_items with inline price_data
                        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
                        for (OrderItem it : items) {
                                // Convert BigDecimal unit price to cents
                                long unitPriceCents = it.getUnitPrice()
                                                .multiply(java.math.BigDecimal.valueOf(100))
                                                .longValue();

                                lineItems.add(
                                                SessionCreateParams.LineItem.builder()
                                                                .setQuantity((long) it.getQuantity())
                                                                .setPriceData(
                                                                                SessionCreateParams.LineItem.PriceData
                                                                                                .builder()
                                                                                                .setCurrency(order
                                                                                                                .getCurrency()
                                                                                                                .toLowerCase())
                                                                                                .setUnitAmount(unitPriceCents)
                                                                                                .setProductData(
                                                                                                                SessionCreateParams.LineItem.PriceData.ProductData
                                                                                                                                .builder()
                                                                                                                                .setName(it.getProductName())
                                                                                                                                .putMetadata("sku",
                                                                                                                                                it.getProductSku())
                                                                                                                                .build())
                                                                                                .build())
                                                                .build());
                        }

                        // Resolve Stripe secret key from config or environment
                        String secretKey = resolveStripeSecretKey();
                        if (secretKey == null || secretKey.isBlank()) {
                                logger.error(
                                                "Stripe secret key is not configured via property 'stripe.secret.key' or STRIPE_SECRET_KEY environment variable");
                                return ResponseEntity.status(500).body(Map.of(
                                                "error", "Stripe secret key not configured",
                                                "hint", "Set STRIPE_SECRET_KEY env var or stripe.secret.key property"));
                        }
                        Stripe.apiKey = secretKey;

                        // Build success and cancel URLs (Next.js routes)
                        String successUrl = frontendBaseUrl + "/checkout/success?orderId=" + req.orderId()
                                        + "&session_id={CHECKOUT_SESSION_ID}";
                        String backendBaseUrl = ServletUriComponentsBuilder.fromRequestUri(httpRequest)
                                        .replacePath(null)
                                        .build()
                                        .toUriString();
                        String encodedOrderId = URLEncoder.encode(req.orderId(), StandardCharsets.UTF_8);
                        String cancelUrl = backendBaseUrl + "/api/checkout/cancel?orderId=" + encodedOrderId
                                        + "&session_id={CHECKOUT_SESSION_ID}";

                        // Create Stripe Checkout Session
                        var params = SessionCreateParams.builder()
                                        .setMode(SessionCreateParams.Mode.PAYMENT)
                                        .setSuccessUrl(successUrl)
                                        .setCancelUrl(cancelUrl)
                                        .addAllLineItem(lineItems)
                                        .setClientReferenceId(req.orderId())
                                        .putMetadata("orderId", req.orderId())
                                        .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                                                        .putMetadata("orderId", req.orderId())
                                                        .build())
                                        // Optional: enable automatic tax and shipping collection
                                        // .setAutomaticTax(SessionCreateParams.AutomaticTax.builder().setEnabled(true).build())
                                        // .setShippingAddressCollection(SessionCreateParams.ShippingAddressCollection.builder()
                                        // .addAllowedCountry(SessionCreateParams.ShippingAddressCollection.AllowedCountry.FI)
                                        // .build())
                                        .build();

                        // Use order number + timestamp for idempotency to avoid conflicts with old
                        // Stripe data
                        // This ensures uniqueness even if database was reset but Stripe has old records
                        String idempotencyKey = "co_" + req.orderId() + "_" + System.currentTimeMillis();

                        var session = com.stripe.model.checkout.Session.create(
                                        params,
                                        com.stripe.net.RequestOptions.builder()
                                                        .setIdempotencyKey(idempotencyKey)
                                                        .build());

                        // Persist session ID and update order status
                        orderService.attachCheckoutSession(req.orderId(), session.getId());

                        logger.info("Created Stripe Checkout Session {} for order {}", session.getId(), req.orderId());
                        return ResponseEntity.ok(Map.of(
                                        "id", session.getId(),
                                        "url", session.getUrl()));

                } catch (StripeException e) {
                        logger.error("Stripe API error: {}", e.getMessage(), e);
                        return ResponseEntity.status(502).body(Map.of(
                                        "error", "Stripe error",
                                        "code", e.getCode() != null ? e.getCode() : "unknown",
                                        "message", e.getMessage()));
                } catch (IllegalArgumentException e) {
                        logger.warn("Validation error: {}", e.getMessage());
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage()));
                } catch (Exception e) {
                        logger.error("Unexpected error creating checkout session: {}", e.getMessage(), e);
                        return ResponseEntity.status(500).body(Map.of(
                                        "error", "Internal error",
                                        "message",
                                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                }
        }

        @GetMapping("/cancel")
        public ResponseEntity<Void> cancelCheckoutFallback(
                        @RequestParam(value = "orderId", required = false) String orderId,
                        @RequestParam(value = "session_id", required = false) String sessionId) {
                if (orderId != null && !orderId.isBlank()) {
                        try {
                                orderService.markPaymentCanceled(orderId, null, sessionId,
                                                "checkout_canceled", "Checkout canceled by customer");
                        } catch (Exception ex) {
                                logger.warn("Cancel fallback could not transition order {}: {}", orderId,
                                                ex.getMessage());
                        }
                }

                String redirectUrl = frontendBaseUrl + "/cart?checkout=canceled";
                return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
        }
}
