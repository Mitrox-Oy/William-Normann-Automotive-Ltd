package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.OrderDTO;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.entities.Order;
import com.ecommerse.backend.entities.OrderItem;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.CartRepository;
import com.ecommerse.backend.repositories.OrderRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.services.analytics.AlertService;
import com.ecommerse.backend.services.notifications.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLookupServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartService cartService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private AlertService alertService;

    @InjectMocks
    private OrderService orderService;

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private Order buildOrder(User user) {
        Order order = new Order();
        order.setId(77L);
        order.setUser(user);
        order.setStatus(OrderStatus.CHECKOUT_CREATED);
        order.setCreatedDate(LocalDateTime.now());
        order.setOrderDate(LocalDateTime.now());
        order.setShippingAddress("123 Main");
        order.setShippingCity("City");
        order.setShippingPostalCode("00000");
        order.setShippingCountry("Country");

        Category category = new Category();
        category.setName("Default");
        category.setSlug("default");

        Product product = new Product();
        product.setId(9L);
        product.setName("Headphones");
        product.setSku("HP-1");
        product.setPrice(new BigDecimal("99.99"));
        product.setCategory(category);

        OrderItem item = new OrderItem(order, product, 1);
        order.addOrderItem(item);
        order.calculateTotalAmount();
        return order;
    }

    @Test
    void getOrderByCheckoutSessionReturnsOrderForOwner() {
        User user = buildUser(1L, "alice@example.com");
        Order order = buildOrder(user);
        order.setStripeCheckoutSessionId("cs_test_123");

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(orderRepository.findByStripeCheckoutSessionId("cs_test_123")).thenReturn(Optional.of(order));

        OrderDTO dto = orderService.getOrderByCheckoutSession("cs_test_123", user.getUsername());

        assertEquals(order.getId(), dto.getId());
        assertEquals(order.getOrderNumber(), dto.getOrderNumber());
        assertEquals(order.getOrderItems().size(), dto.getOrderItems().size());
    }

    @Test
    void getOrderByCheckoutSessionRejectsMismatchedUser() {
        User owner = buildUser(1L, "alice@example.com");
        User other = buildUser(2L, "bob@example.com");
        Order order = buildOrder(other);
        order.setStripeCheckoutSessionId("cs_test_789");

        when(userRepository.findByUsername(owner.getUsername())).thenReturn(Optional.of(owner));
        when(orderRepository.findByStripeCheckoutSessionId("cs_test_789")).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderByCheckoutSession("cs_test_789", owner.getUsername()));
    }

    @Test
    void getLatestOrderForUserReturnsNewestOrder() {
        User user = buildUser(3L, "carol@example.com");
        Order order = buildOrder(user);

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(orderRepository.findTopByUserOrderByCreatedDateDesc(user)).thenReturn(Optional.of(order));

        OrderDTO dto = orderService.getLatestOrderForUser(user.getUsername());

        assertEquals(order.getId(), dto.getId());
    }

    @Test
    void getLatestOrderForUserThrowsWhenMissing() {
        User user = buildUser(4L, "dave@example.com");
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(orderRepository.findTopByUserOrderByCreatedDateDesc(any(User.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> orderService.getLatestOrderForUser(user.getUsername()));
    }
}
