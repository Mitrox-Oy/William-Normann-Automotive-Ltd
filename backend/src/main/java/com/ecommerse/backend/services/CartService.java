package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.CartDTO;
import com.ecommerse.backend.dto.CartItemDTO;
import com.ecommerse.backend.dto.CartValidationResult;
import com.ecommerse.backend.dto.CartAnalytics;
import com.ecommerse.backend.entities.Cart;
import com.ecommerse.backend.entities.CartItem;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.CartRepository;
import com.ecommerse.backend.repositories.CartItemRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Service class for managing shopping carts
 */
@Service
@Transactional
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${cart.expiration.hours:24}")
    private int cartExpirationHours;

    @Value("${cart.max.items:50}")
    private int maxCartItems;

    @Value("${cart.reservation.minutes:30}")
    private int cartReservationMinutes;

    private void reserveStock(Product product, int quantity) {
        if (quantity <= 0) {
            return;
        }
        product.reduceStock(quantity);
        productRepository.save(product);
    }

    private void releaseStock(Product product, int quantity) {
        if (quantity <= 0) {
            return;
        }
        product.increaseStock(quantity);
        productRepository.save(product);
    }

    private void refreshReservation(CartItem cartItem) {
        cartItem.refreshReservation(cartReservationMinutes);
    }

    private void touchCart(Cart cart) {
        if (cart == null) {
            return;
        }
        cart.setIsActive(true);
        cart.setExpiration(cartExpirationHours);
        cart.updateActivity();
    }

    private void refreshReservations(Cart cart) {
        removeExpiredItems(cart);
        cart.getItems().forEach(this::refreshReservation);
        cart.updateActivity();
    }

    private void removeExpiredItems(Cart cart) {
        if (cart == null || cart.getItems().isEmpty()) {
            return;
        }

        List<CartItem> expiredItems = cart.getItems().stream()
                .filter(CartItem::isReservationExpired)
                .collect(Collectors.toList());

        if (expiredItems.isEmpty()) {
            return;
        }

        for (CartItem expired : expiredItems) {
            try {
                logger.info("Releasing expired reservation for cartItem {} in cart {}", expired.getId(), cart.getId());
                removeCartItemInternal(expired, true);
            } catch (Exception ex) {
                logger.warn("Unable to remove expired reservation for cartItem {}: {}", expired.getId(),
                        ex.getMessage());
            }
        }
    }

    private void removeCartItemInternal(CartItem cartItem, boolean restock) {
        Cart cart = cartItem.getCart();
        Product product = cartItem.getProduct();

        if (restock && product != null) {
            releaseStock(product, cartItem.getQuantity());
        }

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        cart.updateTotalAmount();
        cart.setUpdatedDate(LocalDateTime.now());
        cartRepository.save(cart);
    }

    /**
     * Get or create cart for user
     */
    public CartDTO getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCartForUser(userId));
        refreshReservations(cart);
        touchCart(cart);
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    /**
     * Add item to cart
     */
    public CartItemDTO addItemToCart(Long userId, Long productId, Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Validate product exists and is available
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (!product.isAvailable()) {
            throw new IllegalArgumentException("Product is not available: " + product.getName());
        }

        // Prevent adding quote-only products to cart
        if (Boolean.TRUE.equals(product.getQuoteOnly())) {
            throw new IllegalArgumentException(
                    "This product is only available by request quote and cannot be added to cart");
        }

        // Get or create cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCartForUser(userId));

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        CartItem cartItem;
        if (existingItem.isPresent()) {
            // Update existing item
            cartItem = existingItem.get();
            int currentQuantity = cartItem.getQuantity();
            int newQuantity = currentQuantity + quantity;

            if (product.getStockQuantity() < quantity) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStockQuantity());
            }

            reserveStock(product, quantity);

            cartItem.setQuantity(newQuantity);
            cartItem.updateTotalPrice();
            cartItem.setUpdatedDate(LocalDateTime.now());
            refreshReservation(cartItem);
        } else {
            if (product.getStockQuantity() < quantity) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStockQuantity());
            }
            reserveStock(product, quantity);

            // Create new cart item
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setUnitPrice(product.getPrice());
            cartItem.updateTotalPrice();
            cartItem.setCreatedDate(LocalDateTime.now());
            cartItem.setUpdatedDate(LocalDateTime.now());
            refreshReservation(cartItem);

            cart.getItems().add(cartItem);
        }

        // Update cart total
        cart.updateTotalAmount();
        cart.setUpdatedDate(LocalDateTime.now());
        touchCart(cart);

        CartItem saved = cartItemRepository.save(cartItem);
        cartRepository.save(cart);

        return convertToCartItemDTO(saved);
    }

    /**
     * Update item quantity in cart
     */
    public CartItemDTO updateCartItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found with ID: " + cartItemId));

        // Verify cart belongs to user
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }

        Product product = cartItem.getProduct();
        if (!product.getActive() || !product.isAvailable()) {
            throw new IllegalArgumentException("Product " + product.getName() + " is no longer available");
        }

        int currentQuantity = cartItem.getQuantity();
        if (quantity > currentQuantity) {
            int additional = quantity - currentQuantity;
            if (product.getStockQuantity() < additional) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStockQuantity());
            }
            reserveStock(product, additional);
        } else if (quantity < currentQuantity) {
            int release = currentQuantity - quantity;
            releaseStock(product, release);
        }

        cartItem.setQuantity(quantity);
        cartItem.updateTotalPrice();
        cartItem.setUpdatedDate(LocalDateTime.now());
        refreshReservation(cartItem);

        // Update cart total
        Cart cart = cartItem.getCart();
        cart.updateTotalAmount();
        cart.setUpdatedDate(LocalDateTime.now());
        touchCart(cart);

        CartItem updated = cartItemRepository.save(cartItem);
        cartRepository.save(cart);

        return convertToCartItemDTO(updated);
    }

    /**
     * Remove item from cart
     */
    public void removeItemFromCart(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found with ID: " + cartItemId));

        // Verify cart belongs to user
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }

        removeCartItemInternal(cartItem, true);
    }

    /**
     * Clear all items from cart
     */
    public void clearCart(Long userId) {
        clearCart(userId, true);
    }

    public void clearCart(Long userId, boolean restock) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user ID: " + userId));

        List<CartItem> items = new ArrayList<>(cart.getItems());
        for (CartItem item : items) {
            removeCartItemInternal(item, restock);
        }
    }

    /**
     * Get cart item count for user
     */
    @Transactional(readOnly = true)
    public Integer getCartItemCount(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> cart.getItems().stream()
                        .mapToInt(CartItem::getQuantity)
                        .sum())
                .orElse(0);
    }

    /**
     * Get cart total amount for user
     */
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(Cart::getTotalAmount)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Validate cart items (check stock, availability, prices)
     */
    @Transactional(readOnly = true)
    public List<String> validateCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user ID: " + userId));

        List<String> errors = cart.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();
                    if (!product.getActive()) {
                        return "Product '" + product.getName() + "' is no longer available";
                    }
                    if (!product.isAvailable()) {
                        return "Product '" + product.getName() + "' is currently unavailable";
                    }
                    if (product.getStockQuantity() < item.getQuantity()) {
                        return "Insufficient stock for '" + product.getName() + "'. Available: "
                                + product.getStockQuantity();
                    }
                    if (!product.getPrice().equals(item.getUnitPrice())) {
                        return "Price changed for '" + product.getName() + "'. Current price: " + product.getPrice();
                    }
                    return null;
                })
                .filter(error -> error != null)
                .collect(Collectors.toList());

        return errors;
    }

    /**
     * Validate cart and return comprehensive validation results
     */
    public CartValidationResult validateCartComprehensive(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));

        removeExpiredItems(cart);

        CartValidationResult result = new CartValidationResult();
        result.setValid(true);
        List<String> errors = new ArrayList<>(validateCart(userId));
        if (cart.isExpired()) {
            errors.add("Cart has expired. Please refresh your cart.");
        }
        if (!Boolean.TRUE.equals(cart.getIsActive())) {
            errors.add("Cart is inactive. Please refresh your cart.");
        }
        if (cart.getItems().isEmpty()) {
            errors.add("Cart is empty.");
        }
        result.setErrors(errors);
        result.setCartExpired(cart.isExpired());
        result.setCartActive(cart.getIsActive());
        result.setTotalItems(cart.getTotalItems());
        result.setTotalAmount(cart.getTotalAmount());

        if (!result.getErrors().isEmpty() || result.isCartExpired() || !result.isCartActive()) {
            result.setValid(false);
        }

        return result;
    }

    /**
     * Update cart activity timestamp
     */
    public void updateCartActivity(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));

        refreshReservations(cart);
        touchCart(cart);
        cartRepository.save(cart);
    }

    /**
     * Refresh cart prices and availability
     */
    public CartDTO refreshCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));

        removeExpiredItems(cart);

        // Update each cart item
        for (CartItem item : cart.getItems()) {
            item.updateTotalPrice();
            refreshReservation(item);
        }

        // Update cart totals
        cart.updateTotalAmount();
        touchCart(cart);

        Cart updated = cartRepository.save(cart);
        return convertToDTO(updated);
    }

    /**
     * Check if cart has reached maximum item limit
     */
    public boolean isCartFull(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null)
            return false;

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return totalItems >= maxCartItems;
    }

    /**
     * Get cart analytics data
     */
    public CartAnalytics getCartAnalytics(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));

        CartAnalytics analytics = new CartAnalytics();
        analytics.setCartId(cart.getId());
        analytics.setCreatedDate(cart.getCreatedDate());
        analytics.setLastActivity(cart.getLastActivity());
        analytics.setTotalItems(cart.getTotalItems());
        analytics.setTotalAmount(cart.getTotalAmount());
        analytics.setUniqueProducts(cart.getItems().size());
        analytics.setIsExpired(cart.isExpired());
        analytics.setIsActive(cart.getIsActive());

        // Calculate average item price
        if (!cart.getItems().isEmpty()) {
            BigDecimal avgPrice = cart.getTotalAmount()
                    .divide(BigDecimal.valueOf(cart.getTotalItems()), 2, RoundingMode.HALF_UP);
            analytics.setAverageItemPrice(avgPrice);
        } else {
            analytics.setAverageItemPrice(BigDecimal.ZERO);
        }

        return analytics;
    }

    /**
     * Create new cart for user
     */
    private Cart createNewCartForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setTotalItems(0);
        cart.setCreatedDate(LocalDateTime.now());
        cart.setUpdatedDate(LocalDateTime.now());
        cart.setExpiration(cartExpirationHours);
        cart.setIsActive(true);
        cart.setSessionId(UUID.randomUUID().toString());
        cart.updateActivity();

        return cartRepository.save(cart);
    }

    /**
     * Convert Cart entity to DTO
     */
    private CartDTO convertToDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser().getId());
        dto.setTotalAmount(cart.getTotalAmount());
        dto.setItemCount(cart.getItems().size());
        dto.setTotalQuantity(cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum());
        dto.setTotalItems(cart.getTotalItemsCount());
        dto.setIsEmpty(cart.isEmpty());
        dto.setCreatedDate(cart.getCreatedDate());
        dto.setUpdatedDate(cart.getUpdatedDate());
        dto.setExpiresAt(cart.getExpiresAt());
        dto.setIsActive(cart.getIsActive());
        dto.setSessionId(cart.getSessionId());
        dto.setLastActivity(cart.getLastActivity());
        dto.setIsExpired(cart.isExpired());
        dto.setIsValid(cart.isValid());

        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(this::convertToCartItemDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    /**
     * Convert CartItem entity to DTO
     */
    private CartItemDTO convertToCartItemDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setCartId(cartItem.getCart().getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName());
        dto.setProductSku(cartItem.getProduct().getSku());
        dto.setQuantity(cartItem.getQuantity());
        dto.setUnitPrice(cartItem.getUnitPrice());
        dto.setTotalPrice(cartItem.getTotalPrice());
        dto.setAvailable(cartItem.getProduct().isAvailable());
        dto.setInStock(cartItem.getProduct().getStockQuantity() >= cartItem.getQuantity());
        dto.setCurrentStock(cartItem.getProduct().getStockQuantity());
        dto.setCreatedDate(cartItem.getCreatedDate());
        dto.setUpdatedDate(cartItem.getUpdatedDate());
        dto.setReservationExpiresAt(cartItem.getReservedUntil());

        return dto;
    }

    public int releaseExpiredReservations() {
        List<CartItem> expiredItems = cartItemRepository.findExpiredReservations(LocalDateTime.now());
        int releasedCount = 0;
        for (CartItem item : expiredItems) {
            try {
                removeCartItemInternal(item, true);
                releasedCount++;
            } catch (Exception ex) {
                logger.warn("Failed to release reservation for cartItem {}: {}", item.getId(), ex.getMessage());
            }
        }
        return releasedCount;
    }
}
