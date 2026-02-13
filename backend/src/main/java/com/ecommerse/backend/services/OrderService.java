package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.CartValidationResult;
import com.ecommerse.backend.dto.OrderDTO;
import com.ecommerse.backend.dto.OrderItemDTO;
import com.ecommerse.backend.entities.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.ecommerse.backend.repositories.CartRepository;
import com.ecommerse.backend.repositories.OrderRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.services.notifications.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final EnumSet<OrderStatus> PROCESSED_STATUSES = EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING,
            OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    private static final EnumSet<OrderStatus> TERMINAL_CHECKOUT_STATUSES = EnumSet.of(OrderStatus.PAID,
            OrderStatus.FAILED, OrderStatus.CANCELLED, OrderStatus.EXPIRED, OrderStatus.REFUNDED);
    private static final EnumSet<OrderStatus> PAYMENT_ELIGIBLE_STATUSES = EnumSet.of(OrderStatus.PENDING,
            OrderStatus.CONFIRMED, OrderStatus.CHECKOUT_CREATED);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final com.ecommerse.backend.services.analytics.AlertService alertService;
    private final String stripeSecretKeyProperty;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
            CartRepository cartRepository, CartService cartService, ProductRepository productRepository,
            EmailService emailService,
            com.ecommerse.backend.services.analytics.AlertService alertService,
            @Value("${stripe.secret.key:}") String stripeSecretKeyProperty) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.emailService = emailService;
        this.alertService = alertService;
        this.stripeSecretKeyProperty = stripeSecretKeyProperty;
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersForUser(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return orderRepository.findByUserOrderByCreatedDateDesc(user, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return orderRepository.findByUserOrderByCreatedDateDesc(user, Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Verify order belongs to user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return convertToDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderByCheckoutSession(String sessionId, String username) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Checkout session identifier is required");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = orderRepository.findByStripeCheckoutSessionId(sessionId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Order not found for session"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return convertToDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDTO getLatestOrderForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = orderRepository.findTopByUserOrderByCreatedDateDesc(user)
                .orElseThrow(() -> new IllegalArgumentException("No orders found for user"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return convertToDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getRecentOrdersForOwner(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1), Sort.by(Sort.Direction.DESC, "createdDate"));
        return orderRepository.findAll(pageable)
                .getContent()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Create an order from the user's cart
     *
     * @param userId             User ID
     * @param shippingAddress    Shipping address line
     * @param shippingCity       Shipping city
     * @param shippingPostalCode Shipping postal code
     * @param shippingCountry    Shipping country
     * @param shippingAmount     Shipping cost
     * @param taxAmount          Tax amount
     * @return Created OrderDTO
     */
    @Transactional
    public OrderDTO createOrderFromCart(Long userId, String shippingAddress, String shippingCity,
            String shippingPostalCode, String shippingCountry,
            BigDecimal shippingAmount, BigDecimal taxAmount) {
        // Validate cart
        CartValidationResult validation = cartService.validateCartComprehensive(userId);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Cart validation failed: " + String.join(", ", validation.getErrors()));
        }

        // Get cart with items
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from empty cart");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create order
        Order order = new Order(user, shippingAddress, shippingCity, shippingPostalCode, shippingCountry);
        order.setShippingAmount(shippingAmount != null ? shippingAmount : BigDecimal.ZERO);
        order.setTaxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING);

        // Convert cart items to order items (stock already reserved while in cart)
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Final stock check
            if (!product.getActive() || !product.isAvailable()) {
                throw new IllegalArgumentException("Product '" + product.getName() + "' is no longer available");
            }

            OrderItem orderItem = new OrderItem(order, product, cartItem.getQuantity());
            // Use cart item's unit price (snapshot at time of adding to cart)
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            order.addOrderItem(orderItem);
        }

        // Calculate totals
        order.calculateTotalAmount();
        order.setInventoryLocked(true);

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful order creation
        cartService.clearCart(userId, false);

        return convertToDto(savedOrder);
    }

    /**
     * Mark an order as paid after successful payment
     *
     * @param orderId         Order ID
     * @param paymentIntentId Stripe PaymentIntent ID
     * @param username        Username for verification
     * @return Updated OrderDTO
     */
    @Transactional
    public OrderDTO markOrderAsPaid(Long orderId, String paymentIntentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Verify order belongs to user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        Order processed = finalizeOrderPayment(order, paymentIntentId);
        return convertToDto(processed);
    }

    /**
     * Finalize a checkout session by verifying payment status and transitioning the
     * order when appropriate.
     */
    @Transactional
    public OrderDTO finalizeCheckout(Long orderId, String checkoutSessionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        if (!PAYMENT_ELIGIBLE_STATUSES.contains(order.getStatus())) {
            return convertToDto(order);
        }

        String effectiveSessionId = (checkoutSessionId != null && !checkoutSessionId.isBlank())
                ? checkoutSessionId.trim()
                : order.getStripeCheckoutSessionId();

        String secretKey = resolveStripeSecretKey();

        if (secretKey == null || secretKey.isBlank()) {
            logger.warn("Stripe secret key not configured; skipping finalize verification for order {}",
                    order.getOrderNumber());
            return convertToDto(order);
        }

        if (effectiveSessionId == null || effectiveSessionId.isBlank()) {
            logger.warn("Order {} does not have a checkout session id; skipping finalize verification",
                    order.getOrderNumber());
            return convertToDto(order);
        }

        try {
            Stripe.apiKey = secretKey;
            Session session = Session.retrieve(effectiveSessionId);

            if (session != null) {
                String paymentStatus = session.getPaymentStatus();
                String status = session.getStatus();
                if ((paymentStatus != null && paymentStatus.equalsIgnoreCase("paid"))
                        || (status != null && status.equalsIgnoreCase("complete"))) {
                    Order processed = transitionCheckoutStatus(order, OrderStatus.PAID, session.getPaymentIntent(),
                            effectiveSessionId, null, null);
                    return convertToDto(processed);
                }
                logger.info("Checkout session {} not yet paid (status={}, paymentStatus={}) for order {}",
                        effectiveSessionId,
                        status,
                        paymentStatus,
                        order.getOrderNumber());
            }
        } catch (StripeException e) {
            logger.error("Unable to verify checkout session {} for order {}: {}", effectiveSessionId,
                    order.getOrderNumber(), e.getMessage(), e);
            return convertToDto(order);
        }

        return convertToDto(order);
    }

    /**
     * Mark an order as paid from Stripe webhook (no user context).
     * Idempotent: if already PAID or beyond, returns current state.
     */
    @Transactional
    public OrderDTO markOrderAsPaidFromWebhook(Long orderId, String paymentIntentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Order processed = transitionCheckoutStatus(order, OrderStatus.PAID, paymentIntentId,
                order.getStripeCheckoutSessionId(), null, null);
        return convertToDto(processed);
    }

    @Transactional
    public OrderDTO markPaymentFailed(String orderIdOrNumber, String paymentIntentId) {
        return markCheckoutTerminal(orderIdOrNumber, paymentIntentId, null, OrderStatus.FAILED,
                "payment_failed", "Payment failed");
    }

    @Transactional
    public OrderDTO markPaymentFailed(String orderIdOrNumber, String paymentIntentId, String checkoutSessionId,
            String failureCode, String failureMessage) {
        return markCheckoutTerminal(orderIdOrNumber, paymentIntentId, checkoutSessionId, OrderStatus.FAILED,
                failureCode, failureMessage);
    }

    @Transactional
    public OrderDTO markPaymentCanceled(String orderIdOrNumber, String paymentIntentId, String checkoutSessionId,
            String failureCode, String failureMessage) {
        return markCheckoutTerminal(orderIdOrNumber, paymentIntentId, checkoutSessionId, OrderStatus.CANCELLED,
                failureCode, failureMessage);
    }

    @Transactional
    public OrderDTO markCheckoutExpired(String orderIdOrNumber, String checkoutSessionId, String failureCode,
            String failureMessage) {
        return markCheckoutTerminal(orderIdOrNumber, null, checkoutSessionId, OrderStatus.EXPIRED,
                failureCode, failureMessage);
    }

    @Transactional
    public int expireStaleCheckouts(int expiryMinutes) {
        int ttlMinutes = expiryMinutes > 0 ? expiryMinutes : 45;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(ttlMinutes);
        List<Order> staleOrders = orderRepository.findByStatusInAndCreatedDateBeforeOrderByCreatedDateAsc(
                PAYMENT_ELIGIBLE_STATUSES,
                cutoff);

        int expiredCount = 0;
        for (Order order : staleOrders) {
            OrderStatus previousStatus = order.getStatus();
            Order transitioned = transitionCheckoutStatus(order, OrderStatus.EXPIRED, order.getPaymentIntentId(),
                    order.getStripeCheckoutSessionId(), "expired", "Checkout expired due to inactivity");
            if (previousStatus != OrderStatus.EXPIRED && transitioned.getStatus() == OrderStatus.EXPIRED) {
                expiredCount++;
            }
        }
        return expiredCount;
    }

    private Order finalizeOrderPayment(Order order, String paymentIntentId) {
        return transitionCheckoutStatus(order, OrderStatus.PAID, paymentIntentId,
                order != null ? order.getStripeCheckoutSessionId() : null,
                null, null);
    }

    private OrderDTO markCheckoutTerminal(String orderIdOrNumber, String paymentIntentId, String checkoutSessionId,
            OrderStatus terminalStatus, String failureCode, String failureMessage) {
        Order order = resolveOrderForProvider(orderIdOrNumber, paymentIntentId, checkoutSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for checkout transition"));
        Order transitioned = transitionCheckoutStatus(order, terminalStatus, paymentIntentId, checkoutSessionId,
                failureCode, failureMessage);
        return convertToDto(transitioned);
    }

    private Optional<Order> resolveOrderForProvider(String orderIdOrNumber, String paymentIntentId,
            String checkoutSessionId) {
        if (orderIdOrNumber != null && !orderIdOrNumber.isBlank()) {
            return Optional.of(findOrderByIdOrNumber(orderIdOrNumber));
        }
        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            Optional<Order> byPaymentIntent = orderRepository.findByPaymentIntentId(paymentIntentId);
            if (byPaymentIntent.isPresent()) {
                return byPaymentIntent;
            }
        }
        if (checkoutSessionId != null && !checkoutSessionId.isBlank()) {
            Optional<Order> bySession = orderRepository.findByStripeCheckoutSessionId(checkoutSessionId);
            if (bySession.isPresent()) {
                return bySession;
            }
        }
        return Optional.empty();
    }

    private Order transitionCheckoutStatus(Order order, OrderStatus targetStatus, String paymentIntentId,
            String checkoutSessionId, String failureCode, String failureMessage) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            order.setPaymentIntentId(paymentIntentId);
            order.setPaymentProvider("stripe");
        }
        if (checkoutSessionId != null && !checkoutSessionId.isBlank()) {
            order.setStripeCheckoutSessionId(checkoutSessionId);
            order.setPaymentProvider("stripe");
        }

        LocalDateTime now = LocalDateTime.now();
        OrderStatus currentStatus = order.getStatus();

        if (TERMINAL_CHECKOUT_STATUSES.contains(currentStatus)) {
            if (currentStatus == targetStatus && (failureCode != null || failureMessage != null)) {
                if (failureCode != null && !failureCode.isBlank()) {
                    order.setFailureCode(failureCode);
                }
                if (failureMessage != null && !failureMessage.isBlank()) {
                    order.setFailureMessage(trimFailureMessage(failureMessage));
                }
                order.setUpdatedDate(now);
                return orderRepository.save(order);
            }
            return order;
        }

        switch (targetStatus) {
            case PAID -> {
                if (!PAYMENT_ELIGIBLE_STATUSES.contains(currentStatus)) {
                    return order;
                }
                boolean inventoryWasLocked = order.isInventoryLocked();
                if (!inventoryWasLocked) {
                    applyInventoryAdjustments(order);
                }
                order.setStatus(OrderStatus.PAID);
                order.setPaidAt(now);
                order.setInventoryLocked(false);
                order.setFailureCode(null);
                order.setFailureMessage(null);
                order.setUpdatedDate(now);
                Order saved = orderRepository.save(order);
                dispatchPostPaymentNotifications(saved);
                logger.info("Order {} transitioned to PAID (paymentIntent={}, reservedInventory={})",
                        saved.getOrderNumber(), paymentIntentId, inventoryWasLocked);
                return saved;
            }
            case FAILED, CANCELLED, EXPIRED -> {
                if (order.isInventoryLocked()) {
                    restoreInventory(order);
                    order.setInventoryLocked(false);
                    order.setInventoryReleasedAt(now);
                }
                order.setStatus(targetStatus);
                if (targetStatus == OrderStatus.FAILED) {
                    order.setFailedAt(now);
                } else if (targetStatus == OrderStatus.CANCELLED) {
                    order.setCanceledAt(now);
                } else if (targetStatus == OrderStatus.EXPIRED) {
                    order.setExpiredAt(now);
                }
                if (failureCode != null && !failureCode.isBlank()) {
                    order.setFailureCode(failureCode);
                }
                if (failureMessage != null && !failureMessage.isBlank()) {
                    order.setFailureMessage(trimFailureMessage(failureMessage));
                }
                order.setUpdatedDate(now);
                Order saved = orderRepository.save(order);
                logger.info("Order {} transitioned to {}", saved.getOrderNumber(), targetStatus);
                return saved;
            }
            default -> throw new IllegalArgumentException("Unsupported checkout transition status: " + targetStatus);
        }
    }

    private String trimFailureMessage(String failureMessage) {
        if (failureMessage == null) {
            return null;
        }
        return failureMessage.length() > 1000 ? failureMessage.substring(0, 1000) : failureMessage;
    }

    private String resolveStripeSecretKey() {
        if (stripeSecretKeyProperty != null && !stripeSecretKeyProperty.isBlank()) {
            return stripeSecretKeyProperty;
        }
        return System.getenv("STRIPE_SECRET_KEY");
    }

    private void applyInventoryAdjustments(Order order) {
        Map<Long, Integer> quantityByProduct = new HashMap<>();
        Map<Long, Product> productById = new HashMap<>();

        for (OrderItem item : order.getOrderItems()) {
            if (item == null || item.getQuantity() <= 0) {
                continue;
            }
            Product product = item.getProduct();
            if (product == null || product.getId() == null) {
                logger.warn("Order {} item {} has no product reference; skipping stock adjustment",
                        order.getOrderNumber(), item.getId());
                continue;
            }
            quantityByProduct.merge(product.getId(), item.getQuantity(), Integer::sum);
            productById.putIfAbsent(product.getId(), product);
        }

        for (Map.Entry<Long, Integer> entry : quantityByProduct.entrySet()) {
            Long productId = entry.getKey();
            int decrementBy = entry.getValue();
            if (decrementBy <= 0) {
                continue;
            }

            Product product = productById.get(productId);
            if (product == null) {
                product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalStateException("Product not found for ID: " + productId));
            }

            int currentStock = Optional.ofNullable(product.getStockQuantity()).orElse(0);
            int updatedStock = currentStock - decrementBy;
            if (updatedStock < 0) {
                logger.error("Order {} cannot deduct {} units from product {} (available {}).",
                        order.getOrderNumber(), decrementBy, productId, currentStock);
                throw new IllegalStateException("Insufficient stock to fulfill order " + order.getOrderNumber());
            }

            product.setStockQuantity(updatedStock);
            productRepository.save(product);
            logger.debug("Order {} decremented product {} stock to {}", order.getOrderNumber(), productId,
                    updatedStock);
        }
    }

    private void restoreInventory(Order order) {
        Map<Long, Integer> quantityByProduct = new HashMap<>();
        Map<Long, Product> productById = new HashMap<>();

        for (OrderItem item : order.getOrderItems()) {
            if (item == null || item.getQuantity() <= 0) {
                continue;
            }
            Product product = item.getProduct();
            if (product == null || product.getId() == null) {
                logger.warn("Order {} item {} missing product reference during inventory restore",
                        order.getOrderNumber(), item.getId());
                continue;
            }
            quantityByProduct.merge(product.getId(), item.getQuantity(), Integer::sum);
            productById.putIfAbsent(product.getId(), product);
        }

        for (Map.Entry<Long, Integer> entry : quantityByProduct.entrySet()) {
            Long productId = entry.getKey();
            int increaseBy = entry.getValue();
            if (increaseBy <= 0) {
                continue;
            }

            Product product = productById.get(productId);
            if (product == null) {
                product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalStateException("Product not found for ID: " + productId));
            }

            product.increaseStock(increaseBy);
            productRepository.save(product);
            logger.debug("Order {} restored {} units back to product {} (stock now {})",
                    order.getOrderNumber(), increaseBy, productId, product.getStockQuantity());
        }
    }

    private void dispatchPostPaymentNotifications(Order order) {
        try {
            emailService.sendOrderConfirmation(order.getId());
        } catch (Exception ex) {
            logger.warn("Failed to send order confirmation for order {}: {}", order.getId(), ex.getMessage());
        }

        try {
            emailService.sendOwnerNotification(order.getId());
        } catch (Exception ex) {
            logger.warn("Failed to send owner notification for order {}: {}", order.getId(), ex.getMessage());
        }

        try {
            String message = "Order paid: " + order.getOrderNumber();
            alertService.recordSystemAlert(message,
                    order.getTotalAmount() != null ? order.getTotalAmount().toString() : null);
        } catch (Exception ex) {
            logger.warn("Failed to record alert for order {}: {}", order.getId(), ex.getMessage());
        }
    }

    /**
     * Persist Stripe PaymentIntent mapping for an order when intent is created.
     * Sets status to CONFIRMED to indicate PI created and awaiting confirmation.
     */
    @Transactional
    public OrderDTO setPaymentIntentForOrder(Long orderId, String paymentIntentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Only allow setting when order is new/pending
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            return convertToDto(order);
        }

        // Idempotent: if already set, return
        if (paymentIntentId != null && paymentIntentId.equals(order.getPaymentIntentId())) {
            return convertToDto(order);
        }

        order.setPaymentIntentId(paymentIntentId);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        order.setPaymentProvider("stripe");
        Order saved = orderRepository.save(order);
        return convertToDto(saved);
    }

    /**
     * Get all orders for owner (no user filter)
     *
     * @param pageable Pagination parameters
     * @return Page of OrderDTOs
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::convertToDto);
    }

    /**
     * Get orders filtered by status
     *
     * @param status   Order status
     * @param pageable Pagination parameters
     * @return Page of OrderDTOs
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusOrderByCreatedDateDesc(status, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get orders filtered by date range
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @param pageable  Pagination parameters
     * @return Page of OrderDTOs
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return orderRepository.findByCreatedDateBetweenOrderByCreatedDateDesc(startDate, endDate, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get orders filtered by status and date range
     *
     * @param status     Order status (optional, null for all statuses)
     * @param startDate  Start date (optional)
     * @param endDate    End date (optional)
     * @param searchTerm Order number search term (optional)
     * @param pageable   Pagination parameters
     * @return Page of OrderDTOs
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersForOwner(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate,
            String searchTerm, Pageable pageable) {
        Page<Order> orders;

        // Search by order number
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            orders = orderRepository.findByOrderNumberContainingIgnoreCase(searchTerm.trim(), pageable);
            // Filter by status if provided - manually paginate filtered list
            if (status != null) {
                List<Order> filtered = orders.getContent().stream()
                        .filter(order -> order.getStatus() == status)
                        .collect(Collectors.toList());

                // Manually paginate the filtered list
                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), filtered.size());
                List<Order> pagedList = start < filtered.size()
                        ? filtered.subList(start, end)
                        : Collections.emptyList();

                // Convert to DTOs and create Page
                List<OrderDTO> dtos = pagedList.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList());

                return new org.springframework.data.domain.PageImpl<>(dtos, pageable, filtered.size());
            }
            return orders.map(this::convertToDto);
        }

        // Filter by status only
        if (status != null && startDate == null && endDate == null) {
            orders = orderRepository.findByStatusOrderByCreatedDateDesc(status, pageable);
        }
        // Filter by date range only
        else if (status == null && startDate != null && endDate != null) {
            orders = orderRepository.findByCreatedDateBetweenOrderByCreatedDateDesc(startDate, endDate, pageable);
        }
        // Filter by status and date range - need to filter manually
        else if (status != null && startDate != null && endDate != null) {
            // Get all orders in date range, then manually filter by status
            List<Order> allInRange = orderRepository.findByCreatedDateBetween(startDate, endDate);
            List<Order> filtered = allInRange.stream()
                    .filter(order -> order.getStatus() == status)
                    .collect(Collectors.toList());
            // For pagination, we need to paginate the filtered list manually
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            List<Order> pagedList = filtered.subList(Math.min(start, filtered.size()), end);
            // Create a simple page implementation
            return new org.springframework.data.domain.PageImpl<>(pagedList, pageable, filtered.size())
                    .map(this::convertToDto);
        }
        // No filters - get all orders
        else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(this::convertToDto);
    }

    /**
     * Get order by ID for owner (no user verification)
     *
     * @param orderId Order ID
     * @return OrderDTO
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderByIdForOwner(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return convertToDto(order);
    }

    /**
     * Update order status with validation
     *
     * @param orderId   Order ID
     * @param newStatus New order status
     * @param username  Username performing the update (for authorization)
     * @return Updated OrderDTO
     */
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        OrderStatus currentStatus = order.getStatus();

        // Validate status transition
        if (currentStatus == newStatus) {
            return convertToDto(order); // No change needed
        }

        Order savedOrder;

        switch (newStatus) {
            case PAID: {
                savedOrder = transitionCheckoutStatus(order, OrderStatus.PAID, order.getPaymentIntentId(),
                        order.getStripeCheckoutSessionId(), null, null);
                break;
            }
            case CANCELLED: {
                savedOrder = transitionCheckoutStatus(order, OrderStatus.CANCELLED, order.getPaymentIntentId(),
                        order.getStripeCheckoutSessionId(), "owner_cancelled", "Order canceled by owner/admin");
                savedOrder.setShippedDate(null);
                savedOrder.setDeliveredDate(null);
                savedOrder = orderRepository.save(savedOrder);
                break;
            }
            case REFUNDED: {
                boolean needsRestock = !order.isInventoryLocked() || PROCESSED_STATUSES.contains(currentStatus);
                if (needsRestock) {
                    restoreInventory(order);
                }
                order.setInventoryLocked(false);
                order.setStatus(OrderStatus.REFUNDED);
                order.setUpdatedDate(LocalDateTime.now());
                savedOrder = orderRepository.save(order);
                break;
            }
            case SHIPPED: {
                order.setStatus(OrderStatus.SHIPPED);
                order.setShippedDate(LocalDateTime.now());
                order.setUpdatedDate(LocalDateTime.now());
                savedOrder = orderRepository.save(order);
                break;
            }
            case DELIVERED: {
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveredDate(LocalDateTime.now());
                order.setUpdatedDate(LocalDateTime.now());
                savedOrder = orderRepository.save(order);
                break;
            }
            case PROCESSING: {
                order.setStatus(OrderStatus.PROCESSING);
                order.setUpdatedDate(LocalDateTime.now());
                savedOrder = orderRepository.save(order);
                break;
            }
            case CONFIRMED: {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedDate(LocalDateTime.now());
                savedOrder = orderRepository.save(order);
                break;
            }
            case CHECKOUT_CREATED: {
                order.setStatus(OrderStatus.CHECKOUT_CREATED);
                order.setUpdatedDate(LocalDateTime.now());
                savedOrder = orderRepository.save(order);
                break;
            }
            case PENDING: {
                order.setStatus(OrderStatus.PENDING);
                order.setUpdatedDate(LocalDateTime.now());
                savedOrder = orderRepository.save(order);
                break;
            }
            case FAILED: {
                savedOrder = transitionCheckoutStatus(order, OrderStatus.FAILED, order.getPaymentIntentId(),
                        order.getStripeCheckoutSessionId(), "owner_failed", "Order marked failed by owner/admin");
                break;
            }
            case EXPIRED: {
                savedOrder = transitionCheckoutStatus(order, OrderStatus.EXPIRED, order.getPaymentIntentId(),
                        order.getStripeCheckoutSessionId(), "owner_expired", "Order marked expired by owner/admin");
                break;
            }
            default: {
                order.setStatus(newStatus);
                order.setUpdatedDate(LocalDateTime.now());
                savedOrder = orderRepository.save(order);
                break;
            }
        }

        // Trigger email notification for status changes that customers care about
        if (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.DELIVERED) {
            try {
                emailService.sendOrderStatusUpdate(orderId, newStatus.name());
            } catch (Exception e) {
                // Log but don't fail the transaction
                // Email failures are logged in EmailService
            }
        }

        return convertToDto(savedOrder);
    }

    /**
     * Export orders to CSV format
     *
     * @param status    Order status filter (optional)
     * @param startDate Start date filter (optional)
     * @param endDate   End date filter (optional)
     * @return CSV content as string
     */
    @Transactional(readOnly = true)
    public String exportOrdersToCsv(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders;

        // Build query based on filters
        if (status != null && startDate != null && endDate != null) {
            // Status and date range
            orders = orderRepository.findByCreatedDateBetween(startDate, endDate)
                    .stream()
                    .filter(order -> order.getStatus() == status)
                    .collect(Collectors.toList());
        } else if (status != null) {
            // Status only
            orders = orderRepository.findByStatusOrderByCreatedDateAsc(status);
        } else if (startDate != null && endDate != null) {
            // Date range only
            orders = orderRepository.findByCreatedDateBetween(startDate, endDate);
        } else {
            // No filters - get all orders
            orders = orderRepository.findAll();
        }

        // Build CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Order Number,Date,Customer,Status,Total Amount,Items Count,Shipping Address\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Order order : orders) {
            csv.append(escapeCsv(order.getOrderNumber())).append(",");
            csv.append(escapeCsv(order.getCreatedDate() != null ? order.getCreatedDate().format(dateFormatter) : "N/A"))
                    .append(",");
            csv.append(escapeCsv(order.getUser() != null ? order.getUser().getUsername() : "N/A")).append(",");
            csv.append(escapeCsv(order.getStatus().name())).append(",");
            csv.append(order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0.00").append(",");
            csv.append(order.getOrderItems() != null ? order.getOrderItems().size() : 0).append(",");
            String shippingAddress = order.getShippingAddress() + ", " + order.getShippingCity() +
                    ", " + order.getShippingPostalCode() + ", " + order.getShippingCountry();
            csv.append(escapeCsv(shippingAddress)).append("\n");
        }

        return csv.toString();
    }

    /**
     * Find a pending order by string order identifier which can be either numeric
     * ID or exact orderNumber.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Order> findPending(String orderIdOrNumber) {
        if (orderIdOrNumber == null || orderIdOrNumber.isBlank()) {
            return java.util.Optional.empty();
        }
        // Try numeric ID first
        try {
            Long id = Long.parseLong(orderIdOrNumber);
            java.util.Optional<Order> byId = orderRepository.findByIdWithItems(id);
            if (byId.isPresent() && isAwaitingPayment(byId.get())) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
        }
        // Fallback to order number
        java.util.Optional<Order> byNumber = orderRepository.findByOrderNumber(orderIdOrNumber);
        if (byNumber.isPresent() && isAwaitingPayment(byNumber.get())) {
            return byNumber;
        }
        return java.util.Optional.empty();
    }

    /**
     * Recalculate total in cents from DB for a given order (by id or orderNumber),
     * updating the order total.
     */
    @Transactional
    public long recalculateTotalCents(String orderIdOrNumber) {
        // Parse order ID and load with items
        final Long orderId = parseOrderId(orderIdOrNumber);

        // Load order with items using JOIN FETCH
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // Verify order is in correct state
        if (!isAwaitingPayment(order)) {
            throw new IllegalArgumentException("Order is not in PENDING/CHECKOUT_CREATED state: " + order.getStatus());
        }

        // Force initialization of items collection and recalculate
        int itemCount = order.getOrderItems().size();
        if (itemCount == 0) {
            throw new IllegalArgumentException("Order has no items");
        }

        order.calculateTotalAmount();
        Order saved = orderRepository.save(order);

        java.math.BigDecimal total = saved.getTotalAmount() != null ? saved.getTotalAmount()
                : java.math.BigDecimal.ZERO;
        long totalCents = total.multiply(java.math.BigDecimal.valueOf(100)).longValue();

        if (totalCents <= 0) {
            throw new IllegalArgumentException("Calculated total is 0. Items: " + itemCount + ", Total: " + total);
        }

        return totalCents;
    }

    /**
     * Helper method to parse order ID from string (supports both numeric ID and
     * order number)
     */
    private Long parseOrderId(String orderIdOrNumber) {
        try {
            return Long.parseLong(orderIdOrNumber);
        } catch (NumberFormatException e) {
            Order orderByNumber = orderRepository.findByOrderNumber(orderIdOrNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderIdOrNumber));
            return orderByNumber.getId();
        }
    }

    private Order findOrderByIdOrNumber(String orderIdOrNumber) {
        Long orderId = parseOrderId(orderIdOrNumber);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderIdOrNumber));
    }

    /**
     * Attach a Stripe PaymentIntent to the order and set status to CONFIRMED (PI
     * created).
     */
    @Transactional
    public void attachPaymentIntent(String orderIdOrNumber, String paymentIntentId) {
        Order order = findPending(orderIdOrNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found or not PENDING"));
        if (paymentIntentId != null && paymentIntentId.equals(order.getPaymentIntentId())) {
            return;
        }
        order.setPaymentIntentId(paymentIntentId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    /**
     * Attach Stripe Checkout Session ID to the order and set status to
     * CHECKOUT_CREATED.
     */
    @Transactional
    public void attachCheckoutSession(String orderIdOrNumber, String checkoutSessionId) {
        Order order = findPending(orderIdOrNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found or not PENDING"));
        if (checkoutSessionId != null && checkoutSessionId.equals(order.getStripeCheckoutSessionId())) {
            return; // Idempotent
        }
        order.setStripeCheckoutSessionId(checkoutSessionId);
        order.setPaymentProvider("stripe");
        order.setStatus(OrderStatus.CHECKOUT_CREATED);
        order.setUpdatedDate(LocalDateTime.now());
        orderRepository.save(order);
    }

    /**
     * Find order items for a given order ID or order number.
     */
    @Transactional(readOnly = true)
    public List<OrderItem> findItems(String orderIdOrNumber) {
        // Parse order ID
        final Long orderId = parseOrderId(orderIdOrNumber);

        // Load order with items using JOIN FETCH
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // Force initialization and return
        List<OrderItem> items = order.getOrderItems();
        items.size(); // Force initialization
        return items;
    }

    /**
     * Check if an order has already been processed (PAID or beyond).
     */
    @Transactional(readOnly = true)
    public boolean isProcessed(String orderIdOrNumber) {
        try {
            Long id = Long.parseLong(orderIdOrNumber);
            Optional<Order> orderOpt = orderRepository.findById(id);
            if (orderOpt.isEmpty()) {
                return false;
            }
            Order order = orderOpt.get();
            return PROCESSED_STATUSES.contains(order.getStatus());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Mark order as PAID from webhook with paymentIntentId. Idempotent.
     */
    @Transactional
    public void markPaid(String orderIdOrNumber, String paymentIntentId) {
        Order order = findOrderByIdOrNumber(orderIdOrNumber);
        transitionCheckoutStatus(order, OrderStatus.PAID, paymentIntentId, order.getStripeCheckoutSessionId(), null,
                null);
    }

    @Transactional
    public void markPaid(String orderIdOrNumber, String paymentIntentId, String checkoutSessionId) {
        Order order = resolveOrderForProvider(orderIdOrNumber, paymentIntentId, checkoutSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for paid transition"));
        transitionCheckoutStatus(order, OrderStatus.PAID, paymentIntentId, checkoutSessionId, null, null);
    }

    private boolean isAwaitingPayment(Order order) {
        return PAYMENT_ELIGIBLE_STATUSES.contains(order.getStatus());
    }

    /**
     * Escape CSV field (handle commas and quotes)
     */
    private String escapeCsv(String field) {
        if (field == null) {
            return "";
        }
        // If field contains comma, quote, or newline, wrap in quotes and escape quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private OrderDTO convertToDto(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingAmount(order.getShippingAmount());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setUserId(order.getUser() != null ? order.getUser().getId() : null);
        dto.setUsername(order.getUser() != null ? order.getUser().getUsername() : null);
        dto.setShippingAddress(order.getShippingAddress());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingPostalCode(order.getShippingPostalCode());
        dto.setShippingCountry(order.getShippingCountry());
        dto.setNotes(order.getNotes());
        dto.setOrderDate(order.getOrderDate());
        dto.setShippedDate(order.getShippedDate());
        dto.setDeliveredDate(order.getDeliveredDate());
        dto.setCreatedDate(order.getCreatedDate());
        dto.setUpdatedDate(order.getUpdatedDate());
        dto.setCanBeCancelled(order.canBeCancelled());
        dto.setCanBeShipped(order.canBeShipped());
        dto.setCanBeDelivered(order.canBeDelivered());
        dto.setStripeCheckoutSessionId(order.getStripeCheckoutSessionId());
        dto.setPaymentIntentId(order.getPaymentIntentId());
        dto.setPaymentProvider(order.getPaymentProvider());
        dto.setInventoryLocked(order.isInventoryLocked());
        dto.setInventoryReleasedAt(order.getInventoryReleasedAt());
        dto.setFailureCode(order.getFailureCode());
        dto.setFailureMessage(order.getFailureMessage());
        dto.setPaidAt(order.getPaidAt());
        dto.setFailedAt(order.getFailedAt());
        dto.setCanceledAt(order.getCanceledAt());
        dto.setExpiredAt(order.getExpiredAt());

        List<OrderItemDTO> items = order.getOrderItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());
        dto.setOrderItems(items);

        return dto;
    }

    private OrderItemDTO convertItemToDto(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setOrderId(item.getOrder() != null ? item.getOrder().getId() : null);
        dto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        dto.setProductName(item.getProductName());
        dto.setProductSku(item.getProductSku());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }
}
